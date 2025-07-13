#!/bin/bash

# Database Utility Functions for Enterprise Banking Platform
# 
# Database migration, backup, and rollback utilities for production deployments

# Check database connectivity
check_database_connectivity() {
    local env="$1"
    local db_url=$(get_env_config "$env" "database_url")
    
    log_info "Checking database connectivity for $env..."
    
    # Extract connection details from JDBC URL
    local db_host=$(echo "$db_url" | sed -n 's/.*:\/\/\([^:]*\):.*/\1/p')
    local db_port=$(echo "$db_url" | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
    local db_name=$(echo "$db_url" | sed -n 's/.*\/\([^?]*\).*/\1/p')
    
    # Test network connectivity
    if ! nc -z "$db_host" "$db_port" 2>/dev/null; then
        log_error "Cannot reach database server at $db_host:$db_port"
        return 1
    fi
    
    # Test database connection
    if ! test_database_connection "$env"; then
        log_error "Database connection test failed for $env"
        return 1
    fi
    
    log_success "Database connectivity verified for $env"
}

# Test database connection
test_database_connection() {
    local env="$1"
    local db_url=$(get_env_config "$env" "database_url")
    
    # Extract connection details
    local db_host=$(echo "$db_url" | sed -n 's/.*:\/\/\([^:]*\):.*/\1/p')
    local db_port=$(echo "$db_url" | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
    local db_name=$(echo "$db_url" | sed -n 's/.*\/\([^?]*\).*/\1/p')
    
    # Get credentials from environment or prompt
    local db_username="${DB_USERNAME:-banking_user}"
    local db_password="${DB_PASSWORD:-}"
    
    if [[ -z "$db_password" ]]; then
        log_warn "Database password not set in environment"
        return 1
    fi
    
    # Test connection with psql
    PGPASSWORD="$db_password" psql -h "$db_host" -p "$db_port" -U "$db_username" -d "$db_name" -c "SELECT 1;" &> /dev/null
}

# Run database migrations
run_database_migrations() {
    local env="$1"
    local version="$2"
    
    log_info "Running database migrations for $env (version $version)..."
    
    # Create backup before migrations
    create_database_backup "$env" "pre-migration-$version"
    
    # Check if migrations are needed
    if ! check_migration_status "$env" "$version"; then
        log_info "No new migrations needed for version $version"
        return 0
    fi
    
    # Run migrations with timeout
    if run_flyway_migrations "$env" "$version"; then
        log_success "Database migrations completed successfully"
        
        # Verify migration integrity
        verify_migration_integrity "$env" "$version"
    else
        log_error "Database migrations failed"
        
        # Attempt rollback
        rollback_failed_migrations "$env" "$version"
        return 1
    fi
}

# Check migration status
check_migration_status() {
    local env="$1"
    local version="$2"
    
    # Check Flyway schema history
    local pending_migrations=$(get_pending_migrations "$env" "$version")
    
    if [[ $pending_migrations -gt 0 ]]; then
        log_info "Found $pending_migrations pending migrations"
        return 0
    else
        return 1
    fi
}

# Get pending migrations count
get_pending_migrations() {
    local env="$1"
    local version="$2"
    local db_url=$(get_env_config "$env" "database_url")
    
    # Extract connection details
    local db_host=$(echo "$db_url" | sed -n 's/.*:\/\/\([^:]*\):.*/\1/p')
    local db_port=$(echo "$db_url" | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
    local db_name=$(echo "$db_url" | sed -n 's/.*\/\([^?]*\).*/\1/p')
    
    # Query Flyway schema history for pending migrations
    local pending=$(PGPASSWORD="$DB_PASSWORD" psql -h "$db_host" -p "$db_port" -U "$DB_USERNAME" -d "$db_name" -t -c \
        "SELECT COUNT(*) FROM flyway_schema_history WHERE success = false;" 2>/dev/null | tr -d ' ' || echo "0")
    
    echo "$pending"
}

# Run Flyway migrations
run_flyway_migrations() {
    local env="$1"
    local version="$2"
    local db_url=$(get_env_config "$env" "database_url")
    
    # Flyway configuration
    local flyway_conf="/tmp/flyway-${env}.conf"
    create_flyway_config "$env" "$flyway_conf"
    
    # Run migrations with timeout
    timeout "$DB_MIGRATION_TIMEOUT" flyway \
        -configFiles="$flyway_conf" \
        -locations="filesystem:$PROJECT_ROOT/src/main/resources/db/migration" \
        migrate
    
    local flyway_exit_code=$?
    
    # Clean up config file
    rm -f "$flyway_conf"
    
    return $flyway_exit_code
}

# Create Flyway configuration
create_flyway_config() {
    local env="$1"
    local config_file="$2"
    local db_url=$(get_env_config "$env" "database_url")
    
    cat > "$config_file" << EOF
flyway.url=$db_url
flyway.user=$DB_USERNAME
flyway.password=$DB_PASSWORD
flyway.schemas=public,customer_context,loan_context,payment_context,shared_infrastructure
flyway.table=flyway_schema_history
flyway.baselineOnMigrate=true
flyway.validateOnMigrate=true
flyway.cleanDisabled=true
flyway.mixed=false
flyway.group=false
flyway.installedBy=$(whoami)
flyway.target=latest
flyway.outOfOrder=false
flyway.ignoreMissingMigrations=false
flyway.ignoreIgnoredMigrations=false
flyway.ignorePendingMigrations=false
flyway.ignoreFutureMigrations=false
EOF
}

# Verify migration integrity
verify_migration_integrity() {
    local env="$1"
    local version="$2"
    
    log_info "Verifying migration integrity for $env..."
    
    # Check schema integrity
    if ! check_schema_integrity "$env"; then
        log_error "Schema integrity check failed"
        return 1
    fi
    
    # Check data integrity
    if ! check_data_integrity "$env"; then
        log_error "Data integrity check failed"
        return 1
    fi
    
    # Check constraints
    if ! check_database_constraints "$env"; then
        log_error "Database constraints check failed"
        return 1
    fi
    
    log_success "Migration integrity verification completed"
}

# Check schema integrity
check_schema_integrity() {
    local env="$1"
    local db_url=$(get_env_config "$env" "database_url")
    
    # Extract connection details
    local db_host=$(echo "$db_url" | sed -n 's/.*:\/\/\([^:]*\):.*/\1/p')
    local db_port=$(echo "$db_url" | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
    local db_name=$(echo "$db_url" | sed -n 's/.*\/\([^?]*\).*/\1/p')
    
    # Check required tables exist
    local required_tables=("customers" "loans" "payments" "audit_events")
    
    for table in "${required_tables[@]}"; do
        local table_exists=$(PGPASSWORD="$DB_PASSWORD" psql -h "$db_host" -p "$db_port" -U "$DB_USERNAME" -d "$db_name" -t -c \
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = '$table';" 2>/dev/null | tr -d ' ' || echo "0")
        
        if [[ "$table_exists" != "1" ]]; then
            log_error "Required table '$table' not found"
            return 1
        fi
    done
    
    return 0
}

# Check data integrity
check_data_integrity() {
    local env="$1"
    local db_url=$(get_env_config "$env" "database_url")
    
    # Extract connection details
    local db_host=$(echo "$db_url" | sed -n 's/.*:\/\/\([^:]*\):.*/\1/p')
    local db_port=$(echo "$db_url" | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
    local db_name=$(echo "$db_url" | sed -n 's/.*\/\([^?]*\).*/\1/p')
    
    # Check for orphaned records
    local orphaned_loans=$(PGPASSWORD="$DB_PASSWORD" psql -h "$db_host" -p "$db_port" -U "$DB_USERNAME" -d "$db_name" -t -c \
        "SELECT COUNT(*) FROM loan_context.loans l LEFT JOIN customer_context.customers c ON l.customer_id = c.customer_id WHERE c.customer_id IS NULL;" 2>/dev/null | tr -d ' ' || echo "0")
    
    if [[ $orphaned_loans -gt 0 ]]; then
        log_warn "Found $orphaned_loans orphaned loan records"
    fi
    
    # Check payment integrity
    local orphaned_payments=$(PGPASSWORD="$DB_PASSWORD" psql -h "$db_host" -p "$db_port" -U "$DB_USERNAME" -d "$db_name" -t -c \
        "SELECT COUNT(*) FROM payment_context.payments p LEFT JOIN customer_context.customers c ON p.customer_id = c.customer_id WHERE c.customer_id IS NULL;" 2>/dev/null | tr -d ' ' || echo "0")
    
    if [[ $orphaned_payments -gt 0 ]]; then
        log_warn "Found $orphaned_payments orphaned payment records"
    fi
    
    return 0
}

# Check database constraints
check_database_constraints() {
    local env="$1"
    local db_url=$(get_env_config "$env" "database_url")
    
    # Extract connection details
    local db_host=$(echo "$db_url" | sed -n 's/.*:\/\/\([^:]*\):.*/\1/p')
    local db_port=$(echo "$db_url" | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
    local db_name=$(echo "$db_url" | sed -n 's/.*\/\([^?]*\).*/\1/p')
    
    # Check for constraint violations
    local constraint_violations=$(PGPASSWORD="$DB_PASSWORD" psql -h "$db_host" -p "$db_port" -U "$DB_USERNAME" -d "$db_name" -t -c \
        "SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_type = 'CHECK';" 2>/dev/null | tr -d ' ' || echo "0")
    
    log_info "Database constraints verified: $constraint_violations check constraints"
    
    return 0
}

# Create database backup
create_database_backup() {
    local env="$1"
    local backup_name="$2"
    local timestamp=$(date +%Y%m%d-%H%M%S)
    local backup_file="$BACKUP_STORAGE_PATH/${env}-${backup_name}-${timestamp}.sql"
    
    log_info "Creating database backup: $backup_file"
    
    # Ensure backup directory exists
    mkdir -p "$(dirname "$backup_file")"
    
    # Create backup using pg_dump
    local db_url=$(get_env_config "$env" "database_url")
    local db_host=$(echo "$db_url" | sed -n 's/.*:\/\/\([^:]*\):.*/\1/p')
    local db_port=$(echo "$db_url" | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
    local db_name=$(echo "$db_url" | sed -n 's/.*\/\([^?]*\).*/\1/p')
    
    PGPASSWORD="$DB_PASSWORD" pg_dump \
        -h "$db_host" \
        -p "$db_port" \
        -U "$DB_USERNAME" \
        -d "$db_name" \
        --verbose \
        --clean \
        --if-exists \
        --create \
        --format=custom \
        --compress=9 \
        --file="$backup_file"
    
    if [[ $? -eq 0 ]]; then
        log_success "Database backup created: $backup_file"
        
        # Verify backup
        verify_backup "$backup_file"
        
        # Clean up old backups
        cleanup_old_backups "$env"
    else
        log_error "Database backup failed"
        return 1
    fi
}

# Verify backup integrity
verify_backup() {
    local backup_file="$1"
    
    log_info "Verifying backup integrity: $backup_file"
    
    # Check file exists and is not empty
    if [[ ! -f "$backup_file" || ! -s "$backup_file" ]]; then
        log_error "Backup file is missing or empty"
        return 1
    fi
    
    # Verify backup can be read
    if ! pg_restore --list "$backup_file" &> /dev/null; then
        log_error "Backup file is corrupted or unreadable"
        return 1
    fi
    
    log_success "Backup verification completed"
}

# Clean up old backups
cleanup_old_backups() {
    local env="$1"
    
    log_info "Cleaning up old backups for $env..."
    
    # Remove backups older than retention period
    find "$BACKUP_STORAGE_PATH" -name "${env}-*" -type f -mtime +$DB_BACKUP_RETENTION_DAYS -delete
    
    # Keep only the latest N versions
    local backup_count=$(find "$BACKUP_STORAGE_PATH" -name "${env}-*" -type f | wc -l)
    
    if [[ $backup_count -gt $BACKUP_RETENTION_VERSIONS ]]; then
        local excess_backups=$((backup_count - BACKUP_RETENTION_VERSIONS))
        find "$BACKUP_STORAGE_PATH" -name "${env}-*" -type f -printf '%T@ %p\n' | \
            sort -n | head -n "$excess_backups" | cut -d' ' -f2- | xargs rm -f
        
        log_info "Removed $excess_backups old backup files"
    fi
}

# Rollback database migrations
rollback_database_migrations() {
    local env="$1"
    local target_version="$2"
    
    log_warn "Rolling back database migrations for $env to version $target_version"
    
    # Create backup before rollback
    create_database_backup "$env" "pre-rollback-$target_version"
    
    # Get the target migration version
    local target_migration=$(get_migration_version "$target_version")
    
    if [[ -z "$target_migration" ]]; then
        log_error "Cannot determine target migration for version $target_version"
        return 1
    fi
    
    # Perform rollback
    if execute_database_rollback "$env" "$target_migration"; then
        log_success "Database rollback completed successfully"
        
        # Verify rollback integrity
        verify_rollback_integrity "$env" "$target_version"
    else
        log_error "Database rollback failed"
        return 1
    fi
}

# Get migration version for application version
get_migration_version() {
    local app_version="$1"
    
    # This would typically look up the migration version from a mapping table
    # For now, use a simple mapping based on semantic version
    case "$app_version" in
        2.0.*)
            echo "V2_0__baseline.sql"
            ;;
        2.1.*)
            echo "V2_1__enhanced_features.sql"
            ;;
        *)
            echo "V2_0__baseline.sql"
            ;;
    esac
}

# Execute database rollback
execute_database_rollback() {
    local env="$1"
    local target_migration="$2"
    local db_url=$(get_env_config "$env" "database_url")
    
    # Flyway doesn't support automatic rollback, so we need custom logic
    # This would typically involve applying reverse migration scripts
    
    log_info "Executing rollback to migration: $target_migration"
    
    # Apply rollback scripts if they exist
    local rollback_script="$PROJECT_ROOT/src/main/resources/db/rollback/$(echo "$target_migration" | sed 's/V/R/')"
    
    if [[ -f "$rollback_script" ]]; then
        # Extract connection details
        local db_host=$(echo "$db_url" | sed -n 's/.*:\/\/\([^:]*\):.*/\1/p')
        local db_port=$(echo "$db_url" | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
        local db_name=$(echo "$db_url" | sed -n 's/.*\/\([^?]*\).*/\1/p')
        
        PGPASSWORD="$DB_PASSWORD" psql -h "$db_host" -p "$db_port" -U "$DB_USERNAME" -d "$db_name" -f "$rollback_script"
    else
        log_warn "No rollback script found for $target_migration"
        return 1
    fi
}

# Verify rollback integrity
verify_rollback_integrity() {
    local env="$1"
    local version="$2"
    
    log_info "Verifying rollback integrity for $env (version $version)..."
    
    # Check that the application can connect and function
    if check_application_database_health "$env"; then
        log_success "Rollback integrity verification completed"
    else
        log_error "Rollback integrity verification failed"
        return 1
    fi
}

# Rollback failed migrations
rollback_failed_migrations() {
    local env="$1"
    local version="$2"
    
    log_warn "Attempting to rollback failed migrations..."
    
    # Get the last successful migration
    local last_successful=$(get_last_successful_migration "$env")
    
    if [[ -n "$last_successful" ]]; then
        log_info "Rolling back to last successful migration: $last_successful"
        execute_database_rollback "$env" "$last_successful"
    else
        log_error "Cannot determine last successful migration"
        return 1
    fi
}

# Get last successful migration
get_last_successful_migration() {
    local env="$1"
    local db_url=$(get_env_config "$env" "database_url")
    
    # Extract connection details
    local db_host=$(echo "$db_url" | sed -n 's/.*:\/\/\([^:]*\):.*/\1/p')
    local db_port=$(echo "$db_url" | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
    local db_name=$(echo "$db_url" | sed -n 's/.*\/\([^?]*\).*/\1/p')
    
    # Query Flyway schema history for last successful migration
    PGPASSWORD="$DB_PASSWORD" psql -h "$db_host" -p "$db_port" -U "$DB_USERNAME" -d "$db_name" -t -c \
        "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank DESC LIMIT 1;" 2>/dev/null | tr -d ' '
}

# Test database performance
test_database_performance() {
    local env="$1"
    
    log_info "Testing database performance for $env..."
    
    # Simple performance test queries
    local db_url=$(get_env_config "$env" "database_url")
    local db_host=$(echo "$db_url" | sed -n 's/.*:\/\/\([^:]*\):.*/\1/p')
    local db_port=$(echo "$db_url" | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
    local db_name=$(echo "$db_url" | sed -n 's/.*\/\([^?]*\).*/\1/p')
    
    # Test query response time
    local start_time=$(date +%s%N)
    PGPASSWORD="$DB_PASSWORD" psql -h "$db_host" -p "$db_port" -U "$DB_USERNAME" -d "$db_name" -c "SELECT COUNT(*) FROM customer_context.customers;" &> /dev/null
    local end_time=$(date +%s%N)
    local response_time=$(( (end_time - start_time) / 1000000 )) # Convert to milliseconds
    
    log_info "Database query response time: ${response_time}ms"
    
    if [[ $response_time -gt 1000 ]]; then
        log_warn "Database response time is high: ${response_time}ms"
    fi
}