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

import com.facebook.react.ReactRootView;
import com.reactnativenavigation.NavigationActivity;

public class MainActivity extends NavigationActivity {

    private TextView debugOverlay;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ReactRootView lastReactRootView;

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
            // RN view 创建是异步的,用 OnGlobalLayout 持续探测 ReactRootView
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

    /**
     * 找出 RNN 内部的 ReactRootView,确保它的所有子 view 都可 focus。
     * RNN 用 LinearLayout 当根容器,但真正的 RN view 树在 ReactRootView 内。
     * focusSearch 从 LinearLayout 出发不会进入 ReactRootView,
     * 所以我们要直接定位 ReactRootView 并让它接焦点。
     */
    private void findAndPrepareReactRootView() {
        if (lastReactRootView != null) return; // 已经找到了,不再重复
        View found = findReactRootView(getWindow().getDecorView());
        if (found != null) {
            lastReactRootView = (ReactRootView) found;
            // 1. 自己可 focus + touch focus
            lastReactRootView.setFocusableInTouchMode(true);
            lastReactRootView.setFocusable(true);
            // 2. 强制拿焦点(但要在 dispatchKeyEvent 里才能真正生效,不主动触发)
            // lastReactRootView.requestFocus();
            // 3. 递归让所有子 view focusable
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
                // 如果焦点卡在根 LinearLayout,先强制移到 ReactRootView
                if (focused != null && lastReactRootView != null) {
                    String focusClass = focused.getClass().getSimpleName();
                    if (focusClass.equals("LinearLayout") || focused == getWindow().getDecorView()) {
                        lastReactRootView.requestFocus();
                        focused = lastReactRootView;
                    }
                }
                if (focused == null && lastReactRootView != null) {
                    lastReactRootView.requestFocus();
                    focused = lastReactRootView;
                }
                if (focused != null) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                        focused.performClick();
                        handled = true;
                    } else {
                        int dir = directionOf(keyCode);
                        // 先在 focus view 内找,再扩到 ReactRootView 全树
                        View target = focused.focusSearch(dir);
                        if (target == null || target == focused) {
                            target = searchFocusableFromRoot(lastReactRootView, dir);
                        }
                        if (target != null && target != focused) {
                            target.requestFocus();
                            handled = true;
                        }
                    }
                }
            }
            updateDebugOverlay();
        }
        return super.dispatchKeyEvent(event) || handled;
    }

    /**
     * 从根节点向下,递归找方向 dir 上"最近"的可 focus view。
     * fallback:Android focusSearch 找不到时,自己算几何最近邻。
     */
    private View searchFocusableFromRoot(View root, int direction) {
        if (root == null || !root.isFocusable() || !root.isFocusableInTouchMode()) {
            // 递归找一个 focusable 的 leaf 当起点
            return findFirstFocusable(root);
        }
        // 直接用 focusSearch
        return root.focusSearch(direction);
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
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,  // 不抢焦点
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
        String rrvInfo = lastReactRootView == null ? "(not found)" : "ok";
        if (focused == null) {
            debugOverlay.setText("FOCUS: (none) RRV=" + rrvInfo);
        } else {
            debugOverlay.setText(String.format(
                "FOCUS: id=%s class=%s focusable=%s touch=%s RRV=%s",
                focused.getId() == View.NO_ID ? "(no-id)" : String.valueOf(focused.getId()),
                focused.getClass().getSimpleName(),
                focused.isFocusable(),
                focused.isFocusableInTouchMode(),
                rrvInfo
            ));
        }
    }

    private boolean isRunningOnTV() {
        PackageManager pm = getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
                || pm.hasSystemFeature("android.software.leanback");
    }
}
