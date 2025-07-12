-- Run this in BigQuery console to create the dataset and table

CREATE SCHEMA IF NOT EXISTS `your-project-id.cdc_dataset`;

CREATE TABLE IF NOT EXISTS `your-project-id.cdc_dataset.cdc_events` (
  table_name STRING,
  operation STRING,
  record_id STRING,
  data STRING,
  timestamp_ms INT64,
  processing_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP()
);
