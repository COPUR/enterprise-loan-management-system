
import os
import re
import shutil

def rename_package_and_imports(root_src_dir, root_dest_dir):
    # Ensure destination root directory exists
    os.makedirs(root_dest_dir, exist_ok=True)

    for dirpath, dirnames, filenames in os.walk(root_src_dir):
        # Construct corresponding destination directory path
        relative_path = os.path.relpath(dirpath, root_src_dir)
        current_dest_dir = os.path.join(root_dest_dir, relative_path)
        os.makedirs(current_dest_dir, exist_ok=True)

        for filename in filenames:
            if filename.endswith(".java"):
                src_filepath = os.path.join(dirpath, filename)
                dest_filepath = os.path.join(current_dest_dir, filename)

                with open(src_filepath, 'r') as f:
                    content = f.read()

                # Replace package declarations
                content = re.sub(r'package com\.banking', r'package com.bank', content)
                content = re.sub(r'import com\.banking', r'import com.bank', content)

                with open(dest_filepath, 'w') as f:
                    f.write(content)
                print(f"Processed and moved: {src_filepath} to {dest_filepath}")

    # Remove original source directory after all files are moved
    if os.path.exists(root_src_dir):
        shutil.rmtree(root_src_dir)
        print(f"Removed original directory: {root_src_dir}")

if __name__ == "__main__":
    base_path = "/Users/alicopur/Documents/GitHub/enterprise-loan-management-system"
    
    # Process main Java files
    rename_package_and_imports(
        os.path.join(base_path, "src/main/java/com/banking"),
        os.path.join(base_path, "src/main/java/com/bank")
    )
    
    # Process test Java files
    rename_package_and_imports(
        os.path.join(base_path, "src/test/java/com/banking"),
        os.path.join(base_path, "src/test/java/com/bank")
    )
