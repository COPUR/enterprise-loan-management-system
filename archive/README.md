# Archive Directory

This directory contains files migrated from the root directory during repository cleanup.

## Directory Structure

- `backup-code/` - Legacy backup source code and compiled files
- `logs/` - Historical application log files
- `temp-files/` - Temporary files and downloads
- `alternative-builds/` - Alternative build configurations not actively used

## Migration Date
Files migrated on: 2025-06-24

## Files Migrated

### Phase 1 - Low Risk Files
- `backup-src/` → `backup-code/`
- Log files (*.log) → `logs/`
- `spring-boot-cli.tar.gz` → `temp-files/`
- `pom-*.xml` → `alternative-builds/`
- `uv.lock` → `temp-files/`

These files had minimal or no references in the main codebase documentation.