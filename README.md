# cywms · 江苏辰阳电子 WMS

PDA 端按 **原生 Android（Kotlin + Jetpack Compose）** 重建，对接现有 JeecgBoot 后端；**不用 Flutter**。

## 工程

| 路径 | 说明 |
|------|------|
| [`android-pda/`](android-pda/) | PDA 客户端（可 Android Studio 打开） |
| [`legacy/`](legacy/) | 旧 APK 分析说明书 |
| [`docs/pda-rebuild/`](docs/pda-rebuild/) | 重建方案 / 模块矩阵 / 接口契约 |

## 当前骨架能力

- 登录：`POST /jeecg-boot/sys/mLogin`，可配 **IP:端口** 与 **ngrok 代理**
- 登录页：仓库实景背景 + 深色遮罩 + 毛玻璃表单
- 主界面：14 个功能模块分组图标（业务页后续迭代）
- 扫码广播骨架：`com.service.scanner.data` / `ScanCode`

## 运行

用 Android Studio 打开 `android-pda/`，Sync 后 Run。现场填内网地址；远程测试在登录页填 ngrok 代理 URL。
