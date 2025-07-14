-- flink-sql-job.sql
-- This can be executed via Flink SQL Client without compilation

-- Create Kafka source table for users
CREATE TABLE kafka_users (
    id BIGINT,
    name STRING,
    email STRING,
    created_at STRING,
    updated_at STRING,
    __op STRING,
    __ts_ms BIGINT
) WITH (
    'connector' = 'kafka',
    'topic' = 'cdc.public.users',
    'properties.bootstrap.servers' = 'kafka:29092',
    'properties.group.id' = 'flink-sql-consumer',
    'format' = 'json',
    'scan.startup.mode' = 'earliest-offset'
);

-- Create Kafka source table for orders
CREATE TABLE kafka_orders (
    id BIGINT,
    user_id BIGINT,
    product_name STRING,
    quantity INT,
    price DECIMAL(10,2),
    status STRING,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    __op STRING,
    __ts_ms BIGINT
) WITH (
    'connector' = 'kafka',
    'topic' = 'cdc.public.orders',
    'properties.bootstrap.servers' = 'kafka:29092',
    'properties.group.id' = 'flink-sql-consumer',
    'format' = 'json',
    'scan.startup.mode' = 'earliest-offset'
);

-- Create Kafka source table for products
CREATE TABLE kafka_products (
    id BIGINT,
    name STRING,
    category STRING,
    price DECIMAL(10,2),
    stock_quantity INT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    __op STRING,
    __ts_ms BIGINT
) WITH (
    'connector' = 'kafka',
    'topic' = 'cdc.public.products',
    'properties.bootstrap.servers' = 'kafka:29092',
    'properties.group.id' = 'flink-sql-consumer',
    'format' = 'json',
    'scan.startup.mode' = 'earliest-offset'
);

-- Create console sink for testing (replace with BigQuery later)
CREATE TABLE console_sink (
    table_name STRING,
    operation STRING,
    record_id STRING,
    data STRING,
    event_timestamp TIMESTAMP,
    processing_time TIMESTAMP
) WITH (
    'connector' = 'print'
);

-- Process and insert user changes
INSERT INTO console_sink
SELECT 
    'users' as table_name,
    __op as operation,
    CAST(id as STRING) as record_id,
    CONCAT('{"id":', CAST(id as STRING), ',"name":"', name, '","email":"', email, '"}') as data,
    TO_TIMESTAMP(cast(__ts_ms as varchar)) as event_timestamp,
    CURRENT_TIMESTAMP as processing_time
FROM kafka_users;

-- Process and insert order changes
INSERT INTO console_sink
SELECT 
    'orders' as table_name,
    __op as operation,
    CAST(id as STRING) as record_id,
    CONCAT('{"id":', CAST(id as STRING), ',"user_id":', CAST(user_id as STRING), ',"product_name":"', product_name, '","quantity":', CAST(quantity as STRING), ',"price":', CAST(price as STRING), ',"status":"', status, '"}') as data,
    TO_TIMESTAMP(cast(__ts_ms as varchar)) as event_timestamp,
    CURRENT_TIMESTAMP as processing_time
FROM kafka_orders;

-- Process and insert product changes
INSERT INTO console_sink
SELECT 
    'products' as table_name,
    __op as operation,
    CAST(id as STRING) as record_id,
    CONCAT('{"id":', CAST(id as STRING), ',"name":"', name, '","category":"', category, '","price":', CAST(price as STRING), ',"stock_quantity":', CAST(stock_quantity as STRING), '}') as data,
    TO_TIMESTAMP(cast(__ts_ms as varchar)) as event_timestamp,
    CURRENT_TIMESTAMP as processing_time
FROM kafka_products;