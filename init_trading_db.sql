-- init_trading_db.sql

-- ==========================================
-- SETUP TABLES (Infrastructure)
-- ==========================================

-- 1. The Event Log
-- Stores all trade events (Booking, Clearing, etc.)
CREATE TABLE IF NOT EXISTS trading_pipeline_tracker (
    event_id TEXT PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    subject VARCHAR(50) NOT NULL,
    source_system VARCHAR(50) NOT NULL,
    trading_date DATE NOT NULL,
    event_time TIMESTAMP NOT NULL,
    data JSONB NOT NULL
);

-- Index for faster querying of the JSONB payload
CREATE INDEX IF NOT EXISTS idx_trading_data ON trading_pipeline_tracker USING GIN (data);

-- 2. The Static Rules Engine
-- Stores Service Level Objectives (SLOs) for monitoring latency
CREATE TABLE IF NOT EXISTS slo_definitions (
    slo_id SERIAL PRIMARY KEY,
    process_name VARCHAR(50) NOT NULL,
    target_subject VARCHAR(50),
    warning_threshold_seconds INT,
    critical_threshold_seconds INT,
    description TEXT
);