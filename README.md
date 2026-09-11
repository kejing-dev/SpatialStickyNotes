# Spatial Sticky Notes · 空间便利贴（PICO Spatial SDK）

离线、Shared Space 的空间便利贴：在主窗口写下纯文本便签，每条便签以一个独立的平面窗口出现在共享空间里，拖到墙面或桌面后「固定在这里」，远看是一枚标记、走近显示全文，支持编辑、移动、删除，本地自动保存，重启后文字恢复、位置需重新放置。实现依据 [PRD《空间便利贴》v0.1](https://bytedance.us.larkoffice.com/docx/Uo26dYsAqo5gQCxy1ECukgkqs0d)（内部 Lark 文档）与 [Figma「PICO Spatial UI · PRD v0.2」](https://www.figma.com/design/H0KLCZUVvotKl3J2eF29mB/)。

## 技术要点

- **容器**：三个 `Form.Planar` 窗口，全程 Shared Space、不打开 Stage。主窗口 `StickyNotesHome` 为 `DefaultWindowContainer`（Manifest 声明 960 × 600 dp，`ContentSize`，`WorldScale.Dynamic`；内容最小 900 × 560 dp）；编辑窗口 `StickyNoteEditor` 固定 560 × 640 dp、等比缩放、带材质背景；便签窗口 `StickyNoteWindow` 固定 400 × 400 dp、**无材质背景**（黄色卡片直接贴在空间里）、标题栏自动隐藏、`WorldScale.Dynamic`，**每条便签一个窗口**，tag 即便签 id。
- **设计决策**：按用户要求不做 MR 平面检测、不用空间锚点，Manifest 不申请任何权限；便签窗口的位置与贴面完全交给 PICO OS 窗口系统，App 只记录放置状态、不记录坐标。代价是冷启动后位置无法恢复——上次已固定的便签会标记为「上次位置未恢复 · 待重新放置」，从列表一键「放回空间」。
- **放置状态机**：`PENDING`（提交后在自己的窗口里打开，提示「拖动标题栏放到墙面或桌面」）→ 点「固定在这里」立即进入 `PLACED` 并写盘（成功时状态胶囊高亮「已固定在空间 · 已保存」2.5 s；失败时胶囊提示保存失败、`dirty` 置位、主窗口标题栏出现「重试保存」）；窗口聚焦时出现 编辑 / 移动 / 删除 工具条；「移动」回到 `PENDING`；启动时 `PLACED` 一律改为 `NEEDS_REPLACE`。
- **LOD**：Shared Space 读不到头部位姿，`lod/ViewerDistance.kt` 用 `LocalPhysicalLengthConverter.dpToLength(100.dp, Meters)` 每 200 ms 采样——`WorldScale.Dynamic` 会随观看距离改变 dp→米换算系数，取第一次采样作为约 1.75 m 的开窗基线，后续按比例折算距离。`LodController` 带迟滞：≤ 1.5 m 进入近档（全文、可滚动），≥ 1.8 m 退回远档（72 dp 「≡」标记，点击标记或（聚焦时）「展开」可手动展开，回到近档后自动复位）；待放置 / 未固定时始终显示全文。**该距离是估算值**：模拟器中默认开窗距离约 1.75 m 落在远档，尚未在相机 / 头显移动下验证；若平台不改变换算系数，估算恒为约 1.75 m、已固定的便签会停在远档；仅当采样失败（`dpToLength` 抛错或返回 ≤ 0）时无估算、保持全文。
- **UI**：PICO Spatial UI —— `PicoTheme`、`TitleBar`（标题 + 「新建便签」，写盘失败时多出「重试保存」）、`ListItem` 便签列表（摘要、状态行、放置 / 查看 / 放回空间 · 编辑 · 删除）、`TextArea` 编辑框、`Button`、`AlertDialog`（删除确认）、Snackbar；自绘 `StickyCard`（328 dp 黄色卡片）、`StickyMarker`、`StatusPill`、`Panel`，颜色 token 取自 Figma「02 本地组件」页（`ui/Tokens.kt`）。
- **数据**：`NoteStore` 单例，`files/notes.json` 一个 JSON 数组（id / text / createdAt / updatedAt / placement）。每次变更在 `Mutex` 内先更新内存再于 IO 线程写盘：写 `notes.json.tmp` 后 `rename`（失败回退直接覆盖）；每个操作返回 `Result`，失败置 `dirty` 并给出重试；启动时按 `updatedAt` 倒序加载、新建置顶（编辑不改变顺序），未知状态值回退为 `NEEDS_REPLACE`。
- **限制与反馈**：单条 **200 个码点**（`codePointCount`，中文与单码点 emoji 计 1、组合 emoji 按码点数计，输入时自动截断，计数 `n / 200` 到顶变红）；最多 **30 条**（主窗口与 `NoteStore.create` 双重把关，超限 Snackbar 提示删除旧便签）；空白不能提交；删除在列表和便签窗口里都需要一次 `AlertDialog` 确认，删除后同时关闭该便签的窗口与其编辑窗口（若打开）。
- **入口**：`SpatialApplication.onCreate` → `NoteStore.init` → `launch(::mainApp)`；`LaunchActivity` 继承 `SpatialLaunchActivity`。

## 下载

- **APK**：见 GitHub Releases `v1.0.0`（`SpatialStickyNotes-debug.apk`，安装到 PICO Emulator 6.1 / PICO OS 6 设备：`pico-cli app install SpatialStickyNotes-debug.apk`）。
- **模拟器截图与验证报告**：[`deliverables/SpatialStickyNotes_模拟器截图与验证.xlsx`](deliverables/SpatialStickyNotes_模拟器截图与验证.xlsx)（三个工作表：应用信息、PRD 功能覆盖 F01–F10 与 4.1–4.3、17 个测试截图场景）；单张截图在 [`deliverables/screenshots/`](deliverables/screenshots/)。

## 构建与运行

```bash
export JAVA_HOME=~/.pico/primer-cli/jdk/jdk-21.0.12.1+1/Contents/Home
./gradlew :app:assembleDebug
pico-cli emulator start            # 或已在运行的 PICO 设备
pico-cli app install app/build/outputs/apk/debug/app-debug.apk
pico-cli app launch com.example.spatialstickynotes
pico-cli capture screenshot -o shot.png
```

## 模拟器测试（无调试广播通道）

本应用没有单独的调试广播通道，所有操作都走真实 UI。模拟器不支持脚本化的空间射线输入，测试时用 `--layerid` 把点击投到当前焦点窗口的表面上（坐标是该窗口自身表面内的坐标，不是整屏坐标）：

```bash
adb shell input tap <x> <y> --layerid       # 点击焦点窗口内的按钮（新建便签 / 固定在这里 / 编辑 …）
adb shell input text "Hello"                # 编辑框键入（仅 ASCII）
adb logcat -s ViewerDistance NoteStore      # 距离估算采样 / 写盘失败日志
```

拖动便签窗口仍需在模拟器里用鼠标拖系统标题栏；LOD 的远近切换在模拟器中只验证了默认开窗距离落在远档、以及「展开 / 收起」的手动覆盖。

## 目录

- `app/src/main/java/com/example/spatialstickynotes/`
  - `Main.kt` 入口 DSL（三个 WindowContainer）· `platform/` Application 与 LaunchActivity · `content/` 主窗口 / 编辑窗口 / 便签窗口与 `Windows` 常量 · `ui/` Figma 组件与颜色 token · `lod/ViewerDistance.kt` 距离估算与迟滞 · `data/` Note 模型、`NoteStore` 持久化、`NoteSession` 运行时状态
- `deliverables/` 模拟器截图与验证表（Excel）与截图
