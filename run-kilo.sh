#!/bin/bash
# Kilo Code launcher with Kimi Code API

export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"

# Load project-specific environment variables
if [ -f ".env" ]; then
    set -a
    [ -f ".env" ] && . .env
    set +a
fi

# Set Kilo Code environment for Kimi
export ENABLE_TOOL_SEARCH="${ENABLE_TOOL_SEARCH:-false}"
export ANTHROPIC_BASE_URL="${ANTHROPIC_BASE_URL:-https://api.kimi.com/coding/}"
export ANTHROPIC_API_KEY="${ANTHROPIC_API_KEY:-}"

# Run Kilo Code with all passed arguments
kilo "$@"
