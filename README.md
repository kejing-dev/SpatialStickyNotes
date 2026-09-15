# Spatial Sticky Notes · 空间便利贴（PICO Spatial SDK）

离线、Shared Space 的空间便利贴：主窗口是便签列表，每条便签是一个独立的平面窗口，直接以一张黄色卡片的样子贴在共享空间里。点一下便签即选中（系统标题栏随之出现，拖它就能移动），再点一下或点 ✎ 就在卡片上原地编辑，远看折叠成一枚小标记、走近显示全文；本地自动保存，重启后文字恢复，窗口位置完全交给 PICO OS。整套界面只用图标，唯一的文字是删除确认对话框（英文）；便签正文不限语言。实现依据 [PRD《空间便利贴》v0.1](https://bytedance.us.larkoffice.com/docx/Uo26dYsAqo5gQCxy1ECukgkqs0d)（内部 Lark 文档）与 [Figma「PICO Spatial UI · PRD v0.2」](https://www.figma.com/design/H0KLCZUVvotKl3J2eF29mB/)（页面「01 用户路径」第 09 / 09b / 10 / 11 节，v1.2 定稿）。当前版本 `versionName 1.2`、`versionCode 3`。

## v1.2 相比 v1.0 的变化

- **没有放置状态机了**：待放置 / 固定在这里 / 移动 / 放回空间 以及 JSON 里的 `placement` 字段全部移除，便签窗口的位置与贴面完全由 PICO OS 窗口系统负责，App 只存文字；旧 `notes.json` 里的 `placement` 键读取时忽略。
- **没有独立编辑窗口了**：560 × 640 dp 的编辑窗口（`NoteEditorWindow.kt`）删除，新建与编辑都在便签窗口的卡片上行内完成；容器从三个减为两个。
- **界面改为纯图标**：文字按钮、`StatusPill`、Snackbar、`n / 200` 计数文字都不再存在，换成 `ActionStrip` 图标工具条、`CounterRing` 计数环、标题栏的「+」容量环与「!」重试图标（原「重试保存」文字按钮）。
- **唯一的界面文字是英文**：删除确认对话框的 "Delete" 标题；对话框按钮仍为纯图标 ✕（取消，secondary）/ 🗑（删除，danger），120 × 52 dp、28 dp 图标；应用名改为 "Spatial Sticky Notes"。便签正文仍可为中文（测试数据即中文），有意不做中文界面版本。
- **远档标记**由 72 dp 的「≡」改为 96 dp 的折叠小便签 `StickyMarker`（带展开角标）；便签窗口标题栏由自动隐藏改为常显，作为唯一的移动手段。

## 交互模型

- **选中 == 系统窗口焦点**：点一下便签，PICO OS 把焦点交给该窗口，系统标题栏（caption bar）出现——拖标题栏即移动，App 自身不提供移动或放置功能。选中时卡片加 2 dp 强调色描边，卡片正下方（间距 12 dp）居中出现一个胶囊工具条 `ActionStrip`：✎ | 🗑（远处手动展开时末尾多一个 ⤡ 收起）——工具条在卡片下方而不是角上，永远不会盖住便签文字。便签窗口上没有「!」：写盘失败只由主窗口标题栏的「!」（点击重试）和列表行的红点提示。
- **编辑**：点 ✎，或再点一下已选中的卡片（把焦点交给窗口的那一下不算，聚焦后 600 ms 内的点击忽略），卡片原地变成 PICO `TextArea`（不自动聚焦：需要再点入文本才有光标；Enter 换行），右下角出现 200 码点的计数环 `CounterRing`，卡片下方的工具条只剩一个 ✓（文字非空白时可用）。点入文本后卡片旁出现的圆形小键盘按钮是 PICO OS 输入法（`com.bytedance.pico.inputmethod`）的系统组件，不是 App 元素：它只在某个文本框持有光标时出现，是头显上唤起系统键盘的方式，App 无法去掉它，只是不在点 ✎ 时触发。编辑期间 600 ms 防抖自动保存；✓ 或窗口失焦都结束编辑（已自动保存），没有取消。
- **新建**：主窗口标题栏的「+」打开一个新的便签窗口并直接进入编辑态（草稿）。`NoteSession` 同时只保留一个草稿，再点「+」只是把已打开的草稿窗口拉到前面；草稿计入 30 条上限；✓ 才真正创建便签（id 就是窗口 tag，窗口身份不变）；空白草稿在 ✓ / 失焦时直接丢弃并关窗。
- **删除**：工具条或列表行的 🗑 → PICO `AlertDialog`（`Orientation.Vertical`，360 dp，放得进 400 dp 的便签窗口）：标题 "Delete"、一块 300 dp 的黄色便签预览（最多 3 行）、两个纯图标按钮 ✕（取消，secondary）/ 🗑（删除，danger），120 × 52 dp、28 dp 图标；只有标题 "Delete" 是文字。确认后 `NoteStore.delete` 并关闭该便签窗口，从列表侧或便签侧删都一样；`deleted` 标志保证之后不会有任何写回。
- **远 / 近**：远处（≥ 1.8 m）显示折叠标记 `StickyMarker`，点一下展开（`manuallyExpanded`），工具条里的 ⤡ 收起；近处（≤ 1.5 m）显示完整卡片；聚焦或编辑中 LOD 冻结。
- **未聚焦**：只有卡片（或标记），没有工具条；便签上不显示任何保存状态（写盘失败只在主窗口提示）。

## 技术要点

- **容器**：两个 `Form.Planar` 窗口，全程 Shared Space、不开 Stage、不做 MR 平面检测、不用空间锚点，Manifest 不申请任何权限。主窗口 `StickyNotesHome` 为 `DefaultWindowContainer`（Manifest 声明 960 × 600 dp，`ContentSize`，`WorldScale.Dynamic`；内容最小 900 × 560 dp）；便签窗口 `StickyNoteWindow` 固定 400 × 400 dp（`windowConstraints` 最小 = 最大）、`UniformResizable`、**无材质背景**（黄色卡片直接贴在空间里）、`CaptionBarType.Default`、`WorldScale.Dynamic`，**每条便签一个窗口**，tag 即便签 id，bundle 带 `noteId` 与 `draft` 标志。窗口内 328 × 264 dp 的 `StickyCard`（圆角 20）靠上放置（窗口内容区 12 dp 边距 + 卡片 24 dp 上边距，卡片顶距窗口上沿 36 dp），`ActionStrip` 在卡片下方 12 dp 处居中；系统标题栏在窗口之外。
- **主窗口**：`PicoTheme` `TitleBar`，左侧应用图标（`ic_note`），右侧一个 danger「!」（仅在写盘失败 / `dirty` 时出现，点击 `NoteStore.retrySaveAsync`）和一个圆形强调色「+」，外面套一圈容量环（n / 30，n = 已存便签 + 草稿；到 30 时环变 danger、按钮禁用变次要色）。列表行：40 dp 黄色小卡（右上强调色圆点 = 便签窗口已打开，右下 danger 圆点 = 未保存）、单行摘要（第一段非空行，最多 60 码点）、46 × 40 的 danger 🗑；点行打开 / 唤起该便签窗口。空态：便签图标 + 大「+」。
- **每窗口系统标题**：取便签前 ≤ 12 码点写入 `Activity.title` 与 `TaskDescription`，多个便签窗口在系统里可区分；空白草稿保持应用名。
- **LOD**：Shared Space 读不到头部位姿，`lod/ViewerDistance.kt` 用 `LocalPhysicalLengthConverter.dpToLength(100.dp, Meters)` 每 200 ms 采样——`WorldScale.Dynamic` 会随观看距离改变 dp→米换算系数，取第一次采样作为约 1.75 m 的开窗基线，后续按比例折算距离。`LodController` 带迟滞：≤ 1.5 m 进入近档（全文、可滚动），≥ 1.8 m 退回远档；第一次采样直接决定近 / 远（< 1.8 m 为近）；聚焦或编辑中不更新。**该距离是估算值**：模拟器中默认开窗距离约 1.75 m 落在近档，尚未在相机 / 头显移动下验证（模拟器相机无法从 adb 移动）；若平台不改变换算系数，估算恒为约 1.75 m、便签会一直停在近档；仅当采样失败（`dpToLength` 抛错或返回 ≤ 0）时无估算、保持全文。
- **UI**：PICO Spatial UI —— `PicoTheme`、`TitleBar`、`TextArea`、`IconButton`、`AlertDialog`（删除确认，按钮亦为 `IconBtn` 图标）；自绘 `IconBtn`（默认 56 × 48 dp、30 dp 图标，Secondary / Danger / Accent 三种角色）、`ActionStrip` + `StripDivider`、`StickyCard`、`StickyMarker`（96 dp）、`CounterRing`（20 dp 环，80% 变强调色、到顶变 danger）、`DeleteConfirmDialog`；图标为 24 dp 描边矢量 `ic_{edit,delete,check,close,expand,collapse,plus,error,note}`；卡片与标记使用 `spatialHoverEffect` 与默认按压反馈。颜色 token 取自 Figma「02 本地组件」页（`ui/Tokens.kt`：Canvas `#F4F5F1`、Ink `#1F2A25`、Muted `#6A756E`、Border `#CAD2C6`、Accent `#2E5F4B`、Secondary `#DCE4D9`、DangerBg `#F0D9D3`、DangerText `#8C3B2D`、NoteFill `#F5E9A9`、NoteInk `#2B2F27`、Pill `#F6F7F3`）。
- **数据**：`NoteStore` 单例，`files/notes.json` 一个 JSON 数组（id / text / createdAt / updatedAt）。每次变更在 `Mutex` 内先更新内存再于 IO 线程写盘：写 `notes.json.tmp` 后 `rename`（失败回退直接覆盖）；`create(id, text)` 幂等（同 id 视为更新），存在性与容量检查都在锁内；提交段 `NonCancellable`，保证 `dirty` / `unsavedIds` 总能反映真实结果；写盘失败 → `dirty` + `unsavedIds` → 主窗口标题栏出现「!」（点击重试）、列表行出现 danger 圆点；便签窗口本身不显示任何提示。启动时按 `updatedAt` 倒序加载、新建置顶（编辑不改变顺序）。`NoteStore.scope`（`SupervisorJob` + IO）承接必须在窗口关闭后仍完成的写入（✓ 后立即关窗、失焦自动保存）。
- **限制**：单条 **200 个码点**（`codePointCount`，中文与单码点 emoji 计 1、组合 emoji 按码点数计，输入时自动截断）；最多 **30 条**（主窗口「+」与 `NoteStore.create` 双重把关）；空白不能 ✓。
- **评审修正**（2026-09-14，三视角对抗式代码评审，均已在模拟器复验）：草稿窗口在「+」后立即自行关闭（现在只有真实失焦才结束编辑）、删除已打开的便签后被 dispose 写回复活（`deleted` / `wasPersisted` 标志）、`NoteStore.create` 的检查与写入不在同一把锁内（改为锁内幂等）、写盘中途被取消丢失 `dirty` 标志（`NonCancellable` 提交）、便签侧的「!」重试先并入工具条、后按用户要求整个去掉（重试只在主窗口标题栏）、删除对话框 480 dp 超出 400 dp 窗口（改 `Orientation.Vertical` 360 dp）、`ImeAction.Done` 吞掉换行、编辑框自动聚焦（按用户要求撤回：不自动聚焦，点 ✎ 不再弹出系统键盘按钮）、合成时间戳标题与死代码清理。
- **入口**：`SpatialApplication.onCreate` → `NoteStore.init` → `launch(::mainApp)`；`LaunchActivity` 继承 `SpatialLaunchActivity`。

## 下载

- **APK**：见 GitHub Releases `v1.2.0`（`SpatialStickyNotes-debug.apk`，安装到 PICO Emulator 6.1 / PICO OS 6 设备：`pico-cli app install SpatialStickyNotes-debug.apk` → `pico-cli app launch com.example.spatialstickynotes`）。
- **模拟器截图与验证报告**：[`deliverables/SpatialStickyNotes_模拟器截图与验证.xlsx`](deliverables/SpatialStickyNotes_模拟器截图与验证.xlsx)（应用信息、PRD 功能覆盖、逐场景截图与验证结论）；单张截图（JPEG 1600 px）在 [`deliverables/screenshots/`](deliverables/screenshots/)：
  `01_home_list` 主窗口列表 · `02_note_selected` 打开便签 → 聚焦态（描边 + 卡片下方 ✎ / 🗑 + 系统标题栏） · `03_note_editing` ✎ 行内编辑（TextArea + 计数环 + 下方 ✓，尚未点入文本，无键盘按钮） · `04_note_typing` 点入文本后键入（系统键盘按钮为 PICO OS 输入法组件） · `05_note_done` ✓ 完成、列表同步 · `06_save_failed` 写盘失败：主窗口标题栏出现「!」、列表行出现红点，便签本身不变 · `07_retry_ok` 点主窗口标题栏「!」重试成功 · `08_draft_new` 「+」新建草稿：空编辑卡片、下方 ✓ 禁用 · `09_draft_typed` 点入文本后键入 · `10_draft_created` ✓ 创建 · `11_delete_dialog` 便签内删除确认 · `12_deleted` 删除后 · `13_home_delete_dialog` 列表侧删除确认（已打开的便签） · `14_home_deleted` 列表侧删除后窗口关闭 · `15_note_idle` 未聚焦便签（无工具条）。（取自 v1.2 早期构建、相机移动后的机位；最终构建下 adb 让主窗口取焦会把它抬到便签前面）
- v1.0 / v1.1 的旧截图、Figma 导入 SVG 与旧验证表已归档到 `deliverables/archive-v1.1/`，仅作历史参考，不再对应当前界面。

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

本应用没有单独的调试广播通道，所有操作都走真实 UI（验证环境：PICO Emulator 6.1，AVD `Pico_Emulator_6_1`，PICO OS 6.1，2160 × 2158，pico-cli 0.5.0）。模拟器不支持脚本化的空间射线输入，测试时用 `--layerid` 把点击投到当前焦点窗口的表面上——坐标是该窗口自身表面内的坐标，不是整屏坐标；便签窗口的表面是 800 × 800 px（400 dp 窗口），主窗口另有自己的表面：

```bash
adb shell input tap <x> <y> --layerid       # 点击焦点窗口内的图标（+ / ✎ / ✓ / 🗑 / ! …）或卡片
adb shell input text "Hello"                # 键入（仅 ASCII）；编辑器不自动聚焦，要先 tap 一下 TextArea 让它拿到光标
adb logcat -s ViewerDistance NoteStore      # 距离估算采样 / 写盘失败日志
```

写盘失败场景用 `adb shell run-as com.example.spatialstickynotes chmod` 把 `files/` 目录设为只读来触发，恢复后点主窗口标题栏的「!」重试。键入前先 tap TextArea 取得光标，此时卡片旁出现的圆形键盘按钮来自 PICO OS 输入法（`com.bytedance.pico.inputmethod`），不属于 App。拖动便签窗口仍需在模拟器里用鼠标拖系统标题栏。模拟器上无法验证的部分：远 / 近 LOD 切换（相机不能移动，只验证了默认开窗距离落在近档；远档标记、一点展开与 ⤡ 收起均未能在模拟器上触发）、真机键盘弹出行为、真机整体表现。

## 目录

- `app/src/main/java/com/example/spatialstickynotes/`
  - `Main.kt` 入口 DSL（两个 WindowContainer）· `platform/` Application 与 LaunchActivity · `content/` `HomeWindow` 主窗口 / `NoteWindow` 便签窗口 / `Windows` 常量与 bundle · `ui/` `Components`（IconBtn、ActionStrip、StripDivider、StickyCard、StickyMarker、CounterRing、DeleteConfirmDialog）与 `Tokens` 颜色 token · `lod/ViewerDistance.kt` 距离估算与迟滞 · `data/` `Note` 模型、`NoteStore` 持久化、`NoteSession` 运行时状态（已打开 / 草稿窗口）
- `app/src/main/res/drawable/ic_*.xml` 24 dp 描边图标
- `deliverables/` 模拟器截图与验证表（Excel）、截图，以及 `archive-v1.1/` 旧版素材
