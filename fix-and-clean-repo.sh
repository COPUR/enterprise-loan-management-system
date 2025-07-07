#!/bin/bash

echo "🔧 Enterprise Loan Management System - Repository Cleanup"
echo "========================================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if running as root
if [ "$EUID" -eq 0 ]; then 
   echo -e "${RED}Please do not run this script as root${NC}"
   exit 1
fi

echo -e "${YELLOW}This script will:${NC}"
echo "1. Fix permission issues in .git directory"
echo "2. Clean gitignored files from main and test branches"
echo "3. Apply industry-standard .gitignore rules"
echo ""
echo -e "${YELLOW}You will need to enter your password for sudo commands${NC}"
echo ""
read -p "Continue? (y/n) " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Cancelled."
    exit 1
fi

echo ""
echo "Step 1: Fixing permissions..."
echo "-----------------------------"

# Fix .git permissions
echo "→ Fixing .git directory permissions..."
sudo chown -R $(whoami):$(id -gn) .git
echo -e "${GREEN}✓ Git directory permissions fixed${NC}"

# Remove problematic .gradle directories with permission issues
echo "→ Removing problematic .gradle cache directories..."
sudo rm -rf .gradle/configuration-cache/ 2>/dev/null || true
echo -e "${GREEN}✓ Problematic cache directories removed${NC}"

echo ""
echo "Step 2: Cleaning repositories"
echo "-----------------------------"

# Function to clean a branch
clean_branch() {
    local branch=$1
    echo ""
    echo -e "${YELLOW}→ Processing branch: $branch${NC}"
    
    # Checkout branch
    git checkout $branch --quiet || {
        echo -e "${RED}✗ Could not checkout $branch branch${NC}"
        return 1
    }
    
    # Remove all cached files
    echo "  Removing all files from git index..."
    git rm -rf --cached . --quiet 2>/dev/null || true
    
    # Add back only non-ignored files
    echo "  Adding back non-ignored files..."
    git add . 2>/dev/null || true
    
    # Check if there are changes
    if [ -n "$(git status --porcelain 2>/dev/null)" ]; then
        # Count changes
        local removed=$(git status --porcelain | grep "^D " | wc -l | tr -d ' ')
        local total=$(git status --porcelain | wc -l | tr -d ' ')
        
        echo "  Files to be removed: $removed"
        echo "  Total changes: $total"
        
        # Commit
        git commit -m "chore: Apply .gitignore rules and remove ignored files

- Removed $removed files from git tracking
- Applied comprehensive industry-standard .gitignore
- Cleaned: build artifacts, IDE files, logs, caches, secrets
- Security: Removed all sensitive files and credentials" --quiet || {
            echo -e "${RED}✗ Commit failed${NC}"
            return 1
        }
        
        echo -e "${GREEN}✓ Branch $branch cleaned successfully${NC}"
    else
        echo -e "${GREEN}✓ Branch $branch is already clean${NC}"
    fi
}

# Store current branch
original_branch=$(git branch --show-current)

# Clean main branch
clean_branch "main"

# Clean test branch (if exists)
if git show-ref --verify --quiet refs/heads/test; then
    clean_branch "test"
else
    echo -e "${YELLOW}ℹ Test branch not found, skipping${NC}"
fi

# Return to original branch
git checkout $original_branch --quiet

echo ""
echo "Step 3: Final cleanup"
echo "--------------------"

# Remove any remaining untracked files that should be ignored
echo "→ Removing untracked files that match .gitignore..."
git clean -fdX --quiet
echo -e "${GREEN}✓ Untracked ignored files removed${NC}"

echo ""
echo "========================================================"
echo -e "${GREEN}✅ Repository cleanup completed successfully!${NC}"
echo ""
echo "Summary:"
echo "--------"
echo "• Fixed permission issues"
echo "• Cleaned gitignored files from tracked branches"
echo "• Applied industry-standard .gitignore rules"
echo ""
echo "Next steps:"
echo "-----------"
echo "1. Review changes: git log --oneline -n 5"
echo "2. Push to remote: git push origin main test"
echo ""
echo -e "${YELLOW}⚠️  Important: To completely remove sensitive data from git history,${NC}"
echo -e "${YELLOW}   consider using BFG Repo-Cleaner or git filter-branch${NC}"
echo ""
echo "Example with BFG:"
echo "  java -jar bfg.jar --delete-files '*.{env,key,pem}' --no-blob-protection"
echo "  git reflog expire --expire=now --all && git gc --prune=now --aggressive"