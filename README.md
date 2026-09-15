# Spatial Sticky Notes · 空间便利贴（PICO Spatial SDK）

离线、Shared Space 的空间便利贴：主窗口是便签列表，每条便签是一个独立的平面窗口，直接以一张黄色卡片的样子贴在共享空间里。点一下便签即选中（系统标题栏随之出现，拖它就能移动），再点一下或点 ✎ 就在卡片上原地编辑；v1.3 起卡片高度随文字自适应（1–7 行）、便签窗口也随内容高度伸缩，工具条里的 ⤡ 把便签手动收起成一枚小标记（收起态随便签保存，重启后仍是收起的），点标记即展开；本地自动保存，重启后文字恢复，窗口位置完全交给 PICO OS。整套界面只用图标，唯一的文字是删除确认对话框（英文）；便签正文不限语言。实现依据 [PRD《空间便利贴》v0.1](https://bytedance.us.larkoffice.com/docx/Uo26dYsAqo5gQCxy1ECukgkqs0d)（内部 Lark 文档）与 [Figma「PICO Spatial UI · PRD v0.2」](https://www.figma.com/design/H0KLCZUVvotKl3J2eF29mB/)（页面「01 用户路径」第 09 / 09b / 10 / 11 节为 v1.2 定稿，第 13 节「便签窗口 v1.3 · 手动收起 + 自适应高度」为 v1.3 定稿）。当前版本 `versionName 1.3`、`versionCode 4`。

## v1.3 相比 v1.2 的变化

用户在头显上实测 v1.2：按距离折叠从未触发（距离只是由 dp→米换算系数推出的估算值，真机上不可靠），于是 v1.3 把它整个换成手动收起，并让卡片与窗口的高度跟随内容。

- **距离 LOD 去掉**：`lod/ViewerDistance.kt`（`LodController`、`rememberViewerDistanceEstimate`）删除，不再有 1.5 / 1.8 m 阈值、迟滞与 200 ms 采样；容器仍声明 `WorldScale.Dynamic`，但已没有任何代码读换算系数。
- **⤡ 手动收起，且持久化**：选中态工具条改为 ✎ | 🗑 | ⤡（`ic_collapse`，两枚向内的尖角，与标记上展开角标相反）。⤡ 把便签收成 96 dp 的 `StickyMarker`，点标记任意位置展开；收起态作为 `"collapsed": true|false` 写进 `files/notes.json`（`Note.collapsed`、`NoteStore.setCollapsed`），重启后仍是收起的；不改 `updatedAt`，列表顺序不变；草稿（尚未 ✓ 的新便签）没有 ⤡。
- **卡片高度自适应**：`StickyCard` 宽仍 328 dp，高度随文字在 74 dp（1 行）到 254 dp（7 行）之间变化（`Tokens.NoteCardMinHeight / NoteCardMaxHeight`，高度 = 行数 × 30 + 44），更长的便签在卡片内滚动；行内编辑器至少 4 行（164 dp，`Tokens.NoteEditorMinHeight`），随文字长到 254 dp。
- **窗口跟随内容高度**：便签窗口仍 400 dp 宽、`ContainerResizeType.ContentSize`；`NoteWindow` 把量出的内容高度回报给 `Main.kt`，后者用 `windowConstraints(minHeight = maxHeight = contentHeight)` 把窗口钉在内容高度上（SDK 在约束变化时调整容器）；内容以无上限高度测量，窗口既能缩也能长。模拟器上：1 行便签 194 dp、2 行 220 dp、编辑中 284 dp，收起后请求 144 dp 而系统抬到约 180 dp 的下限。
- **点击修复**：`StickyCard` / `StickyMarker` 改回不带 indication 的普通 `clickable`——在 PICO OS 6.1 上，`PicoTheme` 的默认按压指示 / `spatialHoverEffect` 会让点击到不了卡片（再点一下进编辑、点标记展开都无声失效）。

## v1.2 相比 v1.0 的变化

- **没有放置状态机了**：待放置 / 固定在这里 / 移动 / 放回空间 以及 JSON 里的 `placement` 字段全部移除，便签窗口的位置与贴面完全由 PICO OS 窗口系统负责，App 只存文字；旧 `notes.json` 里的 `placement` 键读取时忽略。
- **没有独立编辑窗口了**：560 × 640 dp 的编辑窗口（`NoteEditorWindow.kt`）删除，新建与编辑都在便签窗口的卡片上行内完成；容器从三个减为两个。
- **界面改为纯图标**：文字按钮、`StatusPill`、Snackbar、`n / 200` 计数文字都不再存在，换成 `ActionStrip` 图标工具条、`CounterRing` 计数环、标题栏的「+」容量环与「!」重试图标（原「重试保存」文字按钮）。
- **唯一的界面文字是英文**：删除确认对话框的 "Delete" 标题；对话框按钮仍为纯图标 ✕（取消，secondary）/ 🗑（删除，danger），120 × 52 dp、28 dp 图标；应用名改为 "Spatial Sticky Notes"。便签正文仍可为中文（测试数据即中文），有意不做中文界面版本。
- **折叠小标记**由 72 dp 的「≡」改为 96 dp 的折叠小便签 `StickyMarker`（带展开角标）；便签窗口标题栏由自动隐藏改为常显，作为唯一的移动手段。

## 交互模型

- **选中 == 系统窗口焦点**：点一下便签，PICO OS 把焦点交给该窗口，系统标题栏（caption bar）出现——拖标题栏即移动，App 自身不提供移动或放置功能。选中时卡片加 2 dp 强调色描边，卡片正下方（间距 12 dp）居中出现一个胶囊工具条 `ActionStrip`：✎ | 🗑 | ⤡（草稿没有 ⤡）——工具条在卡片下方而不是角上，永远不会盖住便签文字；卡片未收起时工具条的 60 dp 槽位始终预留，所以聚焦 / 失焦时窗口高度不跳。便签窗口上没有「!」：写盘失败只由主窗口标题栏的「!」（点击重试）和列表行的红点提示。
- **编辑**：点 ✎，或再点一下已选中的卡片（把焦点交给窗口的那一下不算，聚焦后 600 ms 内的点击忽略），卡片原地变成 PICO `TextArea`（不自动聚焦：需要再点入文本才有光标；Enter 换行），右下角出现 200 码点的计数环 `CounterRing`，卡片下方的工具条只剩一个 ✓（文字非空白时可用）；编辑器至少 4 行高（164 dp），随文字长到 254 dp，窗口跟着变高；即使便签存的是收起态，编辑时也总是显示完整编辑器。点入文本后卡片旁出现的圆形小键盘按钮是 PICO OS 输入法（`com.bytedance.pico.inputmethod`）的系统组件，不是 App 元素：它只在某个文本框持有光标时出现，是头显上唤起系统键盘的方式，App 无法去掉它，只是不在点 ✎ 时触发。编辑期间 600 ms 防抖自动保存；✓ 或窗口失焦都结束编辑（已自动保存），没有取消。
- **新建**：主窗口标题栏的「+」打开一个新的便签窗口并直接进入编辑态（草稿）。`NoteSession` 同时只保留一个草稿，再点「+」只是把已打开的草稿窗口拉到前面；草稿计入 30 条上限；✓ 才真正创建便签（id 就是窗口 tag，窗口身份不变）；草稿的工具条没有 ⤡，创建后才能收起；空白草稿在 ✓ / 失焦时直接丢弃并关窗。
- **删除**：工具条或列表行的 🗑 → PICO `AlertDialog`（`Orientation.Vertical`，360 dp，放得进 400 dp 的便签窗口）：标题 "Delete"、一块 300 dp 的黄色便签预览（最多 3 行）、两个纯图标按钮 ✕（取消，secondary）/ 🗑（删除，danger），120 × 52 dp、28 dp 图标；只有标题 "Delete" 是文字；对话框打开期间便签窗口临时放大到 400 dp 高，收起或单行的小窗口也放得下。确认后 `NoteStore.delete` 并关闭该便签窗口，从列表侧或便签侧删都一样；`deleted` 标志保证之后不会有任何写回。
- **收起 / 展开**：工具条里的 ⤡ 把便签收成 96 dp 的折叠标记 `StickyMarker`（带强调色展开角标），标记居中放在卡片原来的位置，窗口缩到系统允许的最小高度；收起的便签没有工具条（聚焦时只有 2 dp 描边），点标记任意位置即展开回卡片。收起态随便签保存，重启后打开仍是收起的；列表顺序不受影响。
- **未聚焦**：只有卡片（或收起后的标记），没有工具条；便签上不显示任何保存状态（写盘失败只在主窗口提示）。

## 技术要点

- **容器**：两个 `Form.Planar` 窗口，全程 Shared Space、不开 Stage、不做 MR 平面检测、不用空间锚点，Manifest 不申请任何权限。主窗口 `StickyNotesHome` 为 `DefaultWindowContainer`（Manifest 声明 960 × 600 dp，`ContentSize`，`WorldScale.Dynamic`；内容最小 900 × 560 dp）；便签窗口 `StickyNoteWindow` 宽固定 400 dp、**高度钉在内容高度上**（`ContainerResizeType.ContentSize`；`Main.kt` 用 `windowConstraints(minWidth = maxWidth = 400.dp, minHeight = maxHeight = contentHeight)` 把最小 = 最大高度设成内容高度，SDK 在约束变化时调整容器）、`UniformResizable`、**无材质背景**（黄色卡片直接贴在空间里）、`CaptionBarType.Default`、`WorldScale.Dynamic`（仍声明，但已没有代码读它），**每条便签一个窗口**，tag 即便签 id，bundle 带 `noteId` 与 `draft` 标志。内容高度由 `NoteWindow` 量出：内容列用 `Modifier.layout` 以无上限高度（`maxHeight = Constraints.Infinity`）测量——普通 `wrapContent` 会被当前窗口的上限夹住，窗口就只能缩不能长——再加外层 12 dp 上下边距经 `onContentHeight` 回报，`Main.kt` 夹在 120–400 dp 内。窗口内 `StickyCard`（328 dp 宽、圆角 20、高度随文字）靠上放置（内容区 12 dp 边距 + 卡片 24 dp 上边距，卡片顶距窗口上沿 36 dp），下方 12 dp 处是一个始终预留的 60 dp 工具条槽位（`ActionStrip` 在其中居中；预留是为了聚焦 / 失焦时窗口不跳），收起后不再保留槽位。模拟器上观察到的窗口高度：1 行便签 194 dp（24 + 74 + 12 + 60 + 24）、2 行 220 dp、编辑中 284 dp；收起后请求 144 dp，但 PICO OS 有约 180 dp 的窗口下限，于是系统标题栏落在标记下方约 36 dp 处。删除对话框打开期间窗口临时开到 400 dp，对话框不会被裁掉；系统标题栏在窗口之外；主窗口内容铺一层近白色底（`Tokens.Canvas` 85% 不透明）盖在系统材质玻璃上，避免深色环境把面板衬成灰色。
- **主窗口**：`PicoTheme` `TitleBar`，左侧应用图标（`ic_note`），右侧一个 danger「!」（仅在写盘失败 / `dirty` 时出现，点击 `NoteStore.retrySaveAsync`）和一个圆形强调色「+」，外面套一圈容量环（n / 30，n = 已存便签 + 草稿；到 30 时环变 danger、按钮禁用变次要色）。列表行：40 dp 黄色小卡（右上强调色圆点 = 便签窗口已打开，右下 danger 圆点 = 未保存）、单行摘要（第一段非空行，最多 60 码点）、46 × 40 的 danger 🗑；点行打开 / 唤起该便签窗口。空态：便签图标 + 大「+」。
- **每窗口系统标题**：取便签前 ≤ 12 码点写入 `Activity.title` 与 `TaskDescription`，多个便签窗口在系统里可区分；空白草稿保持应用名。
- **收起 / 展开（手动）**：v1.3 没有任何距离估算，也不读头部位姿。`NoteWindow` 里 `collapsed = !editing && note.collapsed`：工具条的 ⤡（`ic_collapse`）调用 `NoteStore.setCollapsed(id, true)`，卡片的位置换成 96 dp 的 `StickyMarker`（折叠小便签 + 32 dp 强调色 `ic_expand` 角标，居中在卡片原位），标记的 `onClick` 再写回 `false`。⤡ 只在 `note != null` 时渲染（草稿不能收起）；编辑态优先于收起态（编辑中永远是完整编辑器）；收起后不渲染工具条槽位，窗口随之缩到系统下限；`setCollapsed` 在锁内提交但不改 `updatedAt`，列表顺序不变。
- **UI**：PICO Spatial UI —— `PicoTheme`、`TitleBar`、`TextArea`、`IconButton`、`AlertDialog`（删除确认，按钮亦为 `IconBtn` 图标）；自绘 `IconBtn`（默认 56 × 48 dp、30 dp 图标，Secondary / Danger / Accent 三种角色）、`ActionStrip` + `StripDivider`、`StickyCard`（328 dp 宽，`heightIn(min = NoteCardMinHeight 74 dp, max = NoteCardMaxHeight 254 dp)`，内边距水平 24 / 垂直 22 dp，高度 = 行数 × 30 + 44，超过 7 行在卡片内 `verticalScroll`）、`StickyMarker`（96 dp）、`CounterRing`（20 dp 环，80% 变强调色、到顶变 danger）、`DeleteConfirmDialog`；行内编辑器 `EditingCard` 用 `heightIn(min = NoteEditorMinHeight 164 dp, max = 254 dp)` + `minLines = 4`。便签正文样式 `Tokens.noteTextStyle`：20 sp、行距精确 30 dp，并设 `LineHeightStyle(Trim.None)`——Compose（PICO 分支）默认裁掉首尾行的半行距，n 行会量成 30n − 6 dp，Trim.None 让 1 行 / 7 行的卡片刚好 74 / 254 dp。图标为 24 dp 描边矢量 `ic_{edit,delete,check,close,expand,collapse,plus,error,note}`；卡片与标记改用不带 indication 的普通 `clickable(interactionSource, indication = null)`，不再挂 `spatialHoverEffect` 与默认按压反馈（PICO OS 6.1 上那两者会让点击到不了卡片）。颜色 token 取自 Figma「02 本地组件」页（`ui/Tokens.kt`：Canvas `#F4F5F1`、Ink `#1F2A25`、Muted `#6A756E`、Border `#CAD2C6`、Accent `#2E5F4B`、Secondary `#DCE4D9`、DangerBg `#F0D9D3`、DangerText `#8C3B2D`、NoteFill `#F5E9A9`、NoteInk `#2B2F27`、Pill `#F6F7F3`）。
- **数据**：`NoteStore` 单例，`files/notes.json` 一个 JSON 数组（id / text / createdAt / updatedAt / collapsed；旧文件没有 `collapsed` 键时读作 false）。每次变更在 `Mutex` 内先更新内存再于 IO 线程写盘：写 `notes.json.tmp` 后 `rename`（失败回退直接覆盖）；`create(id, text)` 幂等（同 id 视为更新），存在性与容量检查都在锁内；提交段 `NonCancellable`，保证 `dirty` / `unsavedIds` 总能反映真实结果；写盘失败 → `dirty` + `unsavedIds` → 主窗口标题栏出现「!」（点击重试）、列表行出现 danger 圆点；便签窗口本身不显示任何提示。启动时按 `updatedAt` 倒序加载、新建置顶（编辑在当次会话内不重排，但会更新 `updatedAt`，重启后按 `updatedAt` 重新排序；`setCollapsed` 只改收起态、不碰 `updatedAt`，收起 / 展开在任何时候都不改变顺序）。`NoteStore.scope`（`SupervisorJob` + IO）承接必须在窗口关闭后仍完成的写入（✓ 后立即关窗、失焦自动保存）。
- **限制**：单条 **200 个码点**（`codePointCount`，中文与单码点 emoji 计 1、组合 emoji 按码点数计，输入时自动截断）；最多 **30 条**（主窗口「+」与 `NoteStore.create` 双重把关）；空白不能 ✓。
- **评审修正**（2026-09-14，三视角对抗式代码评审，均已在模拟器复验）：草稿窗口在「+」后立即自行关闭（现在只有真实失焦才结束编辑）、删除已打开的便签后被 dispose 写回复活（`deleted` / `wasPersisted` 标志）、`NoteStore.create` 的检查与写入不在同一把锁内（改为锁内幂等）、写盘中途被取消丢失 `dirty` 标志（`NonCancellable` 提交）、便签侧的「!」重试先并入工具条、后按用户要求整个去掉（重试只在主窗口标题栏）、删除对话框 480 dp 超出 400 dp 窗口（改 `Orientation.Vertical` 360 dp）、`ImeAction.Done` 吞掉换行、编辑框自动聚焦（按用户要求撤回：不自动聚焦，点 ✎ 不再弹出系统键盘按钮）、合成时间戳标题与死代码清理。
- **入口**：`SpatialApplication.onCreate` → `NoteStore.init` → `launch(::mainApp)`；`LaunchActivity` 继承 `SpatialLaunchActivity`。

## 下载

- **APK**：见 GitHub Releases `v1.3.0`（`SpatialStickyNotes-debug.apk`，安装到 PICO Emulator 6.1 / PICO OS 6 设备：`pico-cli app install SpatialStickyNotes-debug.apk` → `pico-cli app launch com.example.spatialstickynotes`）。
- **模拟器截图与验证报告**：[`deliverables/SpatialStickyNotes_模拟器截图与验证.xlsx`](deliverables/SpatialStickyNotes_模拟器截图与验证.xlsx)（应用信息、PRD 功能覆盖、逐场景截图与验证结论）；单张截图（JPEG 1600 px，均为机位 1、adb 驱动；v1.3 这套 10 张替换了 v1.2 的 15 张）在 [`deliverables/screenshots/`](deliverables/screenshots/)：
  `01_home_list` 主窗口列表（4 条） · `02_note_selected` 单行便签：卡片按内容 1 行高，下方 ✎ / 🗑 / ⤡，窗口缩到内容高度（系统标题栏紧贴下方） · `03_note_editing` 点卡片进入编辑：编辑器最少 4 行，窗口随之变高，下方 ✓ · `04_note_done` ✓ 完成，回到 1 行卡片 · `05_note_folded` ⤡ 收起：96 dp 小标记（带展开角标） · `06_note_unfolded` 点标记展开 · `07_note_two_lines` 两行便签：卡片按 2 行高 · `08_delete_dialog` 小便签上的删除确认（窗口临时放大到 400 dp 容纳对话框） · `09_folded_after_restart` 重启后打开仍是收起态（持久化） · `10_draft_new` 「+」新建草稿（编辑态，无 ⤡）
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

本应用没有单独的调试广播通道，所有操作都走真实 UI（验证环境：PICO Emulator 6.1，AVD `Pico_Emulator_6_1`，PICO OS 6.1，2160 × 2158，pico-cli 0.5.0）。模拟器不支持脚本化的空间射线输入，测试时用 `--layerid` 把点击投到当前焦点窗口的表面上——坐标是该窗口自身表面内的坐标，不是整屏坐标；便签窗口的表面是 800 × H px（400 dp 宽，高度随内容变化，原点不变，所以同一种内容高度下坐标固定），主窗口另有自己的表面：

```bash
adb shell input tap <x> <y> --layerid       # 点击焦点窗口内的图标（+ / ✎ / ✓ / 🗑 / ! …）或卡片
adb shell input text "Hello"                # 键入（仅 ASCII）；编辑器不自动聚焦，要先 tap 一下 TextArea 让它拿到光标
adb logcat -s NoteStore                     # 写盘失败日志
```

写盘失败场景用 `adb shell run-as com.example.spatialstickynotes chmod` 把 `files/` 目录设为只读来触发，恢复后点主窗口标题栏的「!」重试。键入前先 tap TextArea 取得光标，此时卡片旁出现的圆形键盘按钮来自 PICO OS 输入法（`com.bytedance.pico.inputmethod`），不属于 App。拖动便签窗口仍需在模拟器里用鼠标拖系统标题栏。便签窗口表面上的 v1.3 点击坐标（px）：卡片右半部 (680, 180)；编辑中的 ✓ (400, 484)；1 行便签的工具条 ✎ (254, 304) / 🗑 (400, 304) / ⤡ (546, 304)，2 行便签的工具条 y = 364；收起后的标记 (400, 240)；删除对话框 ✕ (277, 550) / 🗑 (565, 550)。**仅在模拟器上观察到**：投到便签窗口左上约 300 × 104 dp 区域的 `--layerid` 点击会被吞掉（推测是系统标题栏的输入窗口），所以脚本都点卡片的右半部；头显上未复现、未验证。

v1.3 构建已在模拟器上验证：打开便签 → 自适应的 1 行卡片 + 下方 ✎ / 🗑 / ⤡，窗口缩到 194 dp；点卡片 → 编辑，窗口长到 284 dp，✓ 返回卡片；⤡ → 标记，窗口 180 dp，`notes.json` 里 `collapsed: true`；点标记 → 卡片，`collapsed: false`；重启 → 收起的便签仍以收起态打开；2 行便签 → 更高的卡片（窗口 220 dp）。模拟器上无法验证的部分：真机整体表现（2026-09-14 头显未连接）与真机键盘弹出行为。

## 目录

- `app/src/main/java/com/example/spatialstickynotes/`
  - `Main.kt` 入口 DSL（两个 WindowContainer；便签窗口的 `windowConstraints` 随内容高度更新）· `platform/` Application 与 LaunchActivity · `content/` `HomeWindow` 主窗口 / `NoteWindow` 便签窗口（含内容高度测量与 `EditingCard`）/ `Windows` 常量与 bundle · `ui/` `Components`（IconBtn、ActionStrip、StripDivider、StickyCard、StickyMarker、CounterRing、DeleteConfirmDialog）与 `Tokens` 颜色与尺寸 token（`NoteCardMinHeight / NoteCardMaxHeight / NoteEditorMinHeight`、`noteTextStyle`）· `data/` `Note` 模型（含 `collapsed`）、`NoteStore` 持久化、`NoteSession` 运行时状态（已打开 / 草稿窗口）；`lod/` 包已随距离 LOD 一起删除
- `app/src/main/res/drawable/ic_*.xml` 24 dp 描边图标
- `deliverables/` 模拟器截图与验证表（Excel）、截图，以及 `archive-v1.1/` 旧版素材
