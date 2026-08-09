# cywms · 江苏辰阳电子 WMS

PDA 端按 **原生 Android（Kotlin）** 重建，对接现有 JeecgBoot 后端；**不用 Flutter**。

## 文档

| 文档 | 说明 |
|------|------|
| [重建开发说明书（权威分析）](legacy/辰阳电子WMS_PDA_Android重建开发说明书.md) | 旧 APK + 接口/模块还原 |
| [重建开发方案](docs/pda-rebuild/重建开发方案.md) | 技术选型、UI、代理、分期 |
| [模块功能矩阵](docs/pda-rebuild/模块功能矩阵.md) | 14 宫格 ↔ 新 Screen |
| [接口契约清单](docs/pda-rebuild/接口契约清单.md) | `/jeecg-boot/...` 兼容清单 |

## 下一步

创建 `android-pda` 工程：可配置 BaseUrl/ngrok 代理的登录页 + 分组首页 + 扫码广播骨架，再打通生产/委外领料。
