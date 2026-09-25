package com.liquidglass.java.miba;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import java.util.ArrayList;
import java.util.WeakHashMap;

/**
 * Keeps LiquidGlass visual overflow (outer shadow, blur/lens padding and press
 * deformation) from being clipped by ordinary Android ViewGroup ancestors.
 *
 * <p>The original dedicated LiquidGlassBackdropLayout already disables
 * clipChildren/clipToPadding. Auto-backdrop mode allows glass controls to live
 * inside arbitrary LinearLayout/FrameLayout/ConstraintLayout hierarchies, whose
 * defaults commonly clip children. This manager temporarily disables those two
 * clipping flags while at least one LiquidGlassView is attached below a parent,
 * then restores the parent's original values when the last glass view detaches.</p>
 */
final class AncestorClipManager {
    private static final WeakHashMap<ViewGroup, State> STATES = new WeakHashMap<ViewGroup, State>();
    private static final WeakHashMap<View, ArrayList<ViewGroup>> OWNERS = new WeakHashMap<View, ArrayList<ViewGroup>>();

    private AncestorClipManager() {}

    static void attach(View glass) {
        if (glass == null) return;
        synchronized (STATES) {
            if (OWNERS.containsKey(glass)) return;

            ArrayList<ViewGroup> parents = new ArrayList<ViewGroup>();
            ViewParent p = glass.getParent();
            while (p instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) p;
                parents.add(group);

                State state = STATES.get(group);
                if (state == null) {
                    state = new State(group.getClipChildren(), group.getClipToPadding());
                    STATES.put(group, state);
                }
                state.refCount++;

                // This is exactly what LiquidGlassBackdropLayout does itself.
                if (group.getClipChildren()) group.setClipChildren(false);
                if (group.getClipToPadding()) group.setClipToPadding(false);

                // AutoPortal already moved the glass above the ordinary content tree.
                // The portal host itself is the only ancestor whose clipping must be
                // disabled; do not modify DecorView/system-window clipping flags.
                if (ActivityBackdropManager.isPortalContainer(group)) break;

                p = group.getParent();
            }
            OWNERS.put(glass, parents);
        }
    }

    static void detach(View glass) {
        if (glass == null) return;
        synchronized (STATES) {
            ArrayList<ViewGroup> parents = OWNERS.remove(glass);
            if (parents == null) return;

            for (int i = parents.size() - 1; i >= 0; i--) {
                ViewGroup group = parents.get(i);
                State state = STATES.get(group);
                if (state == null) continue;

                state.refCount--;
                if (state.refCount <= 0) {
                    // Restore only after the last glass descendant is gone.
                    group.setClipChildren(state.originalClipChildren);
                    group.setClipToPadding(state.originalClipToPadding);
                    STATES.remove(group);
                }
            }
            parents.clear();
        }
    }

    private static final class State {
        final boolean originalClipChildren;
        final boolean originalClipToPadding;
        int refCount;

        State(boolean clipChildren, boolean clipToPadding) {
            originalClipChildren = clipChildren;
            originalClipToPadding = clipToPadding;
        }
    }
}
