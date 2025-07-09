-- Create tables for real estate feature engineering pipeline

-- Reference data: neighborhoods with static information
CREATE TABLE IF NOT EXISTS neighborhoods (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    city VARCHAR(50) NOT NULL,
    state VARCHAR(20) NOT NULL,
    zip_code VARCHAR(10) NOT NULL,
    avg_income DECIMAL(12,2),
    population INTEGER,
    crime_rate DECIMAL(5,2),
    school_rating DECIMAL(3,1),
    walkability_score INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Reference data: property types
CREATE TABLE IF NOT EXISTS property_types (
    id SERIAL PRIMARY KEY,
    type_name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

-- Streaming data: property listings
CREATE TABLE IF NOT EXISTS property_listings (
    id SERIAL PRIMARY KEY,
    property_id VARCHAR(50) UNIQUE NOT NULL,
    neighborhood_id INTEGER REFERENCES neighborhoods(id),
    property_type_id INTEGER REFERENCES property_types(id),
    price DECIMAL(12,2) NOT NULL,
    bedrooms INTEGER,
    bathrooms DECIMAL(3,1),
    square_feet INTEGER,
    lot_size_sqft INTEGER,
    year_built INTEGER,
    listing_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8)
);

-- Streaming data: price changes
CREATE TABLE IF NOT EXISTS price_changes (
    id SERIAL PRIMARY KEY,
    property_id VARCHAR(50) NOT NULL,
    old_price DECIMAL(12,2),
    new_price DECIMAL(12,2),
    change_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    change_reason VARCHAR(100)
);

-- Streaming data: property views/interactions
CREATE TABLE IF NOT EXISTS property_interactions (
    id SERIAL PRIMARY KEY,
    property_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(50),
    interaction_type VARCHAR(20), -- 'view', 'favorite', 'contact', 'schedule_tour'
    interaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    session_id VARCHAR(50),
    device_type VARCHAR(20),
    source VARCHAR(50) -- 'web', 'mobile_app', 'agent'
);

-- Feature store: real-time engineered features
CREATE TABLE IF NOT EXISTS property_features (
    property_id VARCHAR(50) PRIMARY KEY,
    
    -- Price-based features
    current_price DECIMAL(12,2),
    price_per_sqft DECIMAL(8,2),
    price_change_7d DECIMAL(12,2),
    price_change_30d DECIMAL(12,2),
    price_volatility_30d DECIMAL(8,4),
    
    -- Market position features
    price_vs_neighborhood_avg DECIMAL(8,4), -- ratio
    price_percentile_neighborhood DECIMAL(5,2),
    price_percentile_city DECIMAL(5,2),
    
    -- Engagement features
    views_last_24h INTEGER DEFAULT 0,
    views_last_7d INTEGER DEFAULT 0,
    views_last_30d INTEGER DEFAULT 0,
    favorites_count INTEGER DEFAULT 0,
    contact_requests_count INTEGER DEFAULT 0,
    
    -- Time-based features
    days_on_market INTEGER,
    listing_recency_score DECIMAL(5,2),
    
    -- Neighborhood context features
    neighborhood_activity_score DECIMAL(5,2),
    neighborhood_price_trend DECIMAL(8,4),
    
    -- Composite features
    demand_score DECIMAL(5,2),
    investment_score DECIMAL(5,2),
    
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Aggregated neighborhood features
CREATE TABLE IF NOT EXISTS neighborhood_features (
    neighborhood_id INTEGER PRIMARY KEY REFERENCES neighborhoods(id),
    
    -- Market metrics
    avg_price DECIMAL(12,2),
    median_price DECIMAL(12,2),
    price_per_sqft_avg DECIMAL(8,2),
    total_active_listings INTEGER,
    new_listings_7d INTEGER,
    
    -- Activity metrics
    total_views_7d INTEGER,
    avg_days_on_market DECIMAL(5,1),
    
    -- Trends
    price_change_7d_pct DECIMAL(6,3),
    price_change_30d_pct DECIMAL(6,3),
    inventory_change_7d INTEGER,
    
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert sample neighborhoods
INSERT INTO neighborhoods (name, city, state, zip_code, avg_income, population, crime_rate, school_rating, walkability_score) VALUES
('Downtown', 'Seattle', 'WA', '98101', 95000.00, 15000, 2.1, 8.5, 95),
('Capitol Hill', 'Seattle', 'WA', '98102', 85000.00, 25000, 1.8, 9.0, 88),
('Fremont', 'Seattle', 'WA', '98103', 78000.00, 18000, 1.2, 8.8, 75),
('Ballard', 'Seattle', 'WA', '98107', 82000.00, 22000, 1.5, 8.2, 82),
('Queen Anne', 'Seattle', 'WA', '98109', 105000.00, 12000, 0.9, 9.2, 90),
('Bellevue Downtown', 'Bellevue', 'WA', '98004', 120000.00, 8000, 0.7, 9.5, 65),
('Redmond', 'Redmond', 'WA', '98052', 135000.00, 25000, 0.8, 9.8, 45),
('Kirkland', 'Kirkland', 'WA', '98033', 95000.00, 15000, 1.0, 9.1, 60);

-- Insert property types
INSERT INTO property_types (type_name, description) VALUES
('Single Family', 'Detached single-family home'),
('Condo', 'Condominium unit'),
('Townhouse', 'Townhouse or row house'),
('Apartment', 'Apartment unit'),
('Duplex', 'Two-unit building'),
('Land', 'Vacant land for development');

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_property_listings_neighborhood ON property_listings(neighborhood_id);
CREATE INDEX IF NOT EXISTS idx_property_listings_active ON property_listings(is_active);
CREATE INDEX IF NOT EXISTS idx_property_listings_date ON property_listings(listing_date);
CREATE INDEX IF NOT EXISTS idx_price_changes_property ON price_changes(property_id);
CREATE INDEX IF NOT EXISTS idx_price_changes_date ON price_changes(change_date);
CREATE INDEX IF NOT EXISTS idx_interactions_property ON property_interactions(property_id);
CREATE INDEX IF NOT EXISTS idx_interactions_date ON property_interactions(interaction_date);
CREATE INDEX IF NOT EXISTS idx_interactions_session ON property_interactions(session_id);

-- Create views for common queries
CREATE OR REPLACE VIEW active_listings AS
SELECT 
    pl.*,
    n.name as neighborhood_name,
    n.city,
    n.state,
    pt.type_name as property_type
FROM property_listings pl
JOIN neighborhoods n ON pl.neighborhood_id = n.id
JOIN property_types pt ON pl.property_type_id = pt.id
WHERE pl.is_active = TRUE;

CREATE OR REPLACE VIEW recent_price_changes AS
SELECT 
    pc.*,
    pl.neighborhood_id,
    n.name as neighborhood_name,
    (pc.new_price - pc.old_price) as price_change,
    ((pc.new_price - pc.old_price) / pc.old_price * 100) as price_change_pct
FROM price_changes pc
JOIN property_listings pl ON pc.property_id = pl.property_id
JOIN neighborhoods n ON pl.neighborhood_id = n.id
WHERE pc.change_date >= CURRENT_DATE - INTERVAL '30 days';


ALTER USER postgres WITH REPLICATION;

-- Create publication for Debezium
CREATE PUBLICATION debezium_publication FOR ALL TABLES;

-- Create replication slot (optional, Debezium can create it automatically)
-- SELECT pg_create_logical_replication_slot('debezium_slot', 'pgoutput');

-- Example tables for CDC (adjust based on your schema)
CREATE TABLE IF NOT EXISTS properties (
    id SERIAL PRIMARY KEY,
    address VARCHAR(255),
    price DECIMAL(10,2),
    bedrooms INTEGER,
    bathrooms INTEGER,
    square_feet INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS price_history (
    id SERIAL PRIMARY KEY,
    property_id INTEGER REFERENCES properties(id),
    old_price DECIMAL(10,2),
    new_price DECIMAL(10,2),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_properties_updated_at 
    BEFORE UPDATE ON properties 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();