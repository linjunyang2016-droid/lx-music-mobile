package cn.toside.music.mobile;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.TextView;

import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactRootView;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.reactnativenavigation.NavigationActivity;

public class MainActivity extends NavigationActivity {

    private TextView debugOverlay;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ReactRootView lastReactRootView;
    private String lastKeyInfo = "(none)";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isRunningOnTV()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isRunningOnTV()) {
            enableDpadFocusOnAllViews(getWindow().getDecorView());
            getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        enableDpadFocusOnAllViews(getWindow().getDecorView());
                        findAndPrepareReactRootView();
                        updateDebugOverlay();
                    }
                }
            );
            showDebugOverlay();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        hideDebugOverlay();
    }

    /**
     * 不再用 Android focus chain(focusSearch 在 RN 上不工作)。
     * 改用 JS DeviceEventEmitter 把 D-pad 事件发给 React 层,
     * 由 React 层用自己维护的 focusable 列表 + 几何最近邻算法定位目标。
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean handled = false;
        if (isRunningOnTV() && event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            String keyName = keyCodeToName(keyCode);
            lastKeyInfo = String.format("key=%s action=DOWN", keyName);
            // 把事件转发给 JS 层
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP
                || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                || keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_BACK
                || keyCode == KeyEvent.KEYCODE_MENU) {
                sendKeyEventToJS(keyCode);
                handled = true;  // 拦截,不传给 super(避免被 RNN 系统消费)
            }
            updateDebugOverlay();
        }
        return handled || super.dispatchKeyEvent(event);
    }

    private String keyCodeToName(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP: return "DPAD_UP";
            case KeyEvent.KEYCODE_DPAD_DOWN: return "DPAD_DOWN";
            case KeyEvent.KEYCODE_DPAD_LEFT: return "DPAD_LEFT";
            case KeyEvent.KEYCODE_DPAD_RIGHT: return "DPAD_RIGHT";
            case KeyEvent.KEYCODE_DPAD_CENTER: return "DPAD_CENTER";
            case KeyEvent.KEYCODE_BACK: return "BACK";
            case KeyEvent.KEYCODE_MENU: return "MENU";
            default: return "K" + keyCode;
        }
    }

    private void sendKeyEventToJS(int keyCode) {
        try {
            ReactInstanceManager rim = getReactInstanceManager();
            if (rim == null) return;
            ReactContext ctx = (ReactContext) rim.getCurrentReactContext();
            if (ctx == null) return;
            String name = keyCodeToName(keyCode);
            ctx.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("TV_DPAD_EVENT", name);
        } catch (Exception e) {
            // ignore
        }
    }

    private void findAndPrepareReactRootView() {
        if (lastReactRootView != null) return;
        View found = findReactRootView(getWindow().getDecorView());
        if (found != null) {
            lastReactRootView = (ReactRootView) found;
            lastReactRootView.setFocusableInTouchMode(true);
            lastReactRootView.setFocusable(true);
            enableDpadFocusOnAllViews(lastReactRootView);
        }
    }

    private View findReactRootView(View root) {
        if (root instanceof ReactRootView) return root;
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View f = findReactRootView(vg.getChildAt(i));
                if (f != null) return f;
            }
        }
        return null;
    }

    private void enableDpadFocusOnAllViews(View view) {
        if (view == null) return;
        String pkg = view.getContext() != null ? view.getContext().getPackageName() : "";
        if (pkg != null && !pkg.equals(getPackageName())) return;
        view.setFocusableInTouchMode(true);
        view.setFocusable(true);
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                enableDpadFocusOnAllViews(vg.getChildAt(i));
            }
        }
    }

    private void showDebugOverlay() {
        if (debugOverlay != null) return;
        debugOverlay = new TextView(this);
        debugOverlay.setBackgroundColor(Color.argb(200, 200, 0, 0));
        debugOverlay.setTextColor(Color.WHITE);
        debugOverlay.setTextSize(10);
        debugOverlay.setPadding(10, 5, 10, 5);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP;
        getWindowManager().addView(debugOverlay, params);
        handler.postDelayed(new Runnable() {
            @Override public void run() { updateDebugOverlay(); handler.postDelayed(this, 500); }
        }, 500);
    }

    private void hideDebugOverlay() {
        handler.removeCallbacksAndMessages(null);
        if (debugOverlay != null) {
            try { getWindowManager().removeView(debugOverlay); } catch (Exception e) {}
            debugOverlay = null;
        }
    }

    private void updateDebugOverlay() {
        if (debugOverlay == null) return;
        View focused = getWindow().getDecorView().findFocus();
        String focusInfo = (focused == null) ? "(none)" :
            String.format("%s focusable=%s", focused.getClass().getSimpleName(), focused.isFocusable());
        debugOverlay.setText("KEY: " + lastKeyInfo + " | FOCUS: " + focusInfo);
    }

    private boolean isRunningOnTV() {
        PackageManager pm = getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
                || pm.hasSystemFeature("android.software.leanback");
    }
}
