package cn.toside.music.mobile;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

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
