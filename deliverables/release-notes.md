## SpatialStickyNotes v1.0.0 · 空间便利贴首版

PICO Spatial SDK 6.1.9 · Shared Space 平面窗口（不开 Stage、不申请 MR 权限）· PICO Spatial UI

**包含**
- `SpatialStickyNotes-debug.apk`：安装到 PICO Emulator 6.1 或 PICO OS 6 设备（`pico-cli app install SpatialStickyNotes-debug.apk` → `pico-cli app launch com.example.spatialstickynotes`）。

**功能**
- 主窗口便签列表：首次使用引导、摘要与状态行、待放置计数、每条可 放置 / 放回空间 / 查看、编辑、删除
- 560 × 640 dp 编辑窗口：黄色输入区、实时计数（200 字上限，按 Unicode 码点计数：中文与单码点 emoji 计 1，超出自动截断）、空白不可提交、取消 / 下一步·放置 / 保存
- 每条便签一个独立平面窗口（无玻璃背景、WorldScale.Dynamic）：拖动系统标题栏放到墙面或桌面后点「固定在这里」；「移动」回到待放置态可重新固定
- 远 / 近两档 LOD 并带迟滞（≤1.5 m 近处全文可滚动、≥1.8 m 远处仅显示 ≡ 标记），「展开 / 收起」可手动覆盖；距离由 dp→米换算系数估算，以开窗 1.75 m 为基准
- 已固定便签聚焦时显示 编辑 / 移动 / 删除 工具条；删除需一次确认（列表与便签窗口均可），确认后关闭便签窗口与其编辑窗口
- 本地 JSON 持久化（临时文件 + 重命名原子写入）；保存失败以 Snackbar 提示并可「重试保存」；冷启动恢复文字，已放置便签标记为「上次位置未恢复 · 待重新放置」可一键放回空间
- 容量上限 30 条，达上限提示先删除旧便签

**验证**
- 模拟器截图与 PRD 功能覆盖：仓库 `deliverables/SpatialStickyNotes_模拟器截图与验证.xlsx`（17 个场景，含 F01–F10 功能覆盖表）
- LOD 距离为估算值，未随相机移动验证；平面吸附由 PICO OS 窗口系统负责；30 条上限提示未触发。以上仍需在 PICO 设备上实测
