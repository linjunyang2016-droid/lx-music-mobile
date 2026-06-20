# 长虹电视(Android TV)装机指南

把 LX Music Mobile(已加 TV 适配)装到长虹电视上,用遥控器基本操作。

## 1. 编译 APK

在能跑 Android Studio / gradle 的开发机上:

```bash
cd lx-music-mobile
npm install
cd android
./gradlew assembleRelease    # 或 assembleDebug 出 debug 包
```

产物在 `android/app/build/outputs/apk/release/app-release.apk`。
首次 release 构建需要 keystore,按 README 文档生成。

## 2. 装到长虹电视

长虹国行电视一般是基于 Android 6/7 魔改,支持 APK 安装但默认不允许未知来源。

步骤:

1. **设置 → 账号与安全 → 安装未知应用 → 允许 U 盘安装** (不同固件措辞可能略不同)
2. 把 `app-release.apk` 拷到 U 盘根目录
3. U 盘插电视 USB 口
4. 用长虹自带文件管理器打开 U 盘,双击 APK → 安装
5. 装好后 LX Music 会出现在桌面 + Android TV launcher 里(因为加了 `LEANBACK_LAUNCHER` category)

如果长虹桌面不显示,可去「应用管理 → 全部应用」找 LX Music。

## 3. 遥控器按键映射

LX Music 没特殊适配,所有按键走 Android 系统标准 TV 行为:

| 遥控器按键 | 行为 |
|---|---|
| 上下左右方向键 | 焦点移动(高亮当前按钮) |
| 中心 OK 键 | 点击当前焦点 |
| 返回键 | 关闭弹窗 / 返回上一页 |
| 播放/暂停键(部分长虹遥控器有) | 切换播放状态(走 react-native-track-player MediaSession) |
| 菜单键(部分长虹遥控器有) | 打开当前页操作菜单 |

## 4. 自定义音源(可选)

LX Mobile 默认带在线搜索。要导入自定义音源(js 文件):

1. 把音源文件(如 `my-source.js`)复制到 U 盘
2. 在长虹上用 LX Music:设置 → 自定义源 → 从文件导入
3. 长虹系统文件选择器能浏览 U 盘

## 5. 已知限制

- **UI 还是手机横屏布局**。LX Mobile 已经有 `Horizontal` 布局,TV 上强制使用,但没有针对 1920x1080 重新设计字号 / 间距。在 4K 屏上可能略小但功能完整。
- **不支持 TV launcher 上的精品推荐行**(那是 androidx.leanback `BrowseFragment` 的事,本项目没引入该库)。
- **不支持语音搜索**。
- **不支持开机自启动 TV 模式**(手动从应用列表启动即可)。
- **底部播放条**底部有一行播放控制条,焦点能落到上面,中心键可切换播放。

## 6. 如果焦点移动有问题

部分国产电视固件会拦截方向键做自己的 UI 导航。应急办法:

- 用 USB 键盘接电视 USB 口测试
- 用手机装 [Cetus Play](https://www.cetusplay.com/) 等 TV 遥控 app,把手机当触摸板 / D-pad

如果方向键完全没反应,反馈时附上:
- 长虹电视型号
- 固件版本(设置 → 关于 → 系统信息)
- 遥控器型号(电池仓里有)
