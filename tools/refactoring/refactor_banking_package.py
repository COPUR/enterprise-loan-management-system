import os
import re
import shutil

def refactor_package_and_move(root_dir):
    # First, update package and import statements in all .java files
    for dirpath, dirnames, filenames in os.walk(root_dir):
        for filename in filenames:
            if filename.endswith(".java"):
                filepath = os.path.join(dirpath, filename)
                with open(filepath, 'r') as f:
                    content = f.read()

                # Replace package declarations (more general regex)
                content = re.sub(r'package com\.(banking|bank\.banking)', r'package com.bank', content)
                # Replace import statements (more general regex)
                content = re.sub(r'import com\.(banking|bank\.banking)', r'import com.bank', content)

                with open(filepath, 'w') as f:
                    f.write(content)
                print(f"Processed: {filepath}")

    # Then, rename directories from deepest to shallowest
    # This ensures that parent directories are renamed after their children
    for dirpath, dirnames, filenames in os.walk(root_dir, topdown=False):
        if os.path.basename(dirpath) == "banking":
            new_path = os.path.join(os.path.dirname(dirpath), "bank")
            if os.path.exists(new_path):
                # If the target directory already exists, move contents
                for item in os.listdir(root):
                    shutil.move(os.path.join(root, item), new_path)
                os.rmdir(root)
                print(f"Moved contents and removed old directory: {root} to {new_path}")
            else:
                os.rename(root, new_path)
                print(f"Renamed directory: {root} to {new_path}")

if __name__ == "__main__":
    project_root = "/Users/alicopur/Documents/GitHub/enterprise-loan-management-system"

    # Refactor main Java files
    main_java_path = os.path.join(project_root, "src/main/java/com/banking")
    if os.path.exists(main_java_path):
        refactor_package_and_move(main_java_path)
    else:
        print(f"Main Java path not found: {main_java_path}")

    # Refactor test Java files
    test_java_path = os.path.join(project_root, "src/test/java/com/banking")
    if os.path.exists(test_java_path):
        refactor_package_and_move(test_java_path)
    else:
        print(f"Test Java path not found: {test_java_path}")