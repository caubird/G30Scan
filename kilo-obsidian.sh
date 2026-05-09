#!/bin/bash
################################################################################
# Kilo Code with Obsidian Knowledge Base Launcher
# 
# This script:
# 1. Loads nvm (Node.js)
# 2. Sets up environment variables (Kimi API + Obsidian vault)
# 3. Validates plugin and MCP server presence
# 4. Starts Kilo Code with full integration
################################################################################

set -e  # Exit on error

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  Kilo Code + Obsidian Knowledge Base Integration${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"

# Load nvm
export NVM_DIR="$HOME/.nvm"
if [ -s "$NVM_DIR/nvm.sh" ]; then
  . "$NVM_DIR/nvm.sh"
  echo "✓ Node.js loaded: $(node -v)"
else
  echo "⚠ Warning: nvm not found. Kilo may not be available."
fi

# Project directory
PROJECT_DIR="/run/media/lijian/Data/test/Android/G30Scanner"
cd "$PROJECT_DIR"

# Environment from .env
if [ -f ".env" ]; then
  set -a
  source .env
  set +a
  echo "✓ Environment loaded from .env"
fi

# Ensure required variables
: "${ANTHROPIC_API_KEY:?Need ANTHROPIC_API_KEY set}"
: "${ANTHROPIC_BASE_URL:?Need ANTHROPIC_BASE_URL set}"
: "${OBSIDIAN_VAULT:=/run/media/lijian/Data/work/知识库/知识库}"

export ENABLE_TOOL_SEARCH="${ENABLE_TOOL_SEARCH:-false}"
export ANTHROPIC_API_KEY
export ANTHROPIC_BASE_URL
export OBSIDIAN_VAULT

  # Verify vault exists
  if [ ! -d "$OBSIDIAN_VAULT" ]; then
    echo -e "${YELLOW}⚠ Warning: Obsidian vault not found at $OBSIDIAN_VAULT${NC}"
    echo "  Knowledge base features will be disabled."
  else
    echo "✓ Obsidian vault: $OBSIDIAN_VAULT"
    echo "  Notes: $(find "$OBSIDIAN_VAULT" -name '*.md' 2>/dev/null | wc -l) markdown files"
  fi

  # Verify Python MCP import
  if python3 -c "import mcp" 2>/dev/null; then
    echo "✓ MCP SDK available"
  else
    echo -e "${YELLOW}⚠ MCP SDK not found - MCP tools disabled${NC}"
    echo "  Install with: pip install mcp --break-system-packages"
  fi

# Verify MCP server script
MCP_SCRIPT="$PROJECT_DIR/.kilo/mcp-servers/obsidian-knowledge.py"
if [ -f "$MCP_SCRIPT" ]; then
  echo "✓ MCP server: $MCP_SCRIPT"
else
  echo "✗ MCP server missing: $MCP_SCRIPT"
fi

# Verify plugin
PLUGIN="$PROJECT_DIR/.kilo/plugins/obsidian-knowledge.js"
if [ -f "$PLUGIN" ]; then
  echo "✓ Kilo plugin: $PLUGIN"
else
  echo "✗ Plugin missing: $PLUGIN"
fi

# Display session info
echo ""
echo -e "${BLUE}Starting Kilo Code...${NC}"
echo -e "  Model: ${YELLOW}kimi-for-coding${NC}"
echo -e "  Vault: ${YELLOW}$OBSIDIAN_VAULT${NC}"
echo -e "  Tools: search_notes, log_to_obsidian, vault_info"
echo ""
echo -e "${YELLOW}Quick Commands:${NC}"
echo "  /status       Verify MCP and plugin are loaded"
echo "  vault_info    Check vault connection"
echo "  search_notes  Query knowledge base"
echo "  log_to_obsidian <title> Write a note"
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""

# Launch Kilo
exec kilo "$@"
