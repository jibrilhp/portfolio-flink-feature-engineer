import json
import random
import time
import uuid
from datetime import datetime, timedelta
from typing import Dict, List
import threading
import logging
import os

from kafka import KafkaProducer
import psycopg2
from psycopg2.extras import RealDictCursor

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class RealEstateDataGenerator:
    def __init__(self):
        # Kafka configuration
        self.kafka_servers = os.getenv('KAFKA_BOOTSTRAP_SERVERS', 'localhost:9092')
        self.producer = KafkaProducer(
            bootstrap_servers=self.kafka_servers,
            value_serializer=lambda v: json.dumps(v).encode('utf-8'),
            key_serializer=lambda k: str(k).encode('utf-8')
        )
        
        # PostgreSQL configuration
        self.db_config = {
            'host': os.getenv('POSTGRES_HOST', 'localhost'),
            'database': os.getenv('POSTGRES_DB', 'real_estate'),
            'user': os.getenv('POSTGRES_USER', 'postgres'),
            'password': os.getenv('POSTGRES_PASSWORD', 'postgres'),
            'port': 5432
        }
        
        # Initialize reference data
        self.neighborhoods = []
        self.property_types = []
        self.active_properties = []
        self.user_sessions = {}
        
        # Load reference data
        self._load_reference_data()
        
        # Generate initial property listings
        self._generate_initial_properties()
    
    def _get_db_connection(self):
        """Get database connection"""
        return psycopg2.connect(**self.db_config)
    
    def _load_reference_data(self):
        """Load neighborhoods and property types from database"""
        try:
            with self._get_db_connection() as conn:
                with conn.cursor(cursor_factory=RealDictCursor) as cur:
                    # Load neighborhoods
                    cur.execute("SELECT * FROM neighborhoods")
                    self.neighborhoods = [dict(row) for row in cur.fetchall()]
                    
                    # Load property types
                    cur.execute("SELECT * FROM property_types")
                    self.property_types = [dict(row) for row in cur.fetchall()]
                    
            logger.info(f"Loaded {len(self.neighborhoods)} neighborhoods and {len(self.property_types)} property types")
        except Exception as e:
            logger.error(f"Error loading reference data: {e}")
            raise
    
    def _generate_initial_properties(self):
        """Generate initial property listings"""
        try:
            with self._get_db_connection() as conn:
                with conn.cursor() as cur:
                    # Clear existing listings
                    cur.execute("DELETE FROM property_listings")
                    
                    # Generate 500 initial properties
                    for _ in range(500):
                        property_data = self._generate_property_listing()
                        
                        cur.execute("""
                            INSERT INTO property_listings 
                            (property_id, neighborhood_id, property_type_id, price, bedrooms, 
                             bathrooms, square_feet, lot_size_sqft, year_built, latitude, longitude)
                            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                        """, (
                            property_data['property_id'],
                            property_data['neighborhood_id'],
                            property_data['property_type_id'],
                            property_data['price'],
                            property_data['bedrooms'],
                            property_data['bathrooms'],
                            property_data['square_feet'],
                            property_data['lot_size_sqft'],
                            property_data['year_built'],
                            property_data['latitude'],
                            property_data['longitude']
                        ))
                        
                        self.active_properties.append(property_data['property_id'])
                    
                    conn.commit()
                    logger.info(f"Generated {len(self.active_properties)} initial properties")
        except Exception as e:
            logger.error(f"Error generating initial properties: {e}")
            raise
    
    def _generate_property_listing(self) -> Dict:
        """Generate a single property listing"""
        neighborhood = random.choice(self.neighborhoods)
        property_type = random.choice(self.property_types)
        
        # Base price influenced by neighborhood income
        base_price = neighborhood['avg_income'] * random.uniform(4, 8)
        
        # Property characteristics
        if property_type['type_name'] == 'Single Family':
            bedrooms = random.randint(2, 5)
            bathrooms = random.uniform(1.5, 3.5)
            square_feet = random.randint(1200, 4000)
            lot_size = random.randint(3000, 12000)
        elif property_type['type_name'] == 'Condo':
            bedrooms = random.randint(1, 3)
            bathrooms = random.uniform(1, 2.5)
            square_feet = random.randint(600, 2000)
            lot_size = 0
        elif property_type['type_name'] == 'Townhouse':
            bedrooms = random.randint(2, 4)
            bathrooms = random.uniform(1.5, 3)
            square_feet = random.randint(1000, 2500)
            lot_size = random.randint(1000, 3000)
        else:
            bedrooms = random.randint(1, 4)
            bathrooms = random.uniform(1, 2.5)
            square_feet = random.randint(500, 1800)
            lot_size = random.randint(0, 2000)
        
        # Adjust price based on characteristics
        price_per_sqft = base_price / square_feet
        price_per_sqft *= random.uniform(0.8, 1.2)  # Add some variance
        final_price = price_per_sqft * square_feet
        
        # Add location coordinates (mock around Seattle area)
        base_lat = 47.6062
        base_lon = -122.3321
        latitude = base_lat + random.uniform(-0.2, 0.2)
        longitude = base_lon + random.uniform(-0.3, 0.3)
        
        return {
            'property_id': f"PROP_{uuid.uuid4().hex[:8].upper()}",
            'neighborhood_id': neighborhood['id'],
            'property_type_id': property_type['id'],
            'price': round(final_price, 2),
            'bedrooms': bedrooms,
            'bathrooms': round(bathrooms, 1),
            'square_feet': square_feet,
            'lot_size_sqft': lot_size,
            'year_built': random.randint(1950, 2023),
            'latitude': round(latitude, 6),
            'longitude': round(longitude, 6),
            'timestamp': datetime.now().isoformat()
        }
    
    def generate_new_listing(self):
        """Generate and send a new property listing"""
        property_data = self._generate_property_listing()
        
        # Send to Kafka
        self.producer.send('property-listings', 
                          key=property_data['property_id'],
                          value=property_data)
        
        # Add to active properties
        self.active_properties.append(property_data['property_id'])
        
        logger.info(f"Generated new listing: {property_data['property_id']}")
    
    def generate_price_change(self):
        """Generate a price change event"""
        if not self.active_properties:
            return
        
        property_id = random.choice(self.active_properties)
        
        # Get current price from database
        try:
            with self._get_db_connection() as conn:
                with conn.cursor() as cur:
                    cur.execute("SELECT price FROM property_listings WHERE property_id = %s", 
                              (property_id,))
                    result = cur.fetchone()
                    if not result:
                        return
                    
                    current_price = float(result[0])
        except Exception as e:
            logger.error(f"Error getting current price: {e}")
            return
        
        # Generate price change (-10% to +15%)
        change_pct = random.uniform(-0.10, 0.15)
        new_price = current_price * (1 + change_pct)
        
        price_change_data = {
            'property_id': property_id,
            'old_price': current_price,
            'new_price': round(new_price, 2),
            'change_amount': round(new_price - current_price, 2),
            'change_percentage': round(change_pct * 100, 2),
            'change_reason': random.choice([
                'market_adjustment', 'seller_motivated', 'appraisal_update',
                'competitive_pricing', 'seasonal_adjustment', 'negotiation'
            ]),
            'timestamp': datetime.now().isoformat()
        }
        
        # Send to Kafka
        self.producer.send('price-changes',
                          key=property_id,
                          value=price_change_data)
        
        logger.info(f"Price change for {property_id}: {change_pct:.2%}")
    
    def generate_property_interaction(self):
        """Generate user interaction with property"""
        if not self.active_properties:
            return
        
        property_id = random.choice(self.active_properties)
        
        # Generate or reuse user session
        if random.random() < 0.3 and self.user_sessions:  # 30% chance to reuse session
            user_id = random.choice(list(self.user_sessions.keys()))
            session_id = self.user_sessions[user_id]
        else:
            user_id = f"USER_{uuid.uuid4().hex[:8]}"
            session_id = f"SESSION_{uuid.uuid4().hex[:8]}"
            self.user_sessions[user_id] = session_id
        
        interaction_data = {
            'property_id': property_id,
            'user_id': user_id,
            'session_id': session_id,
            'interaction_type': random.choice([
                'view', 'view', 'view', 'view', 'view',  # Views are most common
                'favorite', 'contact', 'schedule_tour', 'share'
            ]),
            'device_type': random.choice(['desktop', 'mobile', 'tablet']),
            'source': random.choice(['web', 'mobile_app', 'agent', 'email']),
            'timestamp': datetime.now().isoformat()
        }
        
        # Send to Kafka
        self.producer.send('property-interactions',
                          key=property_id,
                          value=interaction_data)
        
        logger.debug(f"Interaction: {interaction_data['interaction_type']} on {property_id}")
    
    def generate_market_event(self):
        """Generate market-level events"""
        neighborhood = random.choice(self.neighborhoods)
        
        event_data = {
            'event_type': random.choice([
                'interest_rate_change', 'new_development_announced',
                'school_rating_update', 'transportation_improvement',
                'economic_indicator_change', 'seasonal_trend'
            ]),
            'neighborhood_id': neighborhood['id'],
            'neighborhood_name': neighborhood['name'],
            'impact_score': random.uniform(-1.0, 1.0),  # -1 negative, +1 positive
            'description': f"Market event in {neighborhood['name']}",
            'timestamp': datetime.now().isoformat()
        }
        
        # Send to Kafka
        self.producer.send('market-events',
                          key=str(neighborhood['id']),
                          value=event_data)
        
        logger.info(f"Market event in {neighborhood['name']}: {event_data['event_type']}")
    
    def run_continuous_generation(self):
        """Run continuous data generation"""
        logger.info("Starting continuous data generation...")
        
        while True:
            try:
                # Generate different types of events with different probabilities
                rand = random.random()
                
                if rand < 0.4:  # 40% - Property interactions (most frequent)
                    self.generate_property_interaction()
                elif rand < 0.6:  # 20% - Price changes
                    self.generate_price_change()
                elif rand < 0.8:  # 20% - New listings
                    self.generate_new_listing()
                else:  # 20% - Market events
                    self.generate_market_event()
                
                # Wait between events (1-5 seconds)
                time.sleep(random.uniform(1, 5))
                
            except Exception as e:
                logger.error(f"Error in continuous generation: {e}")
                time.sleep(10)  # Wait before retrying
    
    def run_batch_generation(self):
        """Run periodic batch generation"""
        logger.info("Starting batch data generation...")
        
        while True:
            try:
                # Generate multiple interactions in a batch (simulating busy periods)
                batch_size = random.randint(5, 20)
                logger.info(f"Generating batch of {batch_size} interactions")
                
                for _ in range(batch_size):
                    self.generate_property_interaction()
                    time.sleep(0.1)  # Small delay between batch items
                
                # Wait 30-120 seconds before next batch
                time.sleep(random.uniform(30, 120))
                
            except Exception as e:
                logger.error(f"Error in batch generation: {e}")
                time.sleep(30)

def main():
    """Main function to run the data generator"""
    logger.info("Initializing Real Estate Data Generator...")
    
    # Wait for services to be ready
    time.sleep(30)
    
    try:
        generator = RealEstateDataGenerator()
        
        # Start continuous generation in separate thread
        continuous_thread = threading.Thread(target=generator.run_continuous_generation)
        continuous_thread.daemon = True
        continuous_thread.start()
        
        # Start batch generation in separate thread
        batch_thread = threading.Thread(target=generator.run_batch_generation)
        batch_thread.daemon = True
        batch_thread.start()
        
        # Keep main thread alive
        while True:
            time.sleep(60)
            logger.info(f"Data generator running... Active properties: {len(generator.active_properties)}")
            
    except KeyboardInterrupt:
        logger.info("Shutting down data generator...")
    except Exception as e:
        logger.error(f"Fatal error: {e}")
        raise

if __name__ == "__main__":
    main()