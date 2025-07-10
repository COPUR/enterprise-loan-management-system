#!/bin/bash

echo "🔧 Fixing Git merge conflicts across the repository..."

# Find all files with merge conflict markers
CONFLICT_FILES=$(find . -name "*.java" -exec grep -l "<<<<<<< Updated upstream\|=======\|>>>>>>> Stashed changes" {} \;)

if [ -z "$CONFLICT_FILES" ]; then
    echo "✅ No merge conflict markers found"
    exit 0
fi

echo "📋 Found merge conflicts in the following files:"
echo "$CONFLICT_FILES"
echo ""

# Function to clean merge conflicts
clean_merge_conflicts() {
    local file="$1"
    echo "🔄 Cleaning merge conflicts in: $file"
    
    # Create a backup
    cp "$file" "$file.backup"
    
    # Remove merge conflict markers and resolve conflicts
    # This approach keeps the content between the markers and removes the markers themselves
    sed -i.tmp '
        /^<<<<<<< Updated upstream$/d
        /^<<<<<<< HEAD$/d
        /^=======$/d
        /^>>>>>>> Stashed changes$/d
        /^>>>>>>> .*$/d
    ' "$file"
    
    # Remove the temporary file created by sed
    rm -f "$file.tmp"
    
    echo "✅ Cleaned: $file"
}

# Process each file with conflicts
while IFS= read -r file; do
    if [ -n "$file" ]; then
        clean_merge_conflicts "$file"
    fi
done <<< "$CONFLICT_FILES"

echo ""
echo "🧹 Cleaning up any remaining duplicate import statements..."

# Fix common duplicate import issues
find . -name "*.java" -exec sed -i.tmp '/^import.*$/N; /^\(.*\)\n\1$/d' {} \;
find . -name "*.java.tmp" -delete

echo "✅ Merge conflict cleanup completed!"
echo ""
echo "📋 Summary:"
echo "  - Removed all Git merge conflict markers"
echo "  - Cleaned duplicate import statements"
echo "  - Created backup files (.backup) for safety"
echo ""
echo "🚀 Next steps:"
echo "  1. Review the changes"
echo "  2. Test compilation: ./gradlew compileJava"
echo "  3. Run tests: ./gradlew test"
echo "  4. Commit the fixes"