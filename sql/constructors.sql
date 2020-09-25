-- Create database
CREATE DATABASE IF NOT EXISTS logs ON CLUSTER reports;

-- Create base table for events
CREATE TABLE IF NOT EXISTS logs.visit_log ON CLUSTER reports
(
    event_date DateTime,
    dmp String,
    dmp_id UInt64 DEFAULT cityHash64(dmp),
    country String,
    city String,
    gender Int8,
    yob Int32,
    keywords Array(String),
    site_id Int64
)
ENGINE = MergeTree()
PARTITION BY toYYYYMM(event_date)
ORDER BY (event_date, dmp_id, site_id)
SAMPLE BY dmp_id
SETTINGS index_granularity = 8192;

-- Create distributed table over logs.visit_log
CREATE TABLE IF NOT EXISTS logs.visit_log_all ON CLUSTER reports AS logs.visit_log
ENGINE = Distributed('reports', 'logs', 'visit_log', dmp_id);
