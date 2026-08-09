# cywms-pda · 辰阳电子 WMS PDA（原生 Android）

Kotlin + Jetpack Compose 骨架：登录（可配服务器 / ngrok 代理）+ 分组主界面（14 模块入口）。

## 打开工程

用 **Android Studio Hedgehog+** 打开本目录 `android-pda/`，等待 Gradle Sync 后运行到 PDA 或模拟器。

## 登录联调

1. 展开「服务器与代理设置」
2. **现场**：填内网 IP（默认 `172.16.5.7`）与端口 `8080`，代理留空  
3. **远程 / ngrok**：在「代理地址」填 `https://xxxx.ngrok-free.app`（优先于 IP）
4. 可先点「测试连接」，再使用 Jeecg 账号登录（如 `admin`）
5. 接口：`POST {baseUrl}/jeecg-boot/sys/mLogin`

## 已实现

- `mLogin` 登录与 Token 持久化
- 动态 BaseUrl + ngrok Header
- 毛玻璃登录框 + 仓库背景
- 主界面 6 组 / 14 模块图标入口（业务页占位）
- 扫码广播注册：`com.service.scanner.data` / `ScanCode`

## 包名

`com.chenyang.cywms`
