/*
 * Idempotent index creation for Open Finance analytics collections.
 * Run after preflight succeeds.
 */

function ensureIndex(collectionName, keys, options) {
    const name = options && options.name ? options.name : JSON.stringify(keys);
    print("Ensuring index on " + collectionName + ": " + name);
    db.getCollection(collectionName).createIndex(keys, options || {});
}

print("Applying Open Finance analytics indexes on database: " + db.getName());

/* BCNF business-key indexes */
ensureIndex("customer_patterns", { customerId: 1 }, {
    name: "customer_id_uk",
    unique: true
});

ensureIndex("consent_metrics_summary", { participantId: 1 }, {
    name: "participant_id_uk",
    unique: true
});

ensureIndex("compliance_reports", { reportDate: 1, reportType: 1 }, {
    name: "report_date_type_uk",
    unique: true
});

/* Existing query and operational indexes */
ensureIndex("compliance_reports", { reportDate: 1 }, { name: "report_date_idx" });
ensureIndex("compliance_reports", { generatedAt: 1 }, { name: "generated_at_idx" });

ensureIndex("security_incidents", { incidentType: 1 }, { name: "incident_type_idx" });
ensureIndex("security_incidents", { severity: 1 }, { name: "severity_idx" });
ensureIndex("security_incidents", { occurredAt: 1 }, { name: "occurred_at_idx" });
ensureIndex("security_incidents", { status: 1 }, { name: "status_idx" });

ensureIndex("usage_analytics", { participantId: 1, date: 1 }, {
    name: "participant_date_idx"
});
ensureIndex("usage_analytics", { consentId: 1, timestamp: -1 }, {
    name: "consent_timestamp_idx"
});
ensureIndex("usage_analytics", { consentId: 1 }, { name: "consent_id_idx" });
ensureIndex("usage_analytics", { participantId: 1 }, { name: "participant_id_idx" });
ensureIndex("usage_analytics", { date: 1 }, { name: "date_idx" });
ensureIndex("usage_analytics", { timestamp: 1 }, { name: "timestamp_idx" });

ensureIndex("real_time_metrics", { metricType: 1 }, { name: "metric_type_idx" });
ensureIndex("real_time_metrics", { participantId: 1 }, { name: "participant_id_idx" });
ensureIndex("real_time_metrics", { timestamp: 1 }, {
    name: "timestamp_ttl_idx",
    expireAfterSeconds: 300
});

ensureIndex("customer_patterns", { firstConsentDate: 1 }, { name: "first_consent_date_idx" });
ensureIndex("customer_patterns", { lastConsentDate: 1 }, { name: "last_consent_date_idx" });

print("Open Finance analytics indexes ensured successfully.");
