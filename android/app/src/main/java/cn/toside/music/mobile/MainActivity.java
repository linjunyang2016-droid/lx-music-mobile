package cn.toside.music.mobile;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
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

import com.reactnativenavigation.NavigationActivity;

public class MainActivity extends NavigationActivity {

    private TextView debugOverlay;
    private final Handler handler = new Handler(Looper.getMainLooper());

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
            // 启动时 + 每次布局变化时让 RN view 树所有节点可 focus
            enableDpadFocusOnAllViews(getWindow().getDecorView());
            getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        enableDpadFocusOnAllViews(getWindow().getDecorView());
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

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean handled = false;
        if (isRunningOnTV() && event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP
                || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                View focused = getWindow().getDecorView().findFocus();
                if (focused == null) {
                    View target = findFirstFocusable(getWindow().getDecorView());
                    if (target != null) {
                        target.requestFocus();
                        handled = true;
                    }
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                    focused.performClick();
                    handled = true;
                } else {
                    View target = focused.focusSearch(directionOf(keyCode));
                    if (target != null && target != focused) {
                        target.requestFocus();
                        handled = true;
                    }
                }
            }
            updateDebugOverlay();
        }
        // 不管 handled 与否,都继续让 super 处理(避免屏蔽系统行为)
        return super.dispatchKeyEvent(event) || handled;
    }

    private int directionOf(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP: return View.FOCUS_UP;
            case KeyEvent.KEYCODE_DPAD_DOWN: return View.FOCUS_DOWN;
            case KeyEvent.KEYCODE_DPAD_LEFT: return View.FOCUS_LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return View.FOCUS_RIGHT;
            default: return View.FOCUS_FORWARD;
        }
    }

    private View findFirstFocusable(View root) {
        if (root == null) return null;
        if (root.isFocusable() && root.isFocusableInTouchMode()) return root;
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View f = findFirstFocusable(vg.getChildAt(i));
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

    /**
     * TV 适配调试:屏幕顶部红色横条显示当前焦点 view 的 id / class / focusable 状态
     * 关键诊断信息 — 用户看到红色条变化就知道焦点是不是真的在动
     */
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
            WindowManager.LayoutParams.TYPE_APPLICATION,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
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
        if (focused == null) {
            debugOverlay.setText("FOCUS: (none) — press D-pad to give focus");
        } else {
            debugOverlay.setText(String.format(
                "FOCUS: id=%s class=%s focusable=%s touch=%s size=%dx%d pos=(%d,%d)",
                focused.getId() == View.NO_ID ? "(no-id)" : String.valueOf(focused.getId()),
                focused.getClass().getSimpleName(),
                focused.isFocusable(),
                focused.isFocusableInTouchMode(),
                focused.getWidth(), focused.getHeight(),
                focused.getLeft(), focused.getTop()
            ));
        }
    }

    private boolean isRunningOnTV() {
        PackageManager pm = getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
                || pm.hasSystemFeature("android.software.leanback");
    }
}
