## SpatialStickyNotes v1.3.0 · 手动收起 + 卡片自适应高度

PICO Spatial SDK 6.1.9 · Shared Space 平面窗口（不开 Stage、不申请 MR 权限）· PICO Spatial UI · 按已批准的 Figma 设计（页面「01 用户路径」第 13 节「便签窗口 v1.3 · 手动收起 + 自适应高度」；其余沿用 09 / 09b / 10 / 11）在 v1.2 基础上修改

**包含**
- `SpatialStickyNotes-debug.apk`（versionName 1.3 / versionCode 4，应用名 "Spatial Sticky Notes"）：安装到 PICO Emulator 6.1 或 PICO OS 6 设备（`pico-cli app install SpatialStickyNotes-debug.apk` → `pico-cli app launch com.example.spatialstickynotes`）。v1.2 的 `files/notes.json` 直接沿用，没有 `collapsed` 键的便签读作未收起。

**变化 · v1.3 相比 v1.2**
- 去掉按距离切换的远 / 近 LOD：v1.2 用 `WorldScale.Dynamic` 下 dp→米换算系数估算观看距离，≥ 1.8 m 折成标记、≤ 1.5 m 展开全文（`lod/ViewerDistance.kt` 的 `LodController` / `rememberViewerDistanceEstimate`，带迟滞，聚焦或编辑中冻结）。用户在头显上实测这一切换从未触发，v1.3 整个删除 `lod/` 包，不再有 1.5 / 1.8 m 阈值与迟滞；容器仍是 `WorldScale.Dynamic`，但没有任何代码再读换算系数。
- 改为手动收起：选中便签的工具条从 ✎ | 🗑 变为 ✎ | 🗑 | ⤡（`ic_collapse`，两个向内的 chevron，与标记上展开角标的 `ic_expand` 方向相反）。⤡ 把便签折成 96 dp 的 `StickyMarker`，点标记任意位置展开；收起状态按便签持久化到 `notes.json`（新增 `collapsed` 键），重启后保持。
- 卡片高度跟随文字：328 × 264 dp 的固定卡片改为 328 dp 宽、74 dp（1 行）到 254 dp（7 行）随行数增高，更长的便签在卡片内滚动；行内编辑器最少 4 行、随文字增高。
- 便签窗口跟随内容高度：不再固定 400 × 400 dp，宽仍 400 dp，高度由内容决定（1 行便签 194 dp、编辑 284 dp、收起 180 dp），可增可减；删除对话框打开期间临时放大到 400 dp。
- 卡片与标记的点击改回不带指示的普通 `clickable`（去掉 `spatialHoverEffect` 与默认按压反馈）：它们在 PICO OS 6.1 上吞掉了点击，再点卡片进入编辑与点标记展开都曾无声失效。
- Kotlin 从 12 个文件 1,034 行减到 11 个文件 1,014 行（删 81 行 LOD，净增 61 行窗口高度上报与自适应高度）。

**功能**
- 主窗口（960 × 600 dp）沿用 v1.2：标题栏左侧应用图标，右侧「+」容量环（n/30，满 30 条置灰禁用）与写盘失败时的红色「!」（点按重试）；列表行 = 40 dp 黄色色块（右上绿点 = 便签窗口已打开，右下红点 = 未保存）+ 单行摘要（首个非空行，最多 60 码点）+ 🗑，点行打开 / 聚焦该便签窗口；收起 / 展开不改 `updatedAt`，列表顺序不变。空列表显示便签图形 + 大「+」。
- 便签窗口（400 dp 宽、高度跟随内容，每条一个，无玻璃背景，`ContainerResizeType.ContentSize`，系统标题栏在窗口之外）：黄色 `StickyCard` 328 dp 宽、高度 = 行数 × 30 + 44 dp（正文 20 sp、30 dp 精确行距，`LineHeightStyle` Trim.None 让每行都是整 30 dp），1 行 74 dp、最多 7 行 254 dp，再长在卡片内滚动。窗口高度 = 24 + 卡片 + 12 + 60 dp 工具条槽位 + 24：槽位在未收起时始终保留，聚焦时窗口不跳动。`NoteWindow` 用 `Modifier.layout` 以无界高度测量内容并上报，`Main.kt` 把 `windowConstraints` 的 minHeight = maxHeight 钉在该高度（限 120–400 dp），SDK 随约束变化调整容器。模拟器实测：1 行 194 dp、2 行 220 dp、编辑 284 dp；收起请求 144 dp，被系统下限抬到 180 dp
- 「选中 = 系统窗口聚焦」：点便签即聚焦，出现系统标题栏（拖动即移动；App 不做放置 / 固定 / 移动）、2 dp 描边与卡片正下方的胶囊工具条 ✎ | 🗑 | ⤡，不遮挡便签文字；草稿（尚未保存的新便签）没有 ⤡；未聚焦只有卡片（或标记），便签上没有「!」，写盘失败只在主窗口提示
- 手动收起：⤡ → 96 dp 折角迷你便签（带强调色展开角标）居中在原卡片位置，没有工具条，窗口缩到 180 dp；点标记 → 恢复卡片与工具条。状态经 `NoteStore.setCollapsed` 写入 `notes.json` 的 `collapsed`，重启后仍是收起态；编辑态始终显示完整编辑器（即使存的是收起）
- 行内编辑：点 ✎ 或再次点已选中的卡片 → 卡片变为 PICO `TextArea`（不自动聚焦，点入文本才出现光标；Enter 换行），最少 4 行（164 dp）并随文字增高到 254 dp，窗口跟着变高；右下角 20 dp 计数环（200 码点上限，超出即时截断），工具条只剩 ✓（空白时禁用）；600 ms 防抖自动保存，✓ 或窗口失焦结束编辑并回到自适应高度的卡片，没有取消
- 新建 / 删除 / 持久化 / 窗口标题沿用 v1.2：「+」打开草稿窗口直接进入编辑（一次一个草稿，计入 30 条），✓ 创建（id = 窗口 tag），空白草稿丢弃；工具条或列表行 🗑 → 竖排 360 dp 的 PICO `AlertDialog`（标题 "Delete"、300 dp 黄色预览、✕ / 🗑 图标按钮），对话框期间便签窗口临时开到 400 dp 以免裁剪，确认后删除并关窗，`deleted` 标记保证不会写回；JSON 原子写入（临时文件 + 重命名、Mutex、提交段 NonCancellable），失败 → 主窗口标题栏「!」+ 列表行红点；每个便签窗口的系统标题 = 便签前 ≤ 12 码点
- 图标：24 dp 描边矢量 ic_{edit, delete, check, close, expand, collapse, plus, error, note}；卡片与标记不再有 hover / 按压反馈

**修复**
- 再点已选中的卡片进入编辑、点标记展开在 PICO OS 6.1 上无声失效（`PicoTheme` 默认 indication / `spatialHoverEffect` 拦住了点击）→ `StickyCard` / `StickyMarker` 改回 `clickable(interactionSource, indication = null)`
- 窗口只能缩小不能变大（`wrapContentHeight` 被窗口自身的最大高度夹住）→ 内容用 `Modifier.layout` 以 `Constraints.Infinity` 测量后上报真实高度，窗口可增可减
- 删除对话框在缩小后的便签窗口里被裁剪 → 对话框打开期间窗口临时开到 400 dp
- 聚焦时工具条出现让窗口高度跳变 → 60 dp 工具条槽位在未收起时常驻
- Compose（PICO 分支）默认裁掉首尾行的半行距，n 行量出 30n − 6 dp → `LineHeightStyle.Trim.None`，1 行 / 7 行的卡片高度精确

**验证**
- PICO Emulator 6.1（AVD Pico_Emulator_6_1，PICO OS 6.1，2160×2158）· macOS · pico-cli 0.5.0；adb `input tap x y --layerid` 在聚焦窗口自身 surface 上点击（便签窗口 surface 800 × 800 px，窗口缩放后 800 × H px、原点不变），`input text` 键入 ASCII；`run-as cat files/notes.json` 核对 `collapsed`
- 已验证流程（2026-09-14，v1.3 构建）：打开便签 → 1 行自适应卡片 + ✎ / 🗑 / ⤡ 工具条，窗口缩到 194 dp；点卡片 → 编辑，窗口长到 284 dp，✓ 回到 1 行卡片；⤡ → 标记 + 窗口 180 dp，`notes.json` 该条 `collapsed: true`；点标记 → 卡片，`collapsed: false`；重启 → 收起的便签仍以收起态打开；2 行便签 → 更高的卡片
- 模拟器截图与 PRD 功能覆盖：仓库 `deliverables/SpatialStickyNotes_模拟器截图与验证.xlsx`（10 个场景，含 F01–F10 功能覆盖表，F04 / 4.2 改为手动收起）；`deliverables/screenshots/` 换成 v1.3 的 10 张（01_home_list、02_note_selected、03_note_editing、04_note_done、05_note_folded、06_note_unfolded、07_note_two_lines、08_delete_dialog、09_folded_after_restart、10_draft_new），v1.2 的 15 张不再对应当前界面

**已知限制**
- 真机未测：2026-09-14 PICO 头显未连接；设备上的软键盘行为未验证（点入文本后出现的圆形键盘按钮是 PICO OS 输入法组件，App 无法移除，只做到不在点 ✎ 时触发）
- 仅在模拟器观察到：adb `input tap --layerid` 落在便签窗口左上约 300 × 104 dp 区域的点击被吞掉（疑为系统标题栏的输入窗口），测试脚本改点卡片右半部分；真机未复现也未验证
- 系统对窗口高度有约 180 dp 的下限：收起态请求 144 dp 被抬到 180 dp，标题栏在标记下方约 36 dp 处而非紧贴
- 卡片最多 7 行（254 dp），更长的便签在卡片内滚动，窗口不再增高；远处的便签不再自动折叠，只能手动 ⤡ 收起（`WorldScale.Dynamic` 仍开启但已无代码读取距离）
- 30 条上限未触发验证；便签窗口位置由 PICO OS 管理，App 不保存也不恢复位置，冷启动只恢复文字与收起状态，便签窗口从列表重新打开
- 界面文字仅英文（用户决定：UI 需要文字的都英文，便签内容支持中文）；截图为 JPEG 1600 px 压缩版，单一机位
