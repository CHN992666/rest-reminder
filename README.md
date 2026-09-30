# 休息提醒 (Rest Reminder)

一个简单的安卓休息提醒工具，遵循 20-20-20 护眼规则的扩展版：每工作 20 分钟提醒休息 20 秒，循环若干轮后进入长休息。所有时长与循环次数均可自定义。

## 功能

- 工作 N 分钟 → 提醒"该休息了"
- 休息 N 秒 → 提醒"开始工作"
- 循环 X 轮后 → 进入长休息（默认 5 分钟）
- 长休息结束后自动回到工作状态，重新计数
- 全部时长与轮数可在 App 内自定义
- 锁屏后计时继续（前台服务 + WakeLock）
- 进程被杀后可自动恢复计时状态
- 开机自启（如之前在计时）

默认值：工作 20 分钟 / 短休息 20 秒 / 长休息 5 分钟 / 循环 4 轮。

## 安装（无需编译环境）

1. 前往本仓库的 **Releases** 页面
2. 下载最新的 `app-release.apk`（或 `app-debug.apk`）
3. 在安卓手机上点击下载的 APK，按系统提示允许"安装未知来源应用"
4. 安装完成后打开"休息提醒"即可使用

> 首次使用时，系统会请求**通知权限**，请允许，否则提醒不会出现。

## 从源码构建（可选）

需要 JDK 17 和 Gradle 8.7+。本仓库未自带 gradle-wrapper.jar，需本地已安装 Gradle：

```bash
gradle assembleRelease
```

产物位于 `app/build/outputs/apk/release/`。

## 配置签名（仅 CI 维护者需要，一次性操作）

Releases 中的 APK 由 GitHub Actions 自动构建并签名。首次配置签名：

1. 在仓库 **Actions** 页面手动运行 `Generate Release Keystore` workflow
2. 运行完成后，在该次运行的 Summary 页面复制 base64 字符串
3. 在仓库 **Settings → Secrets and variables → Actions → New repository secret** 添加 4 个 secret：
   - `KEYSTORE_BASE64` = 上面的 base64 字符串
   - `KEYSTORE_PASSWORD` = `changeit`
   - `KEY_ALIAS` = `rest-release`
   - `KEY_ALIAS_PASSWORD` = `changeit`
4. 之后每次推送 `v*` 标签，CI 会自动构建签名 APK 并发布到 Releases

> 持久化 keystore 的目的是保证每次发布的 APK 签名一致，用户可"覆盖安装"升级而不丢设置。

## 故障排查

- **锁屏后计时停止**：部分国产 ROM（小米/华为/OPPO 等）会激进杀后台。请到系统设置 → 电池 → 把本应用加入"不受限"或"白名单"。
- **没收到通知**：确认系统设置中本应用的通知权限已开启。
- **开机后没自启**：确认本应用有"开机自启"权限（部分系统需手动授予）。

## 技术栈

- Kotlin + AndroidX
- Foreground Service (specialUse) + CountDownTimer
- SharedPreferences 持久化状态
- Material Components 主题
- View Binding
- Min SDK 21 (Android 5.0)，Target SDK 34 (Android 14)
