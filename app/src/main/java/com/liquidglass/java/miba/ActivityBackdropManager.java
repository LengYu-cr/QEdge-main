package com.liquidglass.java.miba;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RecordingCanvas;
import android.graphics.RenderNode;
import android.view.Choreographer;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.WeakHashMap;

/**
 * Automatic backdrop host for ordinary Android View trees.
 *
 * <p>Fix29 = the proven Fix23 portal-separated capture architecture plus a
 * Choreographer frame pump.  It deliberately does NOT use Fix28 CaptureSlot,
 * because keeping live glass RenderNodes inside the captured DecorView can form
 * a RenderNode reference cycle on HWUI.</p>
 *
 * <p>The important invariant of this implementation is that the normal scene
 * hierarchy and the LiquidGlass hierarchy are physically separated. Top-level
 * LiquidGlassOverlay views are automatically moved to a transparent portal host
 * attached to the window DecorView above the normal content. A zero-drawing
 * placeholder remains at the original layout position, so LinearLayout/FrameLayout/etc
 * keep measuring and positioning exactly as before.</p>
 *
 * <p>The normal scene therefore contains no live glass RenderNodes. The automatic
 * backdrop records the DecorView (with the portal hidden) once per pre-draw into one
 * shared hardware RenderNode. Recording the DecorView instead of only
 * android.R.id.content is intentional: translucent cards/surfaces must first be
 * composited with the Window background, otherwise the refracted layer keeps partial
 * alpha and the unrefracted scene underneath leaks through as a ghost image.</p>
 */
public final class ActivityBackdropManager {
    private static final WeakHashMap<View, Entry> ENTRIES = new WeakHashMap<View, Entry>();
    private static int captureDepth;
    private static int portalMutationDepth;

    private ActivityBackdropManager() {}

    public static boolean isCapturingBackdrop() {
        return captureDepth > 0;
    }

    static boolean isPortalMutation() {
        return portalMutationDepth > 0;
    }

    static boolean isPortalContainer(View view) {
        return view instanceof PortalHost;
    }

    public static Backdrop getBackdrop(View view) {
        if (view == null) return EmptyBackdrop.INSTANCE;
        View root = resolveCaptureRoot(view);
        if (!(root instanceof ViewGroup)) return EmptyBackdrop.INSTANCE;

        Entry entry;
        synchronized (ENTRIES) {
            entry = ENTRIES.get(root);
            if (entry == null) {
                entry = new Entry((ViewGroup) root);
                ENTRIES.put(root, entry);
            }
            entry.install();
            entry.registerPortalCandidate(view);
        }
        return entry.backdrop;
    }

    private static View resolveCaptureRoot(View view) {
        if (view == null) return null;

        /*
         * Capture the Window DecorView, not only android.R.id.content.
         *
         * android.R.id.content is often transparent and Material/Card surfaces may
         * themselves be translucent. Recording that subtree preserves its alpha.
         * When the refracted RenderNode is then drawn over the still-visible real
         * page, the original (unrefracted) text/shapes leak through wherever the
         * sampled scene alpha is below 1.0. That is the "double image"/ghost the
         * device screenshots exposed.
         *
         * The portal architecture makes DecorView capture safe: all real glass
         * controls live in PortalHost and captureHardwareScene() hides that host
         * while recording, so the shared scene cannot recursively contain glass.
         */
        View decor = view.getRootView();
        return decor;
    }

    public static void invalidate(View anyViewInRoot) {
        if (anyViewInRoot == null) return;
        View root = resolveCaptureRoot(anyViewInRoot);
        if (root == null) return;
        synchronized (ENTRIES) {
            Entry entry = ENTRIES.get(root);
            if (entry != null) {
                entry.dirty = true;
                entry.luminanceDirty = true;
                entry.requestFramePump();
            }
        }
        root.postInvalidateOnAnimation();
    }

    public static void setContinuousCapture(View anyViewInRoot, boolean enabled) {
        if (anyViewInRoot == null) return;
        View root = resolveCaptureRoot(anyViewInRoot);
        if (!(root instanceof ViewGroup)) return;
        synchronized (ENTRIES) {
            Entry entry = ENTRIES.get(root);
            if (entry == null) {
                entry = new Entry((ViewGroup) root);
                ENTRIES.put(root, entry);
            }
            entry.continuousCapture = enabled;
            entry.dirty = true;
            entry.install();
            if (enabled) entry.requestFramePump();
            else entry.cancelFramePump();
        }
        root.postInvalidateOnAnimation();
    }

    public static boolean isContinuousCapture(View anyViewInRoot) {
        if (anyViewInRoot == null) return true;
        View root = resolveCaptureRoot(anyViewInRoot);
        if (root == null) return true;
        synchronized (ENTRIES) {
            Entry entry = ENTRIES.get(root);
            return entry == null || entry.continuousCapture;
        }
    }

    /**
     * Caps how often the automatic backdrop is re-recorded.
     *
     * <p>The library default re-records the whole DecorView on every traversal and
     * keeps a Choreographer frame pump alive that forces one traversal per vsync.
     * Recording the DecorView is roughly the same display-list work as drawing the
     * moving content, so a scrolling screen pays for it twice and never reaches idle.</p>
     *
     * <p>With a positive {@code intervalMs} the capture still happens on real
     * traversals (so the glass follows scrolling content), but at most once per
     * interval, and the frame pump stays off - an idle screen then costs nothing.</p>
     *
     * @param intervalMs milliseconds between captures; {@code <= 0} restores the default.
     */
    public static void setCaptureThrottle(View anyViewInRoot, long intervalMs) {
        if (anyViewInRoot == null) return;
        View root = resolveCaptureRoot(anyViewInRoot);
        if (!(root instanceof ViewGroup)) return;
        synchronized (ENTRIES) {
            Entry entry = ENTRIES.get(root);
            if (entry == null) {
                entry = new Entry((ViewGroup) root);
                ENTRIES.put(root, entry);
            }
            entry.captureThrottleMs = Math.max(0L, intervalMs);
            entry.lastCaptureMs = 0L;
            entry.dirty = true;
            entry.install();
            if (entry.captureThrottleMs <= 0L) entry.requestFramePump();
            else entry.cancelFramePump();
        }
        root.postInvalidateOnAnimation();
    }

    /**
     * Shared low-resolution luminance map used by Button/BottomTabs adaptive
     * foreground colors. It is intentionally outside the per-frame glass path.
     */
    public static float sampleAverageLuminance(View view, int sampleWidth, int sampleHeight) {
        if (view == null || sampleWidth <= 0 || sampleHeight <= 0) return -1f;
        View root = resolveCaptureRoot(view);
        if (!(root instanceof ViewGroup)) return -1f;

        Entry entry;
        synchronized (ENTRIES) {
            entry = ENTRIES.get(root);
            if (entry == null) {
                entry = new Entry((ViewGroup) root);
                ENTRIES.put(root, entry);
            }
            entry.install();
            entry.registerPortalCandidate(view);
        }
        return entry.sampleAverageLuminance(view, sampleWidth, sampleHeight);
    }

    /** Called by glass views when they are really removed, not during portal moves. */
    static void release(View view) {
        if (view == null || portalMutationDepth > 0) return;
        View root = resolveCaptureRoot(view);
        if (root == null) return;
        synchronized (ENTRIES) {
            Entry entry = ENTRIES.get(root);
            if (entry != null) entry.removeDeadPortal(view);
        }
    }

    /** 最外层 LiquidGlassOverlay 祖先（即应被 portal 的顶层 overlay）；无则返回 null。 */
    private static View findTopLevelLiquidOverlay(View view, View root) {
        if (view == null) return null;
        View top = view instanceof LiquidGlassOverlay ? view : null;
        ViewParent p = view.getParent();
        while (p instanceof View && p != root) {
            if (p instanceof LiquidGlassOverlay) top = (View) p;
            p = p.getParent();
        }
        return top;
    }

    private static boolean isTopLevelLiquidOverlay(View view, View root) {
        if (!(view instanceof LiquidGlassOverlay)) return false;
        ViewParent p = view.getParent();
        while (p instanceof View && p != root) {
            if (p instanceof PortalHost) return true;
            if (p instanceof LiquidGlassOverlay) return false;
            p = p.getParent();
        }
        return true;
    }

    private static final class Entry {
        private static final int LUMA_MAP_SIZE = 64;
        private static final long LUMA_REFRESH_MS = 120L;

        final ViewGroup root;
        final ViewGroup portalParent;
        final WindowBackdrop backdrop;
        final WeakHashMap<View, PortalRecord> portals = new WeakHashMap<View, PortalRecord>();

        PortalHost portalHost;
        Object backdropNode;
        boolean dirty = true;
        boolean continuousCapture = true;
        boolean installed;
        boolean hasCapture;
        boolean capturing;
        boolean framePosted;

        Bitmap luminanceMap;
        long lastLuminanceCapture;
        boolean luminanceDirty = true;

        /**
         * Minimum time between two automatic scene captures, in milliseconds.
         * {@code <= 0} keeps the library default of capturing on every traversal
         * while the Choreographer frame pump forces a traversal per vsync.
         */
        long captureThrottleMs;
        long lastCaptureMs;

        final ViewTreeObserver.OnPreDrawListener preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                syncPortals();
                if (captureThrottleMs <= 0L) {
                    if (continuousCapture || dirty || !hasCapture) captureHardwareScene();
                    if (continuousCapture) requestFramePump();
                } else if (canCaptureNow()) {
                    if (continuousCapture || dirty || !hasCapture) captureHardwareScene();
                }
                return true;
            }
        };

        boolean canCaptureNow() {
            return android.os.SystemClock.uptimeMillis() - lastCaptureMs >= captureThrottleMs;
        }

        final View.OnAttachStateChangeListener attachListener = new View.OnAttachStateChangeListener() {
            public void onViewAttachedToWindow(View v) {
                install();
                dirty = true;
                requestFramePump();
            }

            public void onViewDetachedFromWindow(View v) {
                cancelFramePump();
                uninstall();
                clear();
                synchronized (ENTRIES) {
                    ENTRIES.remove(root);
                }
            }
        };

        final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
            public void doFrame(long frameTimeNanos) {
                framePosted = false;
                if (!installed || !continuousCapture || captureThrottleMs > 0L
                        || portals.isEmpty() || !root.isAttachedToWindow()) return;

                // Keep one normal Android traversal alive per vsync.  The actual
                // scene capture still happens from OnPreDraw using the proven
                // Fix23 portal-separated hierarchy; this callback never records
                // the View tree itself.
                root.postInvalidateOnAnimation();
                requestFramePump();
            }
        };

        Entry(ViewGroup root) {
            this.root = root;
            View decor = root.getRootView();
            this.portalParent = decor instanceof ViewGroup ? (ViewGroup) decor : root;
            this.backdrop = new WindowBackdrop(this);
        }

        void install() {
            if (installed) {
                if (continuousCapture) requestFramePump();
                return;
            }
            ViewTreeObserver observer = root.getViewTreeObserver();
            if (observer != null && observer.isAlive()) {
                observer.addOnPreDrawListener(preDrawListener);
                root.addOnAttachStateChangeListener(attachListener);
                installed = true;
                if (continuousCapture) requestFramePump();
            }
        }

        void uninstall() {
            if (!installed) return;
            cancelFramePump();
            ViewTreeObserver observer = root.getViewTreeObserver();
            if (observer != null && observer.isAlive()) observer.removeOnPreDrawListener(preDrawListener);
            root.removeOnAttachStateChangeListener(attachListener);
            installed = false;
        }

        void requestFramePump() {
            if (framePosted || !installed || !continuousCapture || captureThrottleMs > 0L
                    || portals.isEmpty()) return;
            framePosted = true;
            try {
                Choreographer.getInstance().postFrameCallback(frameCallback);
            } catch (Throwable ignored) {
                framePosted = false;
                root.postInvalidateOnAnimation();
            }
        }

        void cancelFramePump() {
            if (!framePosted) return;
            try {
                Choreographer.getInstance().removeFrameCallback(frameCallback);
            } catch (Throwable ignored) {
            }
            framePosted = false;
        }

        void clear() {
            cancelFramePump();
            backdropNode = null;
            hasCapture = false;
            if (luminanceMap != null) {
                luminanceMap.recycle();
                luminanceMap = null;
            }
            portals.clear();
            portalHost = null;
        }

        void registerPortalCandidate(final View view) {
            if (view == null || !isTopLevelLiquidOverlay(view, root)) return;
            if (view.getParent() instanceof PortalHost) {
                requestFramePump();
                return;
            }
            if (portals.containsKey(view)) {
                requestFramePump();
                return;
            }

            // Reparent after the current attach/layout callback has unwound.
            root.post(new Runnable() {
                public void run() {
                    portal(view);
                }
            });
        }

        void ensurePortalHost() {
            if (portalHost != null && portalHost.getParent() == portalParent) return;
            portalHost = new PortalHost(root.getContext(), this);
            portalHost.setClipChildren(false);
            portalHost.setClipToPadding(false);
            portalHost.setBackgroundColor(Color.TRANSPARENT);
            portalParent.addView(portalHost, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            portalHost.bringToFront();
        }

        void portal(View target) {
            if (target == null || target.getParent() instanceof PortalHost) return;
            if (!isTopLevelLiquidOverlay(target, root)) return;
            ViewParent parentValue = target.getParent();
            if (!(parentValue instanceof ViewGroup)) return;
            ViewGroup parent = (ViewGroup) parentValue;
            if (parent == root && target == portalHost) return;

            int index = parent.indexOfChild(target);
            if (index < 0) return;
            ViewGroup.LayoutParams originalParams = target.getLayoutParams();
            PortalPlaceholder placeholder = new PortalPlaceholder(target);
            PortalRecord record = new PortalRecord(target, placeholder, parent, index, originalParams);

            ensurePortalHost();
            portalMutationDepth++;
            try {
                parent.removeViewAt(index);
                parent.addView(placeholder, Math.min(index, parent.getChildCount()), originalParams);
                FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                portalHost.addView(target, overlayParams);
                portals.put(target, record);
            } finally {
                portalMutationDepth--;
            }

            dirty = true;
            luminanceDirty = true;
            parent.requestLayout();
            portalHost.requestLayout();
            root.postInvalidateOnAnimation();
            requestFramePump();
        }

        void removeDeadPortal(View target) {
            PortalRecord record = portals.get(target);
            if (record == null) return;
            if (target.getParent() == portalHost && target.isAttachedToWindow()) return;
            portals.remove(target);
            if (portals.isEmpty()) cancelFramePump();
        }

        void syncPortals() {
            if (portalHost == null || portalHost.getParent() != portalParent) return;
            int[] hostLocation = new int[2];
            portalHost.getLocationInWindow(hostLocation);
            ArrayList<View> dead = null;

            for (View target : new ArrayList<View>(portals.keySet())) {
                PortalRecord record = portals.get(target);
                if (record == null || target == null || record.placeholder.getParent() == null) {
                    if (dead == null) dead = new ArrayList<View>();
                    dead.add(target);
                    continue;
                }

                int w = record.placeholder.getWidth();
                int h = record.placeholder.getHeight();
                if (w <= 0 || h <= 0) continue;

                int[] place = new int[2];
                record.placeholder.getLocationInWindow(place);
                int left = place[0] - hostLocation[0];
                int top = place[1] - hostLocation[1];

                if (target.getMeasuredWidth() != w || target.getMeasuredHeight() != h) {
                    target.measure(
                            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
                }
                if (target.getLeft() != left || target.getTop() != top ||
                        target.getWidth() != w || target.getHeight() != h) {
                    // layout() changes the portal base position without touching the target's
                    // own translationX/Y used by LiquidButton/BottomTabs press animation.
                    target.layout(left, top, left + w, top + h);
                }
            }

            if (dead != null) {
                for (int i = 0; i < dead.size(); i++) portals.remove(dead.get(i));
            }
        }

        void ensureCapture() {
            if (capturing || captureDepth > 0) return;
            syncPortals();
            if (!hasCapture || dirty) captureHardwareScene();
        }

        void captureHardwareScene() {
            if (capturing || captureDepth > 0 || !Platform.isRenderNodeSupported()) return;
            int width = root.getWidth();
            int height = root.getHeight();
            if (width <= 0 || height <= 0) return;

            capturing = true;
            captureDepth++;
            int oldPortalVisibility = View.VISIBLE;
            boolean portalHidden = false;
            ArrayList<HiddenOverlay> hidden = new ArrayList<HiddenOverlay>();
            try {
                if (portalHost != null && portalParent == root && portalHost.getParent() == root) {
                    oldPortalVisibility = portalHost.getVisibility();
                    portalHost.setTransitionVisibility(View.INVISIBLE);
                    portalHidden = true;
                }

                // Normally the portal has already removed every top-level glass from
                // the captured content subtree. Keep this small transition-visibility
                // guard for the first frame before a posted portal move completes.
                Api29.hideLiquidOverlays(root, hidden);
                backdropNode = Api29.recordHardware(root, backdropNode, width, height);
                hasCapture = backdropNode != null;
                dirty = false;
                lastCaptureMs = android.os.SystemClock.uptimeMillis();
                luminanceDirty = true;
            } finally {
                Api29.restoreLiquidOverlays(hidden);
                if (portalHidden && portalHost != null) {
                    portalHost.setTransitionVisibility(oldPortalVisibility);
                }
                captureDepth--;
                capturing = false;
            }
        }

        float sampleAverageLuminance(View view, int sampleWidth, int sampleHeight) {
            if (view == null || view.getWidth() <= 0 || view.getHeight() <= 0) return -1f;
            ensureLuminanceMap();
            Bitmap map = luminanceMap;
            if (map == null || map.isRecycled() || root.getWidth() <= 0 || root.getHeight() <= 0) return -1f;

            int[] viewLocation = new int[2];
            int[] rootLocation = new int[2];
            view.getLocationInWindow(viewLocation);
            root.getLocationInWindow(rootLocation);
            float left = viewLocation[0] - rootLocation[0];
            float top = viewLocation[1] - rootLocation[1];
            float width = view.getWidth();
            float height = view.getHeight();

            double sum = 0.0;
            int count = 0;
            int sxCount = Math.max(1, sampleWidth);
            int syCount = Math.max(1, sampleHeight);
            for (int y = 0; y < syCount; y++) {
                float fy = top + height * ((y + 0.5f) / syCount);
                int my = clamp((int) (fy / root.getHeight() * map.getHeight()), 0, map.getHeight() - 1);
                for (int x = 0; x < sxCount; x++) {
                    float fx = left + width * ((x + 0.5f) / sxCount);
                    int mx = clamp((int) (fx / root.getWidth() * map.getWidth()), 0, map.getWidth() - 1);
                    int argb = map.getPixel(mx, my);
                    if (Color.alpha(argb) == 0) continue;
                    float r = Color.red(argb) / 255f;
                    float g = Color.green(argb) / 255f;
                    float b = Color.blue(argb) / 255f;
                    sum += 0.2126 * r + 0.7152 * g + 0.0722 * b;
                    count++;
                }
            }
            return count == 0 ? -1f : (float) (sum / count);
        }

        void ensureLuminanceMap() {
            long now = android.os.SystemClock.uptimeMillis();
            if (luminanceMap != null && !luminanceDirty && now - lastLuminanceCapture < LUMA_REFRESH_MS) return;
            if (captureDepth > 0 || root.getWidth() <= 0 || root.getHeight() <= 0) return;

            if (luminanceMap == null || luminanceMap.isRecycled() ||
                    luminanceMap.getWidth() != LUMA_MAP_SIZE || luminanceMap.getHeight() != LUMA_MAP_SIZE) {
                if (luminanceMap != null && !luminanceMap.isRecycled()) luminanceMap.recycle();
                luminanceMap = Bitmap.createBitmap(LUMA_MAP_SIZE, LUMA_MAP_SIZE, Bitmap.Config.ARGB_8888);
            }
            luminanceMap.eraseColor(Color.TRANSPARENT);

            captureDepth++;
            int oldPortalVisibility = View.VISIBLE;
            boolean portalHidden = false;
            try {
                if (portalHost != null && portalParent == root && portalHost.getParent() == root) {
                    oldPortalVisibility = portalHost.getVisibility();
                    portalHost.setTransitionVisibility(View.INVISIBLE);
                    portalHidden = true;
                }
                Canvas c = new Canvas(luminanceMap);
                c.scale(LUMA_MAP_SIZE / (float) root.getWidth(), LUMA_MAP_SIZE / (float) root.getHeight());
                root.draw(c);
                lastLuminanceCapture = now;
                luminanceDirty = false;
            } catch (Throwable ignored) {
                // Keep the last valid map if a custom View cannot draw on software Canvas.
            } finally {
                if (portalHidden && portalHost != null) portalHost.setTransitionVisibility(oldPortalVisibility);
                captureDepth--;
            }
        }

        static int clamp(int value, int min, int max) {
            return value < min ? min : (value > max ? max : value);
        }
    }

    private static final class WindowBackdrop implements Backdrop {
        final Entry entry;

        WindowBackdrop(Entry entry) {
            this.entry = entry;
        }

        public boolean isCoordinatesDependent() {
            return true;
        }

        public void drawBackdrop(Canvas canvas, LiquidGlassView owner) {
            if (canvas == null || owner == null) return;
            entry.ensureCapture();
            if (entry.backdropNode == null || !Platform.isRenderNodeSupported() || !canvas.isHardwareAccelerated()) return;

            // 防自环崩溃：顶层 overlay 尚未被 portal 移出原父容器时，backdropNode 的录制树
            // （root.draw → holder → tabs → glass）仍包含本 glass，此时再把 backdropNode 写入
            // glass 的 displayList 会形成 RenderNode 环，RenderThread prepareTreeImpl 无限递归 → SIGSEGV。
            // 未就绪则跳过本帧 backdrop 绘制并触发 portal，portal 完成后自动恢复。
            View topOverlay = findTopLevelLiquidOverlay(owner, entry.root);
            if (topOverlay != null && !(topOverlay.getParent() instanceof PortalHost)) {
                entry.registerPortalCandidate(topOverlay);
                return;
            }

            int[] ownerLocation = new int[2];
            int[] rootLocation = new int[2];
            owner.getLocationInWindow(ownerLocation);
            entry.root.getLocationInWindow(rootLocation);

            int save = canvas.save();
            owner.concatInverseLayerTransform(canvas);
            canvas.translate(rootLocation[0] - ownerLocation[0], rootLocation[1] - ownerLocation[1]);
            Api29.draw(canvas, entry.backdropNode);
            canvas.restoreToCount(save);
        }
    }

    private static final class HiddenOverlay {
        final View view;
        final int visibility;
        HiddenOverlay(View view, int visibility) {
            this.view = view;
            this.visibility = visibility;
        }
    }

    private static final class PortalRecord {
        final WeakReference<View> target;
        final PortalPlaceholder placeholder;
        final WeakReference<ViewGroup> originalParent;
        final int originalIndex;
        final ViewGroup.LayoutParams originalLayoutParams;

        PortalRecord(View target, PortalPlaceholder placeholder, ViewGroup parent,
                     int originalIndex, ViewGroup.LayoutParams originalLayoutParams) {
            this.target = new WeakReference<View>(target);
            this.placeholder = placeholder;
            this.originalParent = new WeakReference<ViewGroup>(parent);
            this.originalIndex = originalIndex;
            this.originalLayoutParams = originalLayoutParams;
        }
    }

    /** Transparent layout participant that keeps the original View's measured slot. */
    private static final class PortalPlaceholder extends View {
        private final WeakReference<View> target;

        PortalPlaceholder(View target) {
            super(target.getContext());
            this.target = new WeakReference<View>(target);
            setWillNotDraw(true);
            setClickable(false);
            setFocusable(false);
        }

        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            View v = target.get();
            if (v != null) {
                v.measure(widthMeasureSpec, heightMeasureSpec);
                setMeasuredDimension(v.getMeasuredWidth(), v.getMeasuredHeight());
            } else {
                setMeasuredDimension(
                        View.resolveSize(0, widthMeasureSpec),
                        View.resolveSize(0, heightMeasureSpec));
            }
        }
    }

    /** Top-level transparent portal where actual glass controls are rendered/touched. */
    private static final class PortalHost extends FrameLayout {
        final Entry entry;

        PortalHost(android.content.Context context, Entry entry) {
            super(context);
            this.entry = entry;
            setWillNotDraw(true);
            setClipChildren(false);
            setClipToPadding(false);
        }

        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = View.MeasureSpec.getSize(widthMeasureSpec);
            int height = View.MeasureSpec.getSize(heightMeasureSpec);
            setMeasuredDimension(width, height);
            for (View target : new ArrayList<View>(entry.portals.keySet())) {
                PortalRecord record = entry.portals.get(target);
                if (target == null || record == null) continue;
                int w = record.placeholder.getMeasuredWidth();
                int h = record.placeholder.getMeasuredHeight();
                if (w > 0 && h > 0) {
                    target.measure(
                            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
                }
            }
        }

        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            entry.syncPortals();
        }
    }

    private static final class Api29 {
        private Api29() {}

        static void hideLiquidOverlays(View root, ArrayList<HiddenOverlay> hidden) {
            collectAndHide(root, hidden);
        }

        private static void collectAndHide(View view, ArrayList<HiddenOverlay> hidden) {
            if (view == null) return;
            if (view instanceof LiquidGlassOverlay) {
                int visibility = view.getVisibility();
                if (visibility == View.VISIBLE) {
                    hidden.add(new HiddenOverlay(view, visibility));
                    view.setTransitionVisibility(View.INVISIBLE);
                }
                return;
            }
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) {
                    collectAndHide(group.getChildAt(i), hidden);
                }
            }
        }

        static void restoreLiquidOverlays(ArrayList<HiddenOverlay> hidden) {
            for (int i = hidden.size() - 1; i >= 0; i--) {
                HiddenOverlay item = hidden.get(i);
                if (item.view != null) item.view.setTransitionVisibility(item.visibility);
            }
            hidden.clear();
        }

        static Object recordHardware(View root, Object oldNode, int width, int height) {
            RenderNode node = oldNode instanceof RenderNode
                    ? (RenderNode) oldNode
                    : new RenderNode("LiquidGlassAutoScene");
            node.setPosition(0, 0, width, height);
            RecordingCanvas canvas = node.beginRecording(width, height);
            try {
                root.draw(canvas);
            } finally {
                node.endRecording();
            }
            return node;
        }

        static void draw(Canvas canvas, Object node) {
            canvas.drawRenderNode((RenderNode) node);
        }
    }
}
