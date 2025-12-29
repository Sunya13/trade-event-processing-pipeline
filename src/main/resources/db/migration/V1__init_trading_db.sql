-- ==========================================
-- SETUP TABLES (Managed by Flyway)
-- ==========================================

-- 1. The Event Log
-- This table stores every single event in the system (Booking, Amendment, Verification, Cancellation)
CREATE TABLE IF NOT EXISTS trading_pipeline_tracker (
    event_id TEXT PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    subject VARCHAR(50) NOT NULL,
    source_system VARCHAR(50) NOT NULL,
    trading_date DATE NOT NULL,
    event_time TIMESTAMP NOT NULL,
    data JSONB NOT NULL
);

-- Index for faster querying of the JSONB payload (e.g. searching by tradeRef inside the JSON)
CREATE INDEX IF NOT EXISTS idx_trading_data ON trading_pipeline_tracker USING GIN (data);

-- 2. The Static Rules Engine
-- This table defines the Service Level Objectives (SLOs) for monitoring
CREATE TABLE IF NOT EXISTS slo_definitions (
    slo_id SERIAL PRIMARY KEY,
    process_name VARCHAR(50) NOT NULL,
    target_subject VARCHAR(50),
    warning_threshold_seconds INT,
    critical_threshold_seconds INT,
    description TEXT
);

-- 3. Initial Data Seed (Optional)
-- Insert default SLOs so the monitoring system works out of the box
INSERT INTO slo_definitions (process_name, target_subject, warning_threshold_seconds, critical_threshold_seconds, description)
VALUES
('Trade Booking', 'Equity', 30, 60, 'Equity trades must be booked within 30 seconds'),
('Trade Verification', 'Bond', 120, 300, 'Bond verifications allow more time'),
('Risk Check', NULL, 10, 20, 'Risk checks must be near real-time');