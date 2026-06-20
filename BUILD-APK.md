# LX Music 电视版 · 编译 + 装机傻瓜指南

> 你不会编译 Android?没关系,这份指南就是给"完全不会"的人写的。
> 整个流程你只需要点鼠标,**不敲一行命令**。

---

## 总流程

```
下载 zip  →  解压  →  上传到 GitHub  →  触发云端编译  →  下载 APK  →  装到长虹电视
(2 分钟)    (1 分钟)  (3 分钟)           (10-20 分钟)     (1 分钟)    (5 分钟)
```

预计 30 分钟拿到能装的 APK。

---

## 第一步:准备文件(你已经做完)

桌面上应该有两个文件:

| 文件 | 大小 | 用途 |
|---|---|---|
| `lx-music-tv.zip` | ~4 MB | 完整仓库(含 TV 适配 + 云编译 workflow) |
| `lx-music-tv.patch` | ~14 KB | 备用补丁(只在 zip 出问题时用) |

如果找不到,**用 Everything 搜 "lx-music-tv"** 找一下。

---

## 第二步:注册 GitHub(如果你还没账号)

打开 https://github.com/signup ,按提示注册一个免费账号。
**如果你已经有 GitHub 账号,跳过这步。**

---

## 第三步:Fork 上游仓库

打开 https://github.com/lyswhut/lx-music-mobile

页面右上角有个 **`Fork`** 按钮,点一下。

等几秒,会跳转到 `https://github.com/<你的用户名>/lx-music-mobile`
——这就是你的 fork 仓库。**所有改动都推到这里**,不会影响原作者。

---

## 第四步:把代码上传到你的 Fork

### 方案 A:用 GitHub Desktop(最简单,推荐)

1. 下载安装 GitHub Desktop:https://desktop.github.com/
2. 打开 GitHub Desktop → `File` → `Clone repository`
3. 选你的 fork `你的用户名/lx-music-mobile`,本地路径选个空文件夹
4. 等待克隆完成(可能几十秒,下载的是不含 TV 适配的原版)
5. 用 Windows 资源管理器打开那个文件夹
6. 删掉里面的所有文件(只剩 `.git` 隐藏文件夹)
7. 把 `lx-music-tv.zip` 解压出来的所有内容**拖进去**
8. 回到 GitHub Desktop → 它会显示一堆 changes
9. 左下角填 Commit message(随便填比如 "TV adaptation"),点 `Commit to main`
10. 点上面的 `Push origin`

### 方案 B:直接网页上传 zip(更简单但限制多)

不推荐 —— GitHub 网页不支持上传整个项目 zip 解压后的文件。
用方案 A。

### 方案 C:命令行(如果你以后想学)

```bash
git clone https://github.com/你的用户名/lx-music-mobile.git
cd lx-music-mobile
# 解压 lx-music-tv.zip 覆盖
git add -A
git commit -m "TV adaptation"
git push
```

---

## 第五步:触发云端编译

回到浏览器,打开:
```
https://github.com/你的用户名/lx-music-mobile/actions
```

页面会显示一个 workflow 列表,其中一个叫 **`Build TV Debug APK`**(就是我加的)。
点它 → 右边有 **`Run workflow`** 按钮 → 点 → 选 `master` 分支 → 绿色 `Run workflow` 按钮

**等 10-20 分钟**。可以刷新页面看进度。

---

## 第六步:下载编译好的 APK

编译完成后,Actions 页面会显示一个绿色的 ✓。
点进这个 run → 底部有 **`Artifacts`** 区域 → 点 **`lx-music-tv-debug`** 下载 zip。

解压这个 zip,里面有一个 APK 文件,比如:
```
app-arm64-v8a-debug.apk     (ARM64,大部分现代电视)
app-armeabi-v7a-debug.apk   (ARMv7,老电视)
app-x86_64-debug.apk        (x86,部分电视盒子)
app-universal-debug.apk     (通用版,文件大但保证能装)
```

**不知道选哪个?** 先试 `app-arm64-v8a-debug.apk`(2020 年后的电视基本都是 ARM64)。
装不上再试 `app-universal-debug.apk`。

---

## 第七步:装到长虹电视

1. 把 APK 拷到 U 盘根目录
2. U 盘插长虹电视 USB 口
3. 长虹设置 → 安全 → **允许安装未知来源应用**
4. 长虹自带文件管理器打开 U 盘 → 双击 APK → 安装
5. 桌面 / 应用列表里找 LX Music,启动

---

## 第八步:测试遥控器

LX Music 启动后:

| 遥控器按键 | 应该发生什么 |
|---|---|
| 方向键上/下/左/右 | 焦点在按钮之间移动(被选中的按钮会有绿色高亮+缩放) |
| 中心 OK 键 | 点击当前焦点 |
| 返回键 | 关闭弹窗 / 返回上一页 |
| 播放/暂停键(部分遥控器) | 切换歌曲播放 |

如果方向键完全没反应 → 长虹固件拦截了。**先重启电视再试**,不行就告诉我长虹型号,我看看有没有 workaround。

---

## 常见问题

### Q: Actions 编译失败?
点进失败的 run,看红色 ❌ 行的日志,**复制粘贴整段红色文字**给我。

### Q: APK 装不上电视?
- 检查电视 Android 版本 ≥ 5.0(LX Mobile 要求)
- 看电视 "未知来源" 是不是开了
- 试 `universal` 版本的 APK

### Q: 启动就闪退?
- 看 logcat:`adb logcat | grep LXMusic`(需要 USB 调试)
- 或者长虹应用管理器里看 LX Music 崩溃信息

### Q: 焦点能用,但 UI 还是手机竖屏布局?
正常 —— 我没重写 UI,沿用 LX Mobile 的 Horizontal 布局。
长虹 16:9 电视上看起来会左右黑边,但能用。
**重写 UI 是 B 档工作(5-7 天),需要时再说。**

### Q: 我没搞定,想让你来弄?
直接告诉我卡在哪步,把报错 / 截图给我。

---

## 工作量证明(给好奇的人看)

实际代码改动 +152/-7 行,跨 6 个文件:
- `AndroidManifest.xml`: 加 LEANBACK + uses-feature
- `MainActivity.java`: TV 检测锁横屏
- `src/utils/device.ts`: isTV 工具
- `src/components/common/Button.tsx`: focus 高亮
- `src/screens/Home/index.tsx`: TV 强制横版
- `doc/tv-install-guide.md`: 长虹装机文档

外加 `.github/workflows/build-tv-debug.yml`:云端编译 workflow。