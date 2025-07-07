
import os
import re
import shutil

def move_and_refactor(src_dir, dest_dir, old_pkg, new_pkg):
    if not os.path.exists(src_dir):
        print(f"Source directory {src_dir} does not exist. Skipping.")
        return

    for dirpath, _, filenames in os.walk(src_dir):
        for filename in filenames:
            if filename.endswith(".java"):
                src_filepath = os.path.join(dirpath, filename)
                
                relative_path = os.path.relpath(dirpath, src_dir)
                dest_filepath = os.path.join(dest_dir, relative_path, filename)
                
                os.makedirs(os.path.dirname(dest_filepath), exist_ok=True)

                with open(src_filepath, 'r') as f:
                    content = f.read()

                content = content.replace(old_pkg, new_pkg)

                with open(dest_filepath, 'w') as f:
                    f.write(content)
    
    shutil.rmtree(src_dir)
    print(f"Moved and refactored {src_dir} to {dest_dir}")

def main():
    base_path = "/Users/alicopur/Documents/GitHub/enterprise-loan-management-system/src"
    main_java_path = os.path.join(base_path, "main/java/com/bank")
    test_java_path = os.path.join(base_path, "test/java/com/bank")

    # 1. Consolidate loan packages
    move_and_refactor(
        os.path.join(main_java_path, "loan"),
        os.path.join(main_java_path, "loanmanagement/loan"),
        "com.bank.loan",
        "com.bank.loanmanagement.loan"
    )
    move_and_refactor(
        os.path.join(main_java_path, "loans"),
        os.path.join(main_java_path, "loanmanagement/loan"),
        "com.bank.loans",
        "com.bank.loanmanagement.loan"
    )
    move_and_refactor(
        os.path.join(test_java_path, "loan"),
        os.path.join(test_java_path, "loanmanagement/loan"),
        "com.bank.loan",
        "com.bank.loanmanagement.loan"
    )
    move_and_refactor(
        os.path.join(test_java_path, "loans"),
        os.path.join(test_java_path, "loanmanagement/loan"),
        "com.bank.loans",
        "com.bank.loanmanagement.loan"
    )

    # 2. Consolidate customer packages
    move_and_refactor(
        os.path.join(main_java_path, "loanmanagement/customermanagement"),
        os.path.join(main_java_path, "loanmanagement/customer"),
        "com.bank.loanmanagement.customermanagement",
        "com.bank.loanmanagement.customer"
    )
    move_and_refactor(
        os.path.join(test_java_path, "loanmanagement/customermanagement"),
        os.path.join(test_java_path, "loanmanagement/customer"),
        "com.bank.loanmanagement.customermanagement",
        "com.bank.loanmanagement.customer"
    )

    # 3. Restructure to bounded contexts
    bounded_contexts = ["customer", "payment", "risk"]
    for context in bounded_contexts:
        move_and_refactor(
            os.path.join(main_java_path, f"loanmanagement/{context}"),
            os.path.join(main_java_path, context),
            f"com.bank.loanmanagement.{context}",
            f"com.bank.{context}"
        )
        move_and_refactor(
            os.path.join(test_java_path, f"loanmanagement/{context}"),
            os.path.join(test_java_path, context),
            f"com.bank.loanmanagement.{context}",
            f"com.bank.{context}"
        )

    # 4. Rename loanmanagement to loan
    os.rename(os.path.join(main_java_path, "loanmanagement"), os.path.join(main_java_path, "loan"))
    os.rename(os.path.join(test_java_path, "loanmanagement"), os.path.join(test_java_path, "loan"))

    print("Refactoring complete.")

if __name__ == "__main__":
    main()
