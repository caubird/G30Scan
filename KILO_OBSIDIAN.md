# Kilo + Obsidian Integration - Quick Reference

## Setup Complete

Your Kilo Code installation is now connected to your Obsidian knowledge vault at:
`/run/media/lijian/Data/work/知识库/知识库`

### Files Created

```
.kilo/
├── mcp-servers/
│   ├── obsidian-knowledge.py        # MCP server for vault access
│   └── requirements.txt
├── plugins/
│   └── obsidian-knowledge.js        # Kilo plugin with lifecycle hooks
└── skills/
    └── log-task-results/
        └── SKILL.md                 # Instructions for structured logging

kilo.json                            # Main configuration
.env                                 # Environment (API key, vault path)
kilo-obsidian.sh                     # Launcher script
AGENTS.md                            # Agent guidelines
```

## Usage

### Start Kilo with Obsidian Integration

```bash
cd /run/media/lijian/Data/test/Android/G30Scanner
./kilo-obsidian.sh
```

Or manually (after `source ~/.bashrc`):
```bash
kilo
```

### Pre-Task: Automatic Knowledge Retrieval

When you start a new task, the plugin **automatically** searches your vault for relevant notes and injects them into the conversation context. No action needed.

### During Task: Call Tools to Interact with Vault

Inside Kilo TUI, you can use:

```
/search_notes query="EMG fatigue detection"
```
→ Returns matching notes from your vault

```
/vault_info
```
→ Shows vault statistics

```
/log_to_obsidian title="Build Fix Summary" content="..." tags=["android","gradle"] taskType="coding"
```
→ Writes a structured note to `任务日志/`

Or type `/` and select from available tools.

### Post-Task: Structured Result Logging

**Option A: Ask agent directly**
> Please log this task to the knowledge base with title "G30Scanner Build Fix" and summarize what we did.

**Option B: Use the logging skill**
The agent knows the `log-task-results` skill and will automatically use it when appropriate. Just complete your task and say "that's all" or "done".

**Option C: CLI hook**
```bash
kilo run "Fix the build" --on-task-completed 'log_to_obsidian title="Build Fixed" content="See session #123" tags=["build"] taskType="coding"'
```

### Verify Integration

Inside Kilo, run:
- `/vault_info` — confirms vault connection
- `/search_notes query="G30Scanner"` — tests retrieval
- `kilo models` — should list Kimi models

## Configuration

- **Provider**: Kimi Code via `ANTHROPIC_BASE_URL` and `ANTHROPIC_API_KEY`
- **Model**: `kimi-for-coding`
- **MCP server**: Python-based `obsidian-knowledge.py` (tools: `search_notes`, `append_task_log`, `get_vault_path`)
- **Plugin**: JavaScript plugin `obsidian-knowledge.js` (hooks + tools)
- **Skill**: `log-task-results` provides agent instructions for structured logging

All configuration lives in `kilo.json` in this directory.

## Knowledge Base Workflow

1. **Pre-task**: Plugin auto-searches vault → injects context
2. **Mid-task**: Tools `search_notes`, `vault_info` available
3. **Post-task**: Agent uses `log_to_obsidian` tool (guided by skill) to store results in `任务日志/`
4. **Future tasks**: Previous logs are automatically retrieved as relevant context

## Notes

- Vault is indexed on-demand (simple keyword matching). For large vaults, consider installing `obsidian-notes-rag` (vector search).
- Session logs stored in `~/.local/share/kilo/kilo.db`
- All task logs saved under `<vault>/任务日志/` with date prefix
