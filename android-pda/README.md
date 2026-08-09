# cywms-pda · 辰阳电子 WMS PDA（原生 Android）

Kotlin + Jetpack Compose 骨架：登录（可配服务器 / ngrok 代理）+ 分组主界面（14 模块入口）。

## 打开工程

用 **Android Studio Hedgehog+** 打开本目录 `android-pda/`，等待 Gradle Sync 后运行到 PDA 或模拟器。

## 登录联调

1. 展开「服务器与代理设置」
2. **远程 / ngrok（当前默认）**：代理已预填 `https://3c17-222-186-202-65.ngrok-free.app`
3. **现场直连**：清空代理，填内网 IP（默认 `172.16.5.7`）与端口 `8080`
4. 可先点「测试连接」，再使用 Jeecg 账号登录（如 `admin` / `e`）
5. 接口：`POST {baseUrl}/jeecg-boot/sys/mLogin`

> ngrok 免费域名变更时，只需在登录页改代理地址并保存。

## 已实现

- `mLogin` 登录与 Token 持久化
- 动态 BaseUrl + ngrok Header
- 毛玻璃登录框 + 仓库背景
- **登录页自动检查更新**：从 **GitHub Releases** 拉取最新 APK，有新版本自动下载并可安装
- 主界面 6 组 / 14 模块图标入口（业务页占位）
- 扫码广播注册：`com.service.scanner.data` / `ScanCode`

## 版本发布（GitHub）

1. 提高 `app/build.gradle.kts` 中 `versionName`（建议 `v1.0.0_YYYYMMDD.N`）与 `versionCode`
2. `./gradlew :app:assembleDebug`
3. 创建 Release，**Tag** 使用 `pda-{versionName}`，例如 `pda-v1.0.0_20260809.2`
4. 上传附件，文件名固定为 **`cywms-pda-debug.apk`**

客户端会请求 `https://api.github.com/repos/ciahua/cywms/releases`，取带 `.apk` 的最新非 draft Release，按 tag 中的版本号比较。

## 包名

`com.chenyang.cywms`
