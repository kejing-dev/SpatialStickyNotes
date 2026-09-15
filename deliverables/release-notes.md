## SpatialStickyNotes v1.2.0 · 图标化 UI 与英文对话框

PICO Spatial SDK 6.1.9 · Shared Space 平面窗口（不开 Stage、不申请 MR 权限）· PICO Spatial UI · 按已批准的 Figma 设计（页面「01 用户路径」09 / 09b / 10 / 11）重做

**包含**
- `SpatialStickyNotes-debug.apk`（versionName 1.2 / versionCode 3，应用名 "Spatial Sticky Notes"）：安装到 PICO Emulator 6.1 或 PICO OS 6 设备（`pico-cli app install SpatialStickyNotes-debug.apk` → `pico-cli app launch com.example.spatialstickynotes`）。

**变化 · v1.2 相比 v1.0**
- 整个 UI 图标化：不再有文字按钮、状态胶囊（StatusPill）与 Snackbar；唯一的界面文字是删除对话框的英文标题 "Delete"；对话框按钮同样是纯图标（✕ 取消 / 🗑 删除）。便签正文不限语言，测试数据为中文。
- 去掉放置状态机（待放置 / 固定在这里 / 移动 / 放回空间）：Shared Space 下便签窗口的位置由 PICO OS 窗口系统管理，拖动系统标题栏即可移动，App 不记录、不恢复位置，JSON 不再写 `placement`（读取旧文件时忽略该字段，`files/notes.json` 沿用）。
- 去掉 560 × 640 dp 编辑窗口：新建与编辑都在便签窗口内行内完成；n/200 文字计数改为计数环，「重试保存」文字改为「!」图标，远处 72 dp ≡ 标记改为 96 dp 折角迷你便签。
- Planar 窗口容器由 3 个减为 2 个（主窗口 + 每条便签一个窗口）；`NoteEditorWindow.kt` 删除，Kotlin 从 15 个文件 1,271 行减到 12 个文件 1,032 行。

**功能**
- 主窗口（960 × 600 dp）：标题栏左侧应用图标；右侧圆形「+」外圈为容量环（n/30，满 30 条时置灰禁用、环变红），写盘失败时出现红色「!」（点按重试）。列表行 = 40 dp 黄色色块（右上绿点 = 便签窗口已打开，右下红点 = 未保存）+ 单行摘要（首个非空行，最多 60 码点）+ 🗑；点行打开 / 聚焦该便签窗口。空列表显示便签图形 + 大「+」；面板铺近白色底（Canvas 85%），不随环境变灰。
- 便签窗口（固定 400 × 400 dp，每条一个，无玻璃背景，WorldScale.Dynamic）：328 × 264 dp 黄色卡片靠上放置（窗口内容区 12 dp 边距 + 卡片 24 dp 上边距，卡片顶距窗口上沿 36 dp），工具条在卡片下方 12 dp 处居中，系统标题栏在窗口之外。「选中 = 系统窗口聚焦」：点便签即聚焦，出现系统标题栏（拖动即移动）、2 dp 描边与卡片正下方的胶囊工具条 ✎ | 🗑（远处手动展开时追加 ⤡ 收起），不遮挡便签文字；便签上没有「!」，写盘失败只在主窗口提示
- 行内编辑：点 ✎ 或再次点已选中的卡片 → 卡片变为 PICO TextArea（不自动聚焦，点入文本才出现光标；Enter 换行），右下角 20 dp 计数环（200 码点上限，中文与单码点 emoji 计 1，超出即时截断），卡片下方工具条只剩 ✓（空白时禁用）；点入文本后卡片旁的圆形键盘按钮是 PICO OS 输入法（com.bytedance.pico.inputmethod）的系统组件，用于唤起系统键盘，非 App 元素。输入时 600 ms 防抖自动保存，✓ 或窗口失焦结束编辑，没有取消
- 新建：「+」打开草稿窗口并直接进入编辑（同一时间只有一个草稿，再按「+」回到该草稿；草稿计入 30 条上限）；✓ 创建便签（id = 窗口 tag，窗口身份不变）；空白草稿在 ✓ / 失焦时丢弃
- 删除：工具条或列表行 🗑 → PICO AlertDialog（竖排 360 dp，能放进 400 dp 便签窗口）：标题 "Delete"，300 dp 黄色预览（最多 3 行），纯图标按钮 ✕（取消）/ 🗑（删除，danger）；确认后删除并关闭该便签窗口（两侧入口一致），`deleted` 标记保证不会被写回
- 远 / 近两档 LOD 并带迟滞（≥ 1.8 m 远处显示 96 dp 折角标记，一点展开；≤ 1.5 m 近处全文可滚动），聚焦或编辑中冻结；距离仍由 dp→米换算系数估算，以开窗 1.75 m 为基准
- 每个便签窗口的系统标题 = 便签前 ≤ 12 码点（Activity title + TaskDescription），多窗口可区分；空白草稿显示应用名
- 本地 JSON 持久化（临时文件 + 重命名原子写入、Mutex、提交段不可取消）；失败 → 主窗口标题栏出现「!」（点按重试）、列表行出现红点，便签窗口本身不提示；`create` 幂等（同 id 视为更新），存在性 / 容量检查在锁内完成
- 卡片与远处标记带 PICO spatialHoverEffect 与默认按压反馈；24 dp 描边矢量图标 ic_{edit, delete, check, close, expand, collapse, plus, error, note}

**修复**（2026-09-14 三视角对抗式代码评审，全部在模拟器上复验）
- 「+」后草稿窗口立即自关（失焦效果在初始 isFocused=false 时触发）→ 只有真正的失焦才结束编辑
- 删除已打开窗口的便签后被 dispose 时写回「复活」→ `deleted` / `wasPersisted` 标记，dispose 只冲刷进行中的编辑
- `NoteStore.create` 在锁外 check-then-act 可能产生重复 id → 检查移入锁内并幂等
- 写盘途中协程被取消会丢失 dirty 标记 → 提交段 NonCancellable
- 便签侧「!」重试先移入工具条，后按用户要求整个去掉 → 重试只在主窗口标题栏（列表行红点标记未保存）
- 删除对话框 480 dp 放不进 400 dp 窗口 → Orientation.Vertical（360 dp）
- ImeAction.Done 吞掉换行 → 默认键盘选项；编辑器自动聚焦 → 按用户要求撤回，不自动聚焦（点 ✎ 不弹出系统键盘按钮）；去掉合成时间戳标题；清理死代码（Panel、stripOffsetX、ic_retry、unusedColor）

**验证**
- PICO Emulator 6.1（AVD Pico_Emulator_6_1，PICO OS 6.1，2160×2158）· macOS · pico-cli 0.5.0；adb `input tap x y --layerid` 在聚焦窗口自身 surface 上点击，`input text` 键入 ASCII
- 已验证流程：主窗口列表；打开便签 → 聚焦态；✎ → 行内编辑 → 键入 → ✓ 持久化；「+」→ 草稿窗口保持编辑态 → 键入 → ✓ 创建；工具条 🗑 → 英文对话框 → Delete 删除、关窗、不再出现；列表行 🗑 删除已打开的便签 → 主窗口对话框 → Delete 同时关窗；写盘失败（主窗口标题栏「!」+ 列表行红点）→ 点「!」重试（run-as chmod 将 files 目录设为只读）
- 模拟器截图与 PRD 功能覆盖：仓库 `deliverables/SpatialStickyNotes_模拟器截图与验证.xlsx`（15 个场景，含 F01–F10 功能覆盖表）；v1.0 / v1.1 的截图与 Figma 导入件移至 `deliverables/archive-v1.1/`

**已知限制**
- 远 / 近 LOD 切换未验证：模拟器相机无法从 adb 移动，距离为估算值，首个采样直接决定远 / 近
- 真机未测：2026-09-14 PICO 头显未连接；设备上的软键盘行为未验证（点入文本后出现的圆形键盘按钮是 PICO OS 输入法组件，App 无法移除，只做到不在点 ✎ 时触发）
- 30 条上限未触发验证；便签窗口位置由 PICO OS 管理，App 不保存也不恢复位置，冷启动只恢复文字，便签窗口从列表重新打开
- 界面文字仅英文（用户决定：UI 需要文字的都英文，便签内容支持中文）；截图为 JPEG 1600 px 压缩版，单一机位
