#!/bin/bash

set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PREFLIGHT_SCRIPT="${SCRIPT_DIR}/open-finance-analytics-preflight.js"
readonly INDEX_SCRIPT="${SCRIPT_DIR}/open-finance-analytics-indexes.js"

readonly MONGO_URI="${MONGO_URI:-mongodb://localhost:27017}"
readonly MONGO_DB="${MONGO_DB:-open_finance}"

APPLY_CHANGES="false"

usage() {
    cat <<EOF
Usage: $(basename "$0") [--apply]

Environment variables:
  MONGO_URI   MongoDB connection string (default: mongodb://localhost:27017)
  MONGO_DB    Target database name (default: open_finance)

Modes:
  (default)   Run preflight checks only
  --apply     Run preflight checks and apply indexes if checks pass
EOF
}

for arg in "$@"; do
    case "$arg" in
        --apply)
            APPLY_CHANGES="true"
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $arg" >&2
            usage
            exit 1
            ;;
    esac
done

if ! command -v mongosh >/dev/null 2>&1; then
    echo "mongosh is required but was not found in PATH." >&2
    exit 1
fi

run_mongo_script() {
    local script_file="$1"
    if ! mongosh "$MONGO_URI" \
        --quiet \
        --eval "db = db.getSiblingDB('$MONGO_DB')" \
        --file "$script_file"; then
        echo "Failed to execute ${script_file} on ${MONGO_URI} (db=${MONGO_DB})." >&2
        echo "Verify MongoDB is running and connection settings are correct." >&2
        return 1
    fi
}

echo "Running MongoDB preflight checks for Open Finance analytics..."
run_mongo_script "$PREFLIGHT_SCRIPT"

if [[ "$APPLY_CHANGES" == "true" ]]; then
    echo "Preflight passed. Applying indexes..."
    run_mongo_script "$INDEX_SCRIPT"
    echo "MongoDB analytics migration completed."
else
    echo "Preflight passed. No changes applied (dry-run mode)."
    echo "Re-run with --apply to create indexes."
fi
