/*
 * Preflight checks before applying unique indexes to Open Finance analytics collections.
 * Fails if duplicate keys or null business-key fields are found.
 */

function printJson(label, value) {
    print(label + ": " + JSON.stringify(value, null, 2));
}

function duplicateRows(collectionName, keyFields) {
    const idExpr = {};
    keyFields.forEach((field) => {
        idExpr[field] = "$" + field;
    });

    return db.getCollection(collectionName).aggregate([
        { $group: { _id: idExpr, count: { $sum: 1 }, ids: { $push: "$_id" } } },
        { $match: { count: { $gt: 1 } } },
        {
            $project: {
                _id: 0,
                key: "$_id",
                count: 1,
                sampleIds: { $slice: ["$ids", 5] }
            }
        },
        { $sort: { count: -1 } },
        { $limit: 50 }
    ]).toArray();
}

function nullOrMissingCount(collectionName, keyFields) {
    const filters = keyFields.map((field) => ({
        $or: [
            { [field]: { $exists: false } },
            { [field]: null }
        ]
    }));

    return db.getCollection(collectionName).countDocuments({ $or: filters });
}

const checks = [
    { collection: "customer_patterns", keys: ["customerId"] },
    { collection: "consent_metrics_summary", keys: ["participantId"] },
    { collection: "compliance_reports", keys: ["reportDate", "reportType"] }
];

let hasFailures = false;

print("MongoDB analytics preflight on database: " + db.getName());

checks.forEach((check) => {
    const nullCount = nullOrMissingCount(check.collection, check.keys);
    const duplicates = duplicateRows(check.collection, check.keys);

    print("");
    print("Collection: " + check.collection);
    print("Business key: " + check.keys.join(", "));
    print("Null/missing key docs: " + nullCount);
    print("Duplicate key groups: " + duplicates.length);

    if (nullCount > 0) {
        hasFailures = true;
        print("ERROR: null or missing business-key fields found.");
    }

    if (duplicates.length > 0) {
        hasFailures = true;
        print("ERROR: duplicate business keys found. Sample groups:");
        printJson("duplicates", duplicates);
    }
});

if (hasFailures) {
    throw new Error("Preflight failed. Resolve key violations before applying unique indexes.");
}

print("");
print("Preflight passed. It is safe to apply Open Finance analytics indexes.");
