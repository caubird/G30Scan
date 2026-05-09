# Agents

This file contains instructions for Kilo agents working in this project.

## General Guidelines
- Follow the existing code style and patterns in this project
- Ensure all changes are compatible with the Android/Java/Node.js stack
- Test changes when possible before committing

## 知识库工作流（默认规则）

### 任务开始前
1. **查询知识库**：在开始执行新任务前，先搜索 Obsidian 知识库中的历史任务记录
   - 知识库路径：`/run/media/lijian/Data/work/知识库/知识库`
   - 任务日志目录：`/run/media/知识库/知识库/任务日志/`
   - 索引文件：`[[任务日志/索引]]`
2. **学习历史经验**：阅读相关的历史任务笔记，了解：
   - 之前类似任务是如何解决的
   - 遇到过哪些坑和解决方案
   - 有哪些环境配置、命令、脚本可以直接复用
3. **避免重复犯错**：如果历史记录中有失败/踩坑经验，优先采用已验证的成功方案

### 任务执行中
- 记录关键步骤、命令、配置文件修改
- 记录遇到的问题和解决方案
- 截图或保存关键输出日志

### 任务完成后
1. **写入知识库**：将本次任务的完整过程记录为 Obsidian 笔记
   - 文件命名格式：`YYYY-MM-DD_任务名.md`
   - 存放路径：`/run/media/lijian/Data/work/知识库/知识库/任务日志/`
   - 内容模板：任务概述 → 执行过程 → 关键命令/配置 → 经验教训 → 相关文件
2. **更新索引**：在 `任务日志/索引.md` 中添加新任务条目
3. **建立双向链接**：在笔记中使用 Obsidian 维基链接 `[[...]]` 关联相关历史任务
