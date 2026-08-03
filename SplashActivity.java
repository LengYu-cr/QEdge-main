//
// Decompiled by Jadx - 665ms
//
package com.kugou.android.app.splash;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import c70.c1;
import c70.g1;
import c70.j2;
import c70.k;
import com.kugou.android.app.MediaActivity;
import com.kugou.android.app.s;
import com.kugou.common.app.KGCommonApplication;
import com.kugou.common.permission.KGPermission;
import com.kugou.common.privacy.BasicWebActivity;
import com.kugou.common.privacy.e;
import com.kugou.common.privacy.f;
import com.kugou.common.setting.CommonSettingPrefs;
import com.kugou.framework.service.ipc.core.RemoteConnector;
import d3.g;
import le.d;
import le.o;
import le.p;
import le.w;
import t4.c;

@TargetApi(11)
public class SplashActivity extends BaseSplashActivity {
    public static final String BOOT_BY_SHORTCUT = "BOOT_BY_SHORTCUT";
    private boolean y = false;

    class a implements Runnable {
        a() {
        }

        @Override
        public void run() {
            k.b(SplashActivity.this.getIntent());
        }
    }

    public class b implements Runnable {

        class a implements Runnable {
            a() {
            }

            @Override
            public void run() {
                RemoteConnector.c().g();
                SplashActivity.this.w();
            }
        }

        b() {
        }

        @Override
        public void run() {
            KGPermission.enableChecker = true;
            com.kugou.android.freemode.a.a.Q(false);
            new a().run();
            KGCommonApplication.setHasBasicPermission(true);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void s() {
        startActivity(new Intent((Context) this, (Class<?>) BasicWebActivity.class));
        finish();
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void t() {
        ((BaseSplashActivity) this).n.g("selectSplashAndShow() 2");
        Log.d("zlx_permission", "splash gotoGdtSplashActivity" + this);
        if (j()) {
            return;
        }
        ((BaseSplashActivity) this).n.g("selectSplashAndShow() 3");
        try {
            Intent intent = new Intent();
            w wVar = w.a;
            if (wVar.j()) {
                wVar.l();
                intent.setClass(this, MediaActivity.class);
                intent.putExtra("key_need_show_ad_view", true);
            } else {
                intent.setClass(this, GdtSplashActivity.class);
            }
            o("", "广点通");
            if (getIntent().getExtras() != null) {
                intent.putExtras(getIntent().getExtras());
            }
            if (getIntent().getData() != null) {
                intent.setData(getIntent().getData());
            }
            if (getIntent().getAction() != null) {
                intent.setAction(getIntent().getAction());
            }
            startActivity(intent);
            overridePendingTransition(0, 0);
            ((BaseSplashActivity) this).n.h();
            c.i().u(((BaseSplashActivity) this).n);
            finish();
        } catch (Throwable th) {
            if (c1.d) {
                c1.l(Log.getStackTraceString(th));
            }
            o.a(th, 1000, false);
            finish();
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private boolean u() {
        if (!isTaskRoot()) {
            if (c1.d) {
                c1.i("SplashActivity", "isTaskRoot false");
            }
            Intent intent = getIntent();
            String action = intent.getAction();
            if (intent.hasCategory("android.intent.category.LAUNCHER") && TextUtils.equals(action, "android.intent.action.MAIN")) {
                finish();
                if (c1.d) {
                    c1.i("SplashActivity", "finish true");
                    return true;
                }
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean v() {
        boolean z;
        if (((BaseSplashActivity) this).c < 0) {
            ((BaseSplashActivity) this).c = j2.a(getIntent(), BOOT_BY_SHORTCUT, false) ? 1 : 0;
        }
        if (c1.d) {
            StringBuilder sb = new StringBuilder();
            sb.append("isEspecialWay() = ");
            if (((BaseSplashActivity) this).c == 1) {
                z = true;
            } else {
                z = false;
            }
            sb.append(z);
            c1.m("burone-", sb.toString());
        }
        if (((BaseSplashActivity) this).c != 1) {
            return false;
        }
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void x() {
        f.f(this, new b());
    }

    /* JADX WARN: Multi-variable type inference failed */
    protected boolean b() {
        ((BaseSplashActivity) this).n.g("onCreate() 3.1");
        if (f.e()) {
            if (e.b().d(true)) {
                return false;
            }
            return u();
        }
        ((BaseSplashActivity) this).n.g("onCreate() 3.2");
        g1.l(new a());
        ((BaseSplashActivity) this).n.g("onCreate() 3.3");
        if (u()) {
            return true;
        }
        ((BaseSplashActivity) this).n.g("onCreate() 3.4");
        if (new le.b(this).a(getIntent())) {
            return true;
        }
        ((BaseSplashActivity) this).n.g("onCreate() 3.5");
        if (s.i(getIntent())) {
            h();
            return true;
        }
        ((BaseSplashActivity) this).n.g("onCreate() 3.6");
        if (TextUtils.equals(getIntent().getAction(), "com.kugou.android.action.invoke_for_shiqu_app") && getIntent().getBooleanExtra("is_from_kan", false)) {
            h();
        }
        ((BaseSplashActivity) this).n.g("onCreate() 3.7");
        if (getIntent().getBooleanExtra("key_open_app_without_splash", false)) {
            h();
        }
        ((BaseSplashActivity) this).n.g("onCreate() 3.8");
        return super.b();
    }

    protected void c() {
        ((BaseSplashActivity) this).n.g("checkPermissionOrSelectSplashAndShow()");
        if (e.b().c(false) && !e.b().d(true)) {
            s();
            return;
        }
        if (f.e()) {
            if (c1.d) {
                c1.i("zbj_boot", "checkPermissionOrSelectSplashAndShow: getPreferenceVersion = " + CommonSettingPrefs.G0().L1());
                c1.i("zbj_boot", "checkPermissionOrSelectSplashAndShow: PrivacyUtil.neverAgreed() = " + f.e());
            }
            x();
            return;
        }
        w();
    }

    protected t4.a g() {
        return t4.e.q();
    }

    /* JADX WARN: Multi-variable type inference failed */
    protected void h() {
        ((BaseSplashActivity) this).n.g("selectSplashAndShow() 4");
        c1.V("zlx_permission", "splash gotoMediaActivity" + this);
        if (j()) {
            return;
        }
        ((BaseSplashActivity) this).n.g("selectSplashAndShow() 5");
        try {
            Log.d("zlx_permission", "start MediaActivity" + this);
            getIntent().setClass(this, MediaActivity.class);
            startActivity(getIntent());
            overridePendingTransition(((BaseSplashActivity) this).d, ((BaseSplashActivity) this).e);
            if (c1.d) {
                c1.m("burone-", "start MediaActivity ....");
            }
            ((BaseSplashActivity) this).n.h();
            c.i().u(((BaseSplashActivity) this).n);
            finish();
        } catch (Throwable th) {
            Log.d("zlx_permission", "start MediaActivity  error " + this);
            if (c1.d) {
                c1.l(Log.getStackTraceString(th));
            }
            o.a(th, 1000, false);
            finish();
        }
    }

    public void idleHandle() {
    }

    protected void m(boolean z) {
        if (v()) {
            return;
        }
        d dVar = new d();
        ((BaseSplashActivity) this).k = dVar;
        dVar.h((pt.a) null);
    }

    /* JADX WARN: Multi-variable type inference failed */
    protected void n() {
        KGCommonApplication.isExiting = false;
        if (com.kugou.common.utils.b.O2(this)) {
            if (c1.d) {
                c1.i("SplashConstants", "SplashActivity isCover");
            }
            this.y = true;
            h00.a.R1(true);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    protected void onActivityResult(int i, int i2, Intent intent) {
        super/*android.app.Activity*/.onActivityResult(i, i2, intent);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void onAttachedToWindow() {
        super/*android.app.Activity*/.onAttachedToWindow();
        ((BaseSplashActivity) this).n.g("onAttachedToWindow()");
        if (!this.y && !f.e()) {
            t4.e.q().s(true);
            r();
            ((BaseSplashActivity) this).n.g("waitForResourcesPrepared begin");
        } else {
            g().j(true);
            c();
        }
    }

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    /* JADX WARN: Multi-variable type inference failed */
    protected void onNewIntent(Intent intent) {
        super/*android.app.Activity*/.onNewIntent(intent);
    }

    protected void onResume() {
        super.onResume();
        ((BaseSplashActivity) this).n.g("onResume()");
    }

    /* JADX WARN: Multi-variable type inference failed */
    protected void onStart() {
        super/*android.app.Activity*/.onStart();
        ((BaseSplashActivity) this).n.g("onStart()");
    }

    protected void w() {
        String str;
        c1.V("zlx_permission", "start selectSplashAndShow" + this);
        ((BaseSplashActivity) this).n.g("selectSplashAndShow() 1");
        if (v30.e.p() && !e() && p.a() && wt.f.b()) {
            t();
            return;
        }
        if (!v30.e.p()) {
            c1.A("lzq-gdt", "preload non hot start ad resource");
            g.c().a(false);
        }
        h();
        if (!v30.e.p()) {
            str = "新用户";
        } else {
            str = "闪屏广告关闭";
        }
        o(str, "");
    }
}
