package cn.toside.music.mobile;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.reactnativenavigation.NavigationActivity;

public class MainActivity extends NavigationActivity {

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
            // 启动时 + 每次布局变化时,让 RN view 树所有节点可 focus
            enableDpadFocusOnAllViews(getWindow().getDecorView());
            getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        enableDpadFocusOnAllViews(getWindow().getDecorView());
                    }
                }
            );
        }
    }

    /**
     * TV 适配最关键的一步:自己处理 D-pad key events。
     *
     * 协议弹窗能用是因为 AlertDialog 是 Android 系统组件,系统 focus chain 默认管它。
     * RN 主界面不能用是因为 RN View 虽然 setFocusable(true),
     * 但 ReactRootView 默认不让 D-pad 事件传给子 view。
     *
     * 这里拦截 D-pad 事件,自己 findFocus + focusSearch,绕过这个限制。
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (isRunningOnTV() && event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP
                || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                View focused = getWindow().getDecorView().findFocus();
                if (focused == null) {
                    // 没有 focus 的 view,给第一个可 focus 的 view focus
                    View target = findFirstFocusable(getWindow().getDecorView());
                    if (target != null) {
                        target.requestFocus();
                        return true;
                    }
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                    // OK 键,触发点击
                    focused.performClick();
                    return true;
                } else {
                    // 方向键,用 focusSearch 找邻居
                    View target = focused.focusSearch(directionOf(keyCode));
                    if (target != null && target != focused) {
                        target.requestFocus();
                        return true;
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event);
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

    /**
     * 找第一个可 focus 的 view
     */
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

    /**
     * 递归设所有 view 为 focusable + focusableInTouchMode
     */
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

    private boolean isRunningOnTV() {
        PackageManager pm = getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
                || pm.hasSystemFeature("android.software.leanback");
    }
}
