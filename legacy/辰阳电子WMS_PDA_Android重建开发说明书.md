# 辰阳电子 WMS · PDA Android 重建开发说明书

> 文档用途：在另一台电脑上重建 **原生 Android PDA 客户端**（不用 Flutter）。  
> 分析日期：2026-08-09  
> 分析对象：旧客户端 `app-debug.apk`（Flutter debug 包）+ JeecgBoot 后端 `3.5.5`  
> 原则：对接现有后端，**不改现网接口契约**；开发时勿随意改生产库/运行中服务。

---

## 1. 背景与结论

### 1.1 为什么重建

- 旧 APK 源码丢失（原工程路径痕迹：`D:/0_Projects/ChenYang/cywms_pda`，包名 `cywms` / `com.example.cywms`）。
- 旧包为 **Flutter**；新版改为 **原生 Android（Kotlin 推荐）**，便于直接调用 PDA 扫码广播/SDK，企业功能页也不需要 Flutter UI。

### 1.2 能还原到什么程度

| 项目 | 结论 |
|------|------|
| 完整可编译 Dart 源码 | **不能** 100% 还原 |
| 页面清单 / 接口 URL / 中文文案 / 扫码广播参数 | **可还原**（debug APK 含 `kernel_blob.bin` + `MainActivity` 字节码） |
| 推荐做法 | **原生 Android 重写**，以后端 Controller + 本文接口表为准 |

### 1.3 系统拓扑

```
[PDA 原生 App]  --HTTP-->  [JeecgBoot :8080]  --MySQL-->  cywms
                              ^
[管理 Web Vue3 :3100] --------+
```

| 端 | 地址 / 路径 | 说明 |
|----|-------------|------|
| 后端 API | `http://172.16.5.7:8080` | 旧 App 默认；备选 `http://10.10.10.120:8080` |
| API 前缀 | `/jeecg-boot/...` | 完整 URL = `{baseUrl}/jeecg-boot/{module}/...` |
| 管理后台 | `http://172.16.5.7:3100/` | Vue3；用户名 `admin`，密码 `e`（仅联调） |
| 数据库 | MySQL80 / 库名 `cywms` | 只读对照字段；写操作先用测试环境 |
| 后端业务模块 | `jeecg-module-demo` | 无独立 WMS 模块 |
| Web 页面 | `jeecgboot-vue3/src/views/*` | 建单/查询；盘点主要在 PDA |

---

## 2. 技术选型（新 App）

| 项 | 建议 |
|----|------|
| 语言 | Kotlin + AndroidX + ViewBinding（或 XML Activity） |
| 最低 SDK | 按现场 PDA（常见 Android 7–10） |
| 网络 | OkHttp / Retrofit，JSON |
| 本地存储 | SharedPreferences：`apiUrl`、`username`、`token` |
| UI | 大按钮网格首页 + 列表/扫码页；少动画 |
| 扫码 | **广播 Intent**（见第 3 章）；厂商 SDK 可选增强 |
| 包名建议 | `com.chenyang.cywms` 或继续 `com.example.cywms`（与旧包兼容非必须） |

建议工程目录：

```
cywms-android/
  app/src/main/java/.../
    api/            # Retrofit 接口
    model/          # DTO
    scanner/        # ScannerHelper（广播）
    ui/login|home|stockin|lingliao|tuiliao|stocktake|...
    util/           # Prefs, Toast, 条码解析
  app/src/main/res/layout/
```

---

## 3. 扫码广播（旧 APK 已确认）

旧 App 在 `com.example.cywms.MainActivity` 中注册广播（字节码反汇编确认）：

| 项 | 值 |
|----|-----|
| **Intent Action** | `com.service.scanner.data` |
| **条码 Extra Key** | `ScanCode` |
| 字段常量名 | `ACTION_PDA_SERVICE_SCANNER_DATA` / `SCAN_RESULT_CODE` |

### 3.1 原生接收示例

```kotlin
class ScannerHelper(private val activity: Activity, private val onBarcode: (String) -> Unit) {
    private val action = "com.service.scanner.data"
    private val extraKey = "ScanCode"

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val code = intent?.getStringExtra(extraKey)?.trim().orEmpty()
            if (code.isNotEmpty()) onBarcode(code)
        }
    }

    fun register() {
        activity.registerReceiver(receiver, IntentFilter(action))
        // Android 13+ 视系统要求增加 RECEIVER_EXPORTED / NOT_EXPORTED 标志
    }

    fun unregister() {
        runCatching { activity.unregisterReceiver(receiver) }
    }
}
```

### 3.2 旧 Flutter 桥接（仅了解，新 App 不需要）

- MethodChannel：`com.example.flutter_broadcast_receiver/channel`
- 方法名：`broadcastMessage`（参数为条码字符串）
- 另有：`com.example.cywms/native`（打开原生页等，非扫码主路径）

### 3.3 条码业务约定（从旧逻辑推断）

- 条码常含 `*` 分段，如 `物料代码*数量*...`；初始入库按 `*` 拆分取物料代码。
- 页面常配置「条码长度」校验（长度 +5 冗余）。
- 需防重复扫码；领料要校验余量 / FIFO / 工单数量。
- 扫码后可震动（旧包用 `flutter_vibrate`）。

---

## 4. 登录与通用协议

### 4.1 登录

- **URL**：`POST {baseUrl}/jeecg-boot/sys/mLogin`
- **Body（JSON）**示例：

```json
{
  "username": "admin",
  "password": "e"
}
```

- **响应**：Jeecg 标准 `Result`：

```json
{
  "success": true,
  "message": "",
  "code": 200,
  "result": {
    "token": "...",
    "userInfo": { "username": "admin", "realname": "..." }
  }
}
```

- 登录页应允许修改服务器地址（旧 App：`Global.API_URL`）。
- 多数 PDA 业务接口在 Shiro 中配置为 **`anon`（见第 7 章）**，即使不带 Token 也可能调用成功；仍建议登录后带上 `X-Access-Token`，便于审计与未来收紧权限。

### 4.2 HTTP 建议头

```
Content-Type: application/json
X-Access-Token: {token}   // 若有
```

### 4.3 通用响应判断

- `success == true` 为业务成功；失败读 `message`。
- 列表数据多在 `result.records` 或直接 `result`（按接口而定，以后端为准）。

---

## 5. 功能模块与页面对照

### 5.1 旧 App 主界面模块（首页宫格）

应用标题：**江苏辰阳电子WMS系统**

从旧 APK `home.dart` 附近文案顺序还原，主界面功能入口共 **14 个模块**（不含标题；另有「检查版本」类辅助按钮）：

| 序号 | 模块名称 | 建议路由 / 页面 | 主要后端 |
|------|----------|-----------------|----------|
| 1 | 成品入库 | `product_stockin_scan` | `warehouselog` / `warehouselog2` / `prodrecv` |
| 2 | 成品出库 | `/pages/delivery_order` | `deliverylist/delivery` |
| 3 | 成品盘点 | `/pages/producttaking` | `warehouselog` |
| 4 | 委外领料 | `/pages/mat_delivery_order2` | `outsource` + `outsource_barcode` |
| 5 | 仓库盘点 | `/pages/stocktaking` | `warehouselog/stcoktaking` |
| 6 | 快捷入库 | `/pages/initial_inventory` | `warehouselog/addpda` |
| 7 | 生产领料 | `/pages/mat_delivery_order1` | `shengchanlingliao` + `produce_material_barcode` |
| 8 | 生产退料 | （退料扫码页） | `shengchan_tuiliao` |
| 9 | 移库操作 | `/pages/stockmoving` | `warehouselog/stockmoving` |
| 10 | 物料超领 | `/pages/mat_delivery_order3` | `chaolingdan` + `chaoling_barcode` |
| 11 | 更换标签 | `/pages/change_barcode` | `warehouselog/changebarcode` |
| 12 | 退料出库 | `/pages/tuiliaochuku` | `tuiliaochuku` + `tuiliaochuku_barcode` |
| 13 | 物料重检 | `/pages/mat_doublecheck` | `warehouselog/doublecheck`、`queryalarm*` |
| 14 | 呆料管理入库 | `/pages/initial_inventory1` | `warehouselog/addpda`、`warehouselog2/addpda` |

说明：

- 新 App 首页建议按上表复刻 **14 宫格**（顺序可与旧版一致，便于现场用户习惯）。
- 全应用命名路由中还出现：`receipt_order`（收货）、`stockin_order1`（物料入库单）、`mat_delivery_order`（物料出库）、`outsource_tuiliao`（委外退料单）等，可能从首页外入口或次级页进入；重建时可按优先级放到首页或二级菜单。
- 辅助：`检查版本`（对接 `filedownload/list1`）。

### 5.2 页面与后端对照（完整）

旧 Flutter 页面文件（39 个，包名 `package:cywms`）→ 建议原生 Activity/Fragment：

| 旧页面 / Title | 业务含义 | 主要后端模块 |
|----------------|----------|--------------|
| login / home | 登录、功能菜单 | `sys/mLogin` |
| receipt_order* | 收货单 | `shouhuodan` |
| stockin_order* / mat_recv* | 物料入库扫码 | `entry_notify` / `matarrival_notify` / `stockinorder*` |
| mat_delivery_order1* | **生产领料** | `shengchanlingliao` + `produce_material_barcode` |
| mat_delivery_order2* | **委外领料** | `outsource` + `outsource_barcode` |
| mat_delivery_order3* | **物料超领** | `chaolingdan` + `chaoling_barcode` |
| （生产退料扫码页） | 生产退料 | `shengchan_tuiliao` |
| outsource_tuiliao* | 委外退料 | `outsoucetuiliao` + `outsource_tuiliao_barcode` |
| tuiliaochuku* | 退料出库 | `tuiliaochuku` + `tuiliaochuku_barcode` |
| delivery_order* | 成品出库/发货 | `deliverylist/delivery` |
| product_stockin_scan | 成品入库扫码 | `warehouselog` / `warehouselog2` / `prodrecv` |
| product_stockout_scan | 成品出库扫码 | `deliverylist` / `warehouselog` |
| stocktaking | **仓库盘点** | `warehouselog/stcoktaking`（注意拼写） |
| producttaking | 成品盘点 | 同上/成品相关 |
| stockmoving | **移库** | `warehouselog/stockmoving` |
| initial_inventory* | 快捷/初始/呆料入库 | `warehouselog/addpda`、`warehouselog2/addpda` |
| change_barcode | 更换标签 | `warehouselog/changebarcode` |
| mat_doublecheck | 物料重检 | `warehouselog/doublecheck`、`queryalarm*` |

### 5.3 典型扫码流程

```
选单据列表(list1/2/3)
  → 进明细(query*Detail*)
  → 扫码(verify / getrestqty / checkinfifo)
  → 本地累计条码列表
  → 提交(addpda / addlist / addBarcodeList / add1..add5)
  → 改状态(changestatus / completeorder / finishorder / changemainstatus)
```

---

## 6. 旧 App 实际调用的接口清单

> 完整 URL = `{baseUrl}` + 下表路径。  
> 方法以后端注解为准；下列按旧 App 字符串提取（GET 查询串 / POST JSON 为主）。

### 6.1 系统

| 方法(参考) | 路径 |
|-----------|------|
| POST | `/jeecg-boot/sys/mLogin` |
| GET | `/jeecg-boot/sys/common/static/{fileurl}` |
| GET | `/jeecg-boot/filedownload/filedownload/list1` |

### 6.2 库存台账 warehouselog

| 方法(参考) | 路径 | 用途 |
|-----------|------|------|
| POST | `/jeecg-boot/warehouselog/warehouselog/addpda` | 初始/快捷入库 |
| POST/PUT | `/jeecg-boot/warehouselog/warehouselog/stcoktaking` | 盘点（**拼写 stcok** 必须原样） |
| POST/PUT | `/jeecg-boot/warehouselog/warehouselog/stockmoving` | 移库 |
| POST/PUT | `/jeecg-boot/warehouselog/warehouselog/changeqty` | 改数量 |
| GET | `/jeecg-boot/warehouselog/warehouselog/changebarcode` | 换条码 |
| GET | `/jeecg-boot/warehouselog/warehouselog/checkbarcode` | 检查条码 |
| GET | `/jeecg-boot/warehouselog/warehouselog/checkinfifo` | FIFO 校验 |
| GET | `/jeecg-boot/warehouselog/warehouselog/checkinfifo1` | FIFO 校验（带需求量） |
| GET | `/jeecg-boot/warehouselog/warehouselog/doublecheck` | 重检提交相关 |
| GET | `/jeecg-boot/warehouselog/warehouselog/getsku` | 按条码取 SKU 候选 |
| GET | `/jeecg-boot/warehouselog/warehouselog/instantledger1` | 即时台账 |
| GET | `/jeecg-boot/warehouselog/warehouselog/queryalarm` | 效期预警单条 |
| GET | `/jeecg-boot/warehouselog/warehouselog/queryalarmlist` | 预警列表 |
| GET | `/jeecg-boot/warehouselog/warehouselog/querybarcode` | 查条码 |
| GET | `/jeecg-boot/warehouselog/warehouselog/querybarcode1` | 查条码 |
| GET | `/jeecg-boot/warehouselog/warehouselog/querybarcoderestqty` | 条码余量 |
| GET | `/jeecg-boot/warehouselog/warehouselog/queryfifo1` | FIFO 查询 |
| GET | `/jeecg-boot/warehouselog/warehouselog/querymaterialrestqty` | 物料余量 |
| GET | `/jeecg-boot/warehouselog/warehouselog/queryproductbarcode` | 成品条码 |
| GET | `/jeecg-boot/warehouselog/warehouselog/queryproductinfo` | 成品信息 |
| GET | `/jeecg-boot/warehouselog/warehouselog/querysku` | 按储位查 |
| GET | `/jeecg-boot/warehouselog/warehouselog/queryskubybarcode` | 条码→储位 |
| GET | `/jeecg-boot/warehouselog/warehouselog/verify` | 校验条码 |
| POST | `/jeecg-boot/warehouselog2/warehouselog2/addpda` | 待检库存 PDA 添加 |
| GET | `/jeecg-boot/warehouselog2/warehouselog2/verify` | 待检校验 |

### 6.3 入库 / 到货 / 收货

| 路径前缀 | 关键动作 |
|----------|----------|
| `/jeecg-boot/entry_notify/entryNotify/` | `list1`, `queryQtyById`, `verifybarcode`, `add1`~`add5`, `addbarcodes`, `completeorder` |
| `/jeecg-boot/matarrival_notify/matarrivalNotify/` | `matlist`, `getTailDb`, `queryQtyById`, `queryMatarrivalDetailByMainIdAndBarcode`, `queryMatarrivalDetailByMainIdAndMatcode`, `add1/2/4` |
| `/jeecg-boot/stockinorder/stockinorder/` | `addbarcode`, `deliverydetail` |
| `/jeecg-boot/stockinorder_barcode/stockinorderBarcode/` | `verify`, `querysumqty` |
| `/jeecg-boot/shouhuodan/shouhuodan/` | `list2`, `list3`, `changestatus` |

### 6.4 领料出库

| 模块 | 关键动作 |
|------|----------|
| `shengchanlingliao/shengchanlingliao` | `list2`, `list3`, `queryShengchanlingliaoDetailByMainId1`, `changestatus`, `changemainstatus` |
| `produce_material_barcode/produceMaterialBarcode` | `verify`, `getrestqty`, `getscanqty`, **`addpda`** |
| `outsource/outsource` | `list2`, `list3`, `queryOutsourcedetailByMainId`, `changestatus`, `changemainstatus`, `createlackmaterial` |
| `outsource_barcode/outsourceBarcode` | `verify`, `getrestqty`, `getscanqty`, **`addpda`** |
| `chaolingdan/chaolingdan` | `list2`, `list3`, `queryChaolingdanDetailByMainId1`, `changestatus` |
| `chaoling_barcode/chaolingBarcode` | `getscanqty`, **`addpda`** |

### 6.5 退料

| 模块 | 关键动作 |
|------|----------|
| `shengchan_tuiliao/shengchanTuiliao` | `list1`, `queryShengchanTuiliaoDetailByMainId1`, `verify`, **`addpda`** |
| `outsoucetuiliao/outsoucetuiliao` | `list1`, `addbarcode` |
| `outsource_tuiliao_barcode/outsourceTuiliaoBarcode` | `verify` |
| `tuiliaochuku/tuiliaochuku` | `list1`, `queryTuiliaoChukuDetailByMainId`, `verify`, `queryBarcodeRestqty`, `finishorder` |
| `tuiliaochuku_barcode/tuiliaochukuBarcode` | `addBarcodeList` |

### 6.6 成品发货

| 路径 | 动作 |
|------|------|
| `/jeecg-boot/deliverylist/delivery/` | `list2`, `addlist`, `completeorder`, `getSubmittedBoxQty`, `querybarcodelist`, `querybarcodestate` |

---

## 7. Shiro PDA 白名单（权威后端清单）

文件：`jeecg-boot-base-core/.../shiro/ShiroConfig.java`，注释段 `//CYWMS`。  
下列路径相对 **context 后的 servlet path**（即不含或含 `/jeecg-boot` 取决于部署；客户端务必带 `/jeecg-boot` 前缀与现网一致）。

要点：

- `/sys/mLogin` → anon  
- `/entry_notify/entryNotify/**`、`/matarrival_notify/matarrivalNotify/**`、`/deliverylist/delivery/**`、`/prodrecv/prodrecv/**` → 整树 anon  
- 大量 `*/addpda`、`verify`、`list1/2/3`、盘点 `stcoktaking`、移库 `stockmoving` → anon  

开发时以 **该白名单 + Controller 源码** 为准；不要擅自把生产鉴权改严/改松。

---

## 8. 关键请求体字段（PDA DTO）

### 8.1 领料扫码提交 `OutsourceBarcodePda` / `ProduceMaterialBarcodePda`

```json
{
  "fk_outsourcedetail": "子单/详情ID",
  "deliveryno": "主单ID",
  "scanner": "操作人用户名",
  "needqty": "需发数量",
  "barcodelist": ["条码1", "条码2"],
  "scanqtyList": ["实发数量1", "实发数量2"],
  "restqtyList": ["余量1", "余量2"]
}
```

路径示例：

- `POST /jeecg-boot/outsource_barcode/outsourceBarcode/addpda`
- `POST /jeecg-boot/produce_material_barcode/produceMaterialBarcode/addpda`

### 8.2 库存初始入库 `WarehouseBarcode`

```json
{
  "sku": "储位码",
  "creator": "操作人",
  "barcodelist": ["物料*数量*..."],
  "restqtylist": ["数量1", "数量2"]
}
```

路径：`POST /jeecg-boot/warehouselog/warehouselog/addpda`  
后端会按条码 `*` 拆物料代码，查 `base_material`，写入 `warehouselog`。

### 8.3 盘点 / 移库

- `stcoktaking`、`stockmoving`：Body 为 `List<Warehouselog>`（字段以后端实体为准）。  
- 旧文案字段痕迹：`jystaff`（盘点人）、`jyremark`（盘点备注）、`ykstaff`（移库人）等。

### 8.4 列表查询常见参数

- `userName` / `username`：当前用户  
- `searchValue`：搜索关键字  
- `id` / `barcode` / `matcode` / `deliveryno` / `orderNo` 等

---

## 9. 与 Web / 数据库的分工

| 端 | 职责 |
|----|------|
| Web（Vue3） | 建单、导入导出、库存台账查询、发货拣货核对、K3 同步等 |
| PDA | 扫码执行：领退料、入库上架、盘点、移库、换标、重检 |
| DB `cywms` | 单据主从表、`warehouselog`、各类 `*_barcode`、欠料表等 |

说明：Vue 端**没有独立「盘点」菜单页**；盘点能力在 PDA + `warehouselog` 接口。发货拣货在 Web 的 `shipment`/`deliverylist` 更完整，PDA 也有成品出库扫码能力。

后端包路径（开发机可对照）：

```
D:\cywms\3.5.5\jeecg-boot\jeecg-module-demo\src\main\java\org\jeecg\modules\demo\
```

Web 视图路径：

```
D:\cywms\3.5.5\jeecgboot-vue3\src\views\
```

---

## 10. 推荐开发里程碑

### Phase 0（1–2 天）— 骨架

1. 新建 Android 工程  
2. `ApiClient` + 可配置 `baseUrl`  
3. 登录页（`mLogin`）+ SharedPreferences  
4. 首页功能网格（按第 5 章菜单）  
5. `ScannerHelper` 注册 `com.service.scanner.data` / `ScanCode`  

### Phase 1 — 现场优先

1. 条码查询 / `verify`  
2. **生产领料**、**委外领料**（list → 明细 → verify → addpda → changestatus）  
3. 入库扫码（entry_notify / stockinorder）  
4. 盘点 `stcoktaking`、移库 `stockmoving`  

### Phase 2 — 完整覆盖

收货、到货、退料全链路、成品出入库、超领、重检、换条码、效期预警、APK 文件下载。

### Phase 3 — 加固

- Android 12+ 广播注册标志  
- 连续扫、禁软键盘抢焦点  
- 重复扫 / FIFO / 箱数尾数校验  
- 与 Web 单据状态联调清单  

---

## 11. 联调检查清单（摘录）

- [ ] 修改服务器地址后可登录  
- [ ] 扫一把枪：EditText/回调能拿到 `ScanCode`  
- [ ] 生产领料：扫码 → 提交 → Web 可见条码与状态变化  
- [ ] 委外领料同上  
- [ ] 入库扫码箱数/尾数累计正确  
- [ ] 盘点提交（注意 URL 拼写 `stcoktaking`）  
- [ ] 移库后储位变化  
- [ ] 换条码 / 重检  
- [ ] 弱网、重复扫、超发数量有提示  

测试账号（现网管理端，谨慎使用）：`admin` / `e`。  
**写库前优先使用测试单或测试库。**

---

## 12. 旧 APK 与附属材料

| 材料 | 说明 |
|------|------|
| 旧 APK | 当前备份路径示例：`D:\OpenSSH\app-debug.apk`（请拷贝到开发机） |
| 原工程包名 | `com.example.cywms`；Flutter package：`cywms` |
| 版本痕迹 | `versionName=1.0.0`，`compileSdk=34` |
| 依赖痕迹（旧 Flutter） | `http`、`shared_preferences`、`fluttertoast`、`flutter_vibrate`、`provider`、`path_provider`、`device_info_plus` |
| 本仓库分析输出 | `D:\cywms\Android\analysis\`（可选拷贝） |
| 本文档 | `D:\cywms\Android\docs\辰阳电子WMS_PDA_Android重建开发说明书.md` |

### 12.1 旧 Flutter 页面文件清单（对照用）

```
globals.dart, main.dart, pages/utils.dart, pages/home.dart
change_barcode.dart
delivery_order.dart, delivery_order_detail.dart
initial_inventory.dart, initial_inventory1.dart
mat_delivery_order.dart ~ mat_delivery_order3.dart
mat_delivery_order_detail.dart ~ detail3.dart
mat_delivery_order_detail_barcode_scan1/2/3.dart
mat_doublecheck.dart, mat_recv_order_detail.dart
outsource_tuiliao.dart, outsource_tuiliao_barcode_scan.dart
product_stockin_scan.dart, product_stockout_scan.dart, producttaking.dart
receipt_order.dart, receipt_order_detail.dart
stockin_order1.dart, stockin_order_barcode.dart
stockin_order_barcode_scan1/2.dart, stockin_order_detail.dart
stockmoving.dart, stocktaking.dart
tuiliaochuku.dart, tuiliaochuku_detail.dart, tuiliaochuku_detail_scan.dart
```

---

## 13. 开发机需要同步的资料包（建议）

请把下列内容打成一个文件夹拷到 PDA 开发电脑：

1. **本文档**（必须）  
2. **旧 APK** `app-debug.apk`（对照扫码与界面）  
3. **后端源码**（至少 `jeecg-module-demo` + `ShiroConfig.java` + `LoginController`）  
4. （可选）Web `src/views` 中对应业务页的 `*.api.ts`，便于看字段  
5. （可选）Swagger：若现网开启，浏览器打开 `{baseUrl}/jeecg-boot/doc.html`  

---

## 14. 注意事项（必读）

1. **接口拼写错误要兼容**：如 `stcoktaking`、`outsoucetuiliao`（少了一个 r）。  
2. **不要改现网 Shiro / Controller**，除非单独发版并回归全部 PDA/Web。  
3. PDA 与 Web 共用同一套业务数据；并发扫码要防重复提交。  
4. 条码规则、箱数/尾数、FIFO 细节以 Controller 实现 + 联调为准；旧文案仅作提示。  
5. 新 App **不要用 Flutter**；扫码直接 `BroadcastReceiver`。  

---

## 15. 附录：旧 App 提取的接口路径（清洗列表）

```
/jeecg-boot/sys/mLogin
/jeecg-boot/sys/common/static
/jeecg-boot/filedownload/filedownload/list1
/jeecg-boot/warehouselog/warehouselog/addpda
/jeecg-boot/warehouselog/warehouselog/changebarcode
/jeecg-boot/warehouselog/warehouselog/changeqty
/jeecg-boot/warehouselog/warehouselog/checkbarcode
/jeecg-boot/warehouselog/warehouselog/checkinfifo
/jeecg-boot/warehouselog/warehouselog/checkinfifo1
/jeecg-boot/warehouselog/warehouselog/doublecheck
/jeecg-boot/warehouselog/warehouselog/getsku
/jeecg-boot/warehouselog/warehouselog/instantledger1
/jeecg-boot/warehouselog/warehouselog/queryalarm
/jeecg-boot/warehouselog/warehouselog/queryalarmlist
/jeecg-boot/warehouselog/warehouselog/querybarcode
/jeecg-boot/warehouselog/warehouselog/querybarcode1
/jeecg-boot/warehouselog/warehouselog/querybarcoderestqty
/jeecg-boot/warehouselog/warehouselog/queryfifo1
/jeecg-boot/warehouselog/warehouselog/querymaterialrestqty
/jeecg-boot/warehouselog/warehouselog/queryproductbarcode
/jeecg-boot/warehouselog/warehouselog/queryproductinfo
/jeecg-boot/warehouselog/warehouselog/querysku
/jeecg-boot/warehouselog/warehouselog/queryskubybarcode
/jeecg-boot/warehouselog/warehouselog/stcoktaking
/jeecg-boot/warehouselog/warehouselog/stockmoving
/jeecg-boot/warehouselog/warehouselog/verify
/jeecg-boot/warehouselog2/warehouselog2/addpda
/jeecg-boot/warehouselog2/warehouselog2/verify
/jeecg-boot/entry_notify/entryNotify/add1
/jeecg-boot/entry_notify/entryNotify/add2
/jeecg-boot/entry_notify/entryNotify/add3
/jeecg-boot/entry_notify/entryNotify/add4
/jeecg-boot/entry_notify/entryNotify/add5
/jeecg-boot/entry_notify/entryNotify/addbarcodes
/jeecg-boot/entry_notify/entryNotify/completeorder
/jeecg-boot/entry_notify/entryNotify/list1
/jeecg-boot/entry_notify/entryNotify/queryQtyById
/jeecg-boot/entry_notify/entryNotify/verifybarcode
/jeecg-boot/matarrival_notify/matarrivalNotify/add1
/jeecg-boot/matarrival_notify/matarrivalNotify/add2
/jeecg-boot/matarrival_notify/matarrivalNotify/add4
/jeecg-boot/matarrival_notify/matarrivalNotify/getTailDb
/jeecg-boot/matarrival_notify/matarrivalNotify/matlist
/jeecg-boot/matarrival_notify/matarrivalNotify/queryMatarrivalDetailByMainIdAndBarcode
/jeecg-boot/matarrival_notify/matarrivalNotify/queryMatarrivalDetailByMainIdAndMatcode
/jeecg-boot/matarrival_notify/matarrivalNotify/queryQtyById
/jeecg-boot/stockinorder/stockinorder/addbarcode
/jeecg-boot/stockinorder/stockinorder/deliverydetail
/jeecg-boot/stockinorder_barcode/stockinorderBarcode/querysumqty
/jeecg-boot/stockinorder_barcode/stockinorderBarcode/verify
/jeecg-boot/shouhuodan/shouhuodan/list2
/jeecg-boot/shouhuodan/shouhuodan/list3
/jeecg-boot/shouhuodan/shouhuodan/changestatus
/jeecg-boot/shengchanlingliao/shengchanlingliao/list2
/jeecg-boot/shengchanlingliao/shengchanlingliao/list3
/jeecg-boot/shengchanlingliao/shengchanlingliao/queryShengchanlingliaoDetailByMainId1
/jeecg-boot/shengchanlingliao/shengchanlingliao/changestatus
/jeecg-boot/shengchanlingliao/shengchanlingliao/changemainstatus
/jeecg-boot/produce_material_barcode/produceMaterialBarcode/verify
/jeecg-boot/produce_material_barcode/produceMaterialBarcode/getrestqty
/jeecg-boot/produce_material_barcode/produceMaterialBarcode/getscanqty
/jeecg-boot/produce_material_barcode/produceMaterialBarcode/addpda
/jeecg-boot/outsource/outsource/list2
/jeecg-boot/outsource/outsource/list3
/jeecg-boot/outsource/outsource/queryOutsourcedetailByMainId
/jeecg-boot/outsource/outsource/changestatus
/jeecg-boot/outsource/outsource/changemainstatus
/jeecg-boot/outsource/outsource/createlackmaterial
/jeecg-boot/outsource_barcode/outsourceBarcode/verify
/jeecg-boot/outsource_barcode/outsourceBarcode/getrestqty
/jeecg-boot/outsource_barcode/outsourceBarcode/getscanqty
/jeecg-boot/outsource_barcode/outsourceBarcode/addpda
/jeecg-boot/chaolingdan/chaolingdan/list2
/jeecg-boot/chaolingdan/chaolingdan/list3
/jeecg-boot/chaolingdan/chaolingdan/queryChaolingdanDetailByMainId1
/jeecg-boot/chaolingdan/chaolingdan/changestatus
/jeecg-boot/chaoling_barcode/chaolingBarcode/getscanqty
/jeecg-boot/chaoling_barcode/chaolingBarcode/addpda
/jeecg-boot/shengchan_tuiliao/shengchanTuiliao/list1
/jeecg-boot/shengchan_tuiliao/shengchanTuiliao/queryShengchanTuiliaoDetailByMainId1
/jeecg-boot/shengchan_tuiliao/shengchanTuiliao/verify
/jeecg-boot/shengchan_tuiliao/shengchanTuiliao/addpda
/jeecg-boot/outsoucetuiliao/outsoucetuiliao/list1
/jeecg-boot/outsoucetuiliao/outsoucetuiliao/addbarcode
/jeecg-boot/outsource_tuiliao_barcode/outsourceTuiliaoBarcode/verify
/jeecg-boot/tuiliaochuku/tuiliaochuku/list1
/jeecg-boot/tuiliaochuku/tuiliaochuku/queryTuiliaoChukuDetailByMainId
/jeecg-boot/tuiliaochuku/tuiliaochuku/verify
/jeecg-boot/tuiliaochuku/tuiliaochuku/queryBarcodeRestqty
/jeecg-boot/tuiliaochuku/tuiliaochuku/finishorder
/jeecg-boot/tuiliaochuku_barcode/tuiliaochukuBarcode/addBarcodeList
/jeecg-boot/deliverylist/delivery/list2
/jeecg-boot/deliverylist/delivery/addlist
/jeecg-boot/deliverylist/delivery/completeorder
/jeecg-boot/deliverylist/delivery/getSubmittedBoxQty
/jeecg-boot/deliverylist/delivery/querybarcodelist
/jeecg-boot/deliverylist/delivery/querybarcodestate
```

---

**文档结束。** 若开发机可访问现网，建议先用 Postman/Apifox 按「登录 → 生产领料 list2 → verify → addpda」跑通一条链路，再写 UI。
