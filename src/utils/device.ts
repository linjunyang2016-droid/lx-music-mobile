import { Platform } from 'react-native'

/**
 * 是否运行在 TV 设备(Android TV / 国产电视魔改系统)
 *
 * RN 自带 Platform.isTV,在 Android 端通过 PackageManager 的
 * FEATURE_TELEVISION / FEATURE_LEANBACK 检测。Manifest 加了
 * <uses-feature android:name="android.software.leanback" required="false"/>
 * 之后,这个值在长虹/小米等电视上会返回 true。
 */
export const isTV = Platform.isTV === true

/**
 * TV 上是否锁定横屏。
 * 长虹国行电视一般 16:9 1920x1080,锁横屏体验更好。
 * 手机不受影响——只在 isTV 时锁。
 */
export const shouldLockLandscape = isTV
