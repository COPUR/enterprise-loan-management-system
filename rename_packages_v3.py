
import os
import re
import shutil

def rename_package_and_imports(root_dir):
    for dirpath, dirnames, filenames in os.walk(root_dir):
        for filename in filenames:
            if filename.endswith(".java"):
                filepath = os.path.join(dirpath, filename)
                with open(filepath, 'r') as f:
                    content = f.read()

                # Replace package declarations
                content = re.sub(r'package com\.banking', r'package com.bank', content)
                # Replace import statements
                content = re.sub(r'import com\.banking', r'import com.bank', content)

                with open(filepath, 'w') as f:
                    f.write(content)
                print(f"Processed: {filepath}")

    # Rename the top-level directory after all files are modified
    parent_dir = os.path.dirname(root_dir)
    new_root_dir = os.path.join(parent_dir, "bank")
    
    if os.path.exists(root_dir):
        shutil.move(root_dir, new_root_dir)
        print(f"Renamed directory: {root_dir} to {new_root_dir}")

if __name__ == "__main__":
    base_path = "/Users/alicopur/Documents/GitHub/enterprise-loan-management-system"
    
    # Process main Java files
    rename_package_and_imports(
        os.path.join(base_path, "src/main/java/com/banking")
    )
    
    # Process test Java files
    rename_package_and_imports(
        os.path.join(base_path, "src/test/java/com/banking")
    )
