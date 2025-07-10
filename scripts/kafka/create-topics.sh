#!/bin/bash

# Enterprise Loan Management System - Kafka Topics Creation
# Creates all required Kafka topics with proper partitioning and replication

set -e

KAFKA_BROKER="kafka:9092"
PARTITIONS=3
REPLICATION_FACTOR=1

echo "🚀 Creating Kafka topics for Enterprise Loan Management System..."

# Wait for Kafka to be ready
echo "⏳ Waiting for Kafka to be ready..."
until kafka-broker-api-versions --bootstrap-server $KAFKA_BROKER >/dev/null 2>&1; do
    echo "Kafka not ready yet, waiting 5 seconds..."
    sleep 5
done

echo "✅ Kafka is ready!"

# Function to create topic
create_topic() {
    local topic_name=$1
    local partitions=${2:-$PARTITIONS}
    local replication=${3:-$REPLICATION_FACTOR}
    
    echo "📝 Creating topic: $topic_name (partitions: $partitions, replication: $replication)"
    
    kafka-topics --create \
        --bootstrap-server $KAFKA_BROKER \
        --topic $topic_name \
        --partitions $partitions \
        --replication-factor $replication \
        --if-not-exists \
        --config cleanup.policy=compact,delete \
        --config retention.ms=604800000 \
        --config segment.ms=86400000 \
        --config max.message.bytes=1048576
    
    if [ $? -eq 0 ]; then
        echo "✅ Topic $topic_name created successfully"
    else
        echo "❌ Failed to create topic $topic_name"
        return 1
    fi
}

# Core Banking Topics
echo "🏦 Creating Core Banking Topics..."

# Customer Domain Events
create_topic "customer.events" 3 1
create_topic "customer.created" 3 1
create_topic "customer.updated" 3 1
create_topic "customer.activated" 3 1
create_topic "customer.suspended" 3 1
create_topic "customer.closed" 3 1
create_topic "customer.kyc.completed" 3 1
create_topic "customer.credit.updated" 3 1

# Loan Domain Events
create_topic "loan.events" 3 1
create_topic "loan.application.submitted" 3 1
create_topic "loan.application.approved" 3 1
create_topic "loan.application.rejected" 3 1
create_topic "loan.disbursed" 3 1
create_topic "loan.payment.made" 3 1
create_topic "loan.payment.overdue" 3 1
create_topic "loan.paid.off" 3 1
create_topic "loan.defaulted" 3 1
create_topic "loan.restructured" 3 1

# Payment Domain Events
create_topic "payment.events" 3 1
create_topic "payment.initiated" 3 1
create_topic "payment.processed" 3 1
create_topic "payment.completed" 3 1
create_topic "payment.failed" 3 1
create_topic "payment.cancelled" 3 1
create_topic "payment.refunded" 3 1
create_topic "payment.reversed" 3 1

# Compliance and Audit Topics
echo "🔒 Creating Compliance and Audit Topics..."

create_topic "compliance.events" 3 1
create_topic "compliance.kyc.check" 3 1
create_topic "compliance.aml.check" 3 1
create_topic "compliance.sanctions.check" 3 1
create_topic "compliance.pep.check" 3 1
create_topic "compliance.regulatory.report" 3 1

create_topic "audit.events" 6 1
create_topic "audit.user.actions" 3 1
create_topic "audit.data.changes" 3 1
create_topic "audit.security.events" 3 1
create_topic "audit.access.logs" 3 1
create_topic "audit.system.events" 3 1

# ML and Analytics Topics
echo "🤖 Creating ML and Analytics Topics..."

create_topic "ml.events" 3 1
create_topic "ml.fraud.detection" 3 1
create_topic "ml.credit.scoring" 3 1
create_topic "ml.risk.assessment" 3 1
create_topic "ml.anomaly.detection" 3 1
create_topic "ml.model.training" 3 1
create_topic "ml.model.deployment" 3 1
create_topic "ml.predictions" 3 1

# Real-time Analytics
create_topic "analytics.events" 3 1
create_topic "analytics.transaction.volume" 3 1
create_topic "analytics.performance.metrics" 3 1
create_topic "analytics.business.metrics" 3 1
create_topic "analytics.customer.behavior" 3 1

# Federation and Cross-Region Topics
echo "🌍 Creating Federation and Cross-Region Topics..."

create_topic "federation.events" 3 1
create_topic "federation.metrics" 3 1
create_topic "federation.alerts" 3 1
create_topic "federation.disaster.recovery" 3 1
create_topic "federation.regional.sync" 3 1

# Cross-region replication topics
create_topic "cross.region.us.east.1" 3 1
create_topic "cross.region.eu.west.1" 3 1
create_topic "cross.region.ap.southeast.1" 3 1

# OAuth 2.1 + DPoP + FAPI Security Topics
echo "🔐 Creating Security Topics..."

create_topic "security.events" 3 1
create_topic "security.oauth.events" 3 1
create_topic "security.dpop.events" 3 1
create_topic "security.fapi.events" 3 1
create_topic "security.authentication" 3 1
create_topic "security.authorization" 3 1
create_topic "security.token.events" 3 1
create_topic "security.session.events" 3 1

# Zero Trust Security Topics
create_topic "zerotrust.events" 3 1
create_topic "zerotrust.continuous.verification" 3 1
create_topic "zerotrust.policy.enforcement" 3 1
create_topic "zerotrust.threat.detection" 3 1

# Open Banking Topics
echo "🏪 Creating Open Banking Topics..."

create_topic "openbanking.events" 3 1
create_topic "openbanking.account.access" 3 1
create_topic "openbanking.payment.initiation" 3 1
create_topic "openbanking.consent.management" 3 1
create_topic "openbanking.api.calls" 3 1

# Notification and Communication Topics
echo "📢 Creating Notification Topics..."

create_topic "notifications.events" 3 1
create_topic "notifications.email" 3 1
create_topic "notifications.sms" 3 1
create_topic "notifications.push" 3 1
create_topic "notifications.system.alerts" 3 1

# Dead Letter Topics
echo "💀 Creating Dead Letter Topics..."

create_topic "deadletter.events" 3 1
create_topic "deadletter.customer" 3 1
create_topic "deadletter.loan" 3 1
create_topic "deadletter.payment" 3 1
create_topic "deadletter.compliance" 3 1
create_topic "deadletter.ml" 3 1

# Monitoring and Metrics Topics
echo "📊 Creating Monitoring Topics..."

create_topic "monitoring.events" 3 1
create_topic "monitoring.health.checks" 3 1
create_topic "monitoring.performance" 3 1
create_topic "monitoring.errors" 3 1
create_topic "monitoring.alerts" 3 1

# High-throughput topics for transaction processing
echo "⚡ Creating High-Throughput Topics..."

create_topic "transactions.high.volume" 6 1
create_topic "payments.real.time" 6 1
create_topic "fraud.detection.real.time" 6 1

# Configure topic settings for specific use cases
echo "⚙️ Configuring topic-specific settings..."

# High-retention topics for compliance
kafka-configs --bootstrap-server $KAFKA_BROKER --alter --entity-type topics --entity-name audit.events --add-config retention.ms=31536000000  # 1 year
kafka-configs --bootstrap-server $KAFKA_BROKER --alter --entity-type topics --entity-name compliance.events --add-config retention.ms=31536000000  # 1 year

# Low-latency topics for real-time processing
kafka-configs --bootstrap-server $KAFKA_BROKER --alter --entity-type topics --entity-name fraud.detection.real.time --add-config min.insync.replicas=1
kafka-configs --bootstrap-server $KAFKA_BROKER --alter --entity-type topics --entity-name payments.real.time --add-config min.insync.replicas=1

# Compacted topics for state management
kafka-configs --bootstrap-server $KAFKA_BROKER --alter --entity-type topics --entity-name customer.events --add-config cleanup.policy=compact
kafka-configs --bootstrap-server $KAFKA_BROKER --alter --entity-type topics --entity-name loan.events --add-config cleanup.policy=compact

# List all created topics
echo "📋 Listing all created topics:"
kafka-topics --bootstrap-server $KAFKA_BROKER --list | sort

# Verify topic creation
echo "🔍 Verifying topic configurations:"
total_topics=$(kafka-topics --bootstrap-server $KAFKA_BROKER --list | wc -l)
echo "✅ Total topics created: $total_topics"

# Topic health check
echo "🏥 Performing topic health check..."
for topic in $(kafka-topics --bootstrap-server $KAFKA_BROKER --list | head -5); do
    kafka-topics --bootstrap-server $KAFKA_BROKER --describe --topic $topic | head -1
done

echo "🎉 Kafka topics setup completed successfully!"
echo "📝 Topics are ready for Enterprise Loan Management System"
echo "🔧 All topics configured with:"
echo "   - Partitions: $PARTITIONS"
echo "   - Replication Factor: $REPLICATION_FACTOR"
echo "   - Retention: 7 days (audit/compliance: 1 year)"
echo "   - Cleanup Policy: compact,delete"
echo "   - Max Message Size: 1MB"

# Create sample messages for testing
echo "📨 Creating sample messages for testing..."

# Sample customer event
echo '{"eventType":"customer.created","customerId":"110e8400-e29b-41d4-a716-446655440001","customerNumber":"CUST-001","timestamp":"2024-01-01T10:00:00Z","eventData":{"customerType":"INDIVIDUAL","status":"ACTIVE","riskRating":"LOW"}}' | kafka-console-producer --bootstrap-server $KAFKA_BROKER --topic customer.events

# Sample loan event
echo '{"eventType":"loan.application.submitted","loanId":"990e8400-e29b-41d4-a716-446655440001","applicationId":"880e8400-e29b-41d4-a716-446655440001","timestamp":"2024-01-15T10:00:00Z","eventData":{"loanType":"PERSONAL","requestedAmount":25000.00,"customerId":"110e8400-e29b-41d4-a716-446655440001"}}' | kafka-console-producer --bootstrap-server $KAFKA_BROKER --topic loan.events

# Sample payment event
echo '{"eventType":"payment.processed","paymentId":"bb0e8400-e29b-41d4-a716-446655440001","timestamp":"2024-03-01T10:00:00Z","eventData":{"amount":486.87,"currency":"USD","paymentMethod":"BANK_TRANSFER","status":"COMPLETED"}}' | kafka-console-producer --bootstrap-server $KAFKA_BROKER --topic payment.events

# Sample ML event
echo '{"eventType":"fraud.detection","predictionId":"2f0e8400-e29b-41d4-a716-446655440001","timestamp":"2024-03-01T10:00:00Z","eventData":{"entityType":"PAYMENT","fraudProbability":0.0234,"riskScore":2,"confidence":0.9876}}' | kafka-console-producer --bootstrap-server $KAFKA_BROKER --topic ml.fraud.detection

# Sample federation event
echo '{"eventType":"cross.region.metric","region":"us-east-1","timestamp":"2024-03-01T10:00:00Z","eventData":{"metricType":"CPU_UTILIZATION","value":65.5,"unit":"percentage","threshold":80}}' | kafka-console-producer --bootstrap-server $KAFKA_BROKER --topic federation.metrics

echo "✅ Sample messages created successfully!"
echo "🔗 You can now test the system with pre-populated Kafka topics and sample data"

exit 0