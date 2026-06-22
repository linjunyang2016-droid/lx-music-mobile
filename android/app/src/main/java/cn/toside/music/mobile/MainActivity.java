package cn.toside.music.mobile;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.KeyEvent;

import com.reactnativenavigation.NavigationActivity;
import com.facebook.react.ReactActivityDelegate;
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint;
import com.facebook.react.defaults.DefaultReactActivityDelegate;

public class MainActivity extends NavigationActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TV 适配:检测到 TV 设备(Android TV / 长虹等国行魔改电视)
        // 就锁定横屏,因为 LX Mobile 的 Horizontal 布局是为大屏横版设计的。
        // 手机端不受影响——只有 isTV 时才锁。
        if (isRunningOnTV()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // TV 适配:让所有 RN View 在有触摸屏的设备上也能接 D-pad 焦点。
        // 这是国产电视(长虹/小米魔改)的核心问题——
        // Android 默认 focusable 模式不支持 D-pad,
        // 必须在 onResume 后把 root view 树所有节点设成 focusableInTouchMode=true。
        if (isRunningOnTV()) {
            enableDpadFocusOnAllViews(getWindow().getDecorView());
        }
    }

    /**
     * 递归把 view 树所有节点设成 focusableInTouchMode,
     * 让 D-pad key events 能 focus 到任意 View。
     * 只对 RN 容器内的 View 生效(不影响系统 UI)。
     */
    private void enableDpadFocusOnAllViews(View view) {
        if (view == null) return;
        view.setFocusableInTouchMode(true);
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                enableDpadFocusOnAllViews(vg.getChildAt(i));
            }
        }
    }

    /**
     * 通过 PackageManager 检测 TV 特性,匹配 manifest uses-feature 里
     * android.software.leanback / FEATURE_TELEVISION 任一即视为 TV。
     */
    private boolean isRunningOnTV() {
        PackageManager pm = getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
                || pm.hasSystemFeature("android.software.leanback");
    }
}
