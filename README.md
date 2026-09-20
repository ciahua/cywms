# cywms · 江苏辰阳电子 WMS

PDA 端按 **原生 Android（Kotlin + Jetpack Compose）** 重建，对接现有 JeecgBoot 后端；**不用 Flutter**。

> **维护接手请先读：** [`docs/交接文档.md`](docs/交接文档.md) / [`docs/交接文档.docx`](docs/交接文档.docx)（位置、用法、发版、完成度、风险）

## 工程

| 路径 | 说明 |
|------|------|
| [`android-pda/`](android-pda/) | PDA 客户端（用 Android Studio 打开此目录） |
| [`docs/交接文档.md`](docs/交接文档.md) / [`docs/交接文档.docx`](docs/交接文档.docx) | **项目交接 / 运维手册**（Markdown + Word） |
| [`docs/pda-rebuild/`](docs/pda-rebuild/) | 重建方案 / 模块矩阵 / 接口契约 |
| [`legacy/`](legacy/) | 旧 APK 分析说明书（权威依据） |

## 分支与发布

| 项 | 说明 |
|----|------|
| 开发分支 | `cursor/pda-rebuild-plan-695c`（业务代码在此；`main` 尚未合并） |
| Draft PR | https://github.com/ciahua/cywms/pull/1 |
| APK | [GitHub Releases](https://github.com/ciahua/cywms/releases) · Tag `pda-v*` · 附件 `cywms-pda-debug.apk` |
| 当前版本 | **v1.1.1** |

## 当前能力（摘要）

- 登录：`POST /jeecg-boot/sys/mLogin`，可配 **IP:端口** 与 **ngrok 代理**
- 登录页：仓库实景背景 + 深色遮罩 + 毛玻璃表单；GitHub 自动检查更新
- 主界面：14 个功能模块分组入口
- **生产领料**三屏已正式实现；其余模块多为入口骨架
- 扫码广播：`com.service.scanner.data` / `ScanCode`

## 运行

```bash
git checkout cursor/pda-rebuild-plan-695c
# Android Studio 打开 android-pda/，Sync 后 Run
```

现场填内网地址；远程测试在登录页填 ngrok 代理 URL。细节见交接文档。
