import json
import random
import time
import uuid
from datetime import datetime
from typing import Dict
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
        self.kafka_servers = os.getenv('KAFKA_BOOTSTRAP_SERVERS', 'broker:29092')
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

        self.neighborhoods = []
        self.property_types = []
        self.active_properties = []
        self.user_sessions = {}

        self._load_reference_data()
        self._generate_initial_properties()

    def _get_db_connection(self):
        return psycopg2.connect(**self.db_config)

    def _load_reference_data(self):
        try:
            with self._get_db_connection() as conn:
                with conn.cursor(cursor_factory=RealDictCursor) as cur:
                    cur.execute("SELECT * FROM neighborhoods")
                    self.neighborhoods = [dict(row) for row in cur.fetchall()]
                    cur.execute("SELECT * FROM property_types")
                    self.property_types = [dict(row) for row in cur.fetchall()]
            logger.info(f"Loaded {len(self.neighborhoods)} neighborhoods and {len(self.property_types)} property types")
        except Exception as e:
            logger.error(f"Error loading reference data: {e}")
            raise

    def _generate_initial_properties(self):
        try:
            with self._get_db_connection() as conn:
                with conn.cursor() as cur:
                    cur.execute("DELETE FROM property_listings")
                    for _ in range(500):
                        property_data = self._generate_property_listing()
                        cur.execute("""
                            INSERT INTO property_listings 
                            (property_id, neighborhood_id, property_type_id, price, bedrooms, 
                             bathrooms, square_feet, lot_size_sqft, year_built, latitude, longitude)
                            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                        """, (
                            property_data['propertyId'],
                            property_data['neighborhoodId'],
                            property_data['propertyTypeId'],
                            property_data['price'],
                            property_data['bedrooms'],
                            property_data['bathrooms'],
                            property_data['squareFeet'],
                            property_data['lotSizeSquareFeet'],
                            property_data['yearBuilt'],
                            property_data['latitude'],
                            property_data['longitude']
                        ))
                        self.active_properties.append(property_data['propertyId'])
                    conn.commit()
                    logger.info(f"Generated {len(self.active_properties)} initial properties")
        except Exception as e:
            logger.error(f"Error generating initial properties: {e}")
            raise

    def _generate_property_listing(self) -> Dict:
        neighborhood = random.choice(self.neighborhoods)
        property_type = random.choice(self.property_types)
        base_price = float(neighborhood['avg_income']) * random.uniform(4, 8)

        if property_type['type_name'] == 'Single Family':
            bedrooms = random.randint(2, 5)
            bathrooms = random.uniform(1.5, 3.5)
            square_feet = random.randint(1200, 4000)
            lot_size_square_feet = random.randint(3000, 12000)
        elif property_type['type_name'] == 'Condo':
            bedrooms = random.randint(1, 3)
            bathrooms = random.uniform(1, 2.5)
            square_feet = random.randint(600, 2000)
            lot_size_square_feet = 0
        elif property_type['type_name'] == 'Townhouse':
            bedrooms = random.randint(2, 4)
            bathrooms = random.uniform(1.5, 3)
            square_feet = random.randint(1000, 2500)
            lot_size_square_feet = random.randint(1000, 3000)
        else:
            bedrooms = random.randint(1, 4)
            bathrooms = random.uniform(1, 2.5)
            square_feet = random.randint(500, 1800)
            lot_size_square_feet = random.randint(0, 2000)

        price_per_sqft = base_price / square_feet
        price_per_sqft *= random.uniform(0.8, 1.2)
        final_price = price_per_sqft * square_feet

        base_lat = 47.6062
        base_lon = -122.3321
        latitude = base_lat + random.uniform(-0.2, 0.2)
        longitude = base_lon + random.uniform(-0.3, 0.3)

        return {
            'propertyId': f"PROP_{uuid.uuid4().hex[:8].upper()}",
            'neighborhoodId': neighborhood['id'],
            'propertyTypeId': property_type['id'],
            'price': round(final_price, 2),
            'bedrooms': bedrooms,
            'bathrooms': round(bathrooms, 1),
            'squareFeet': square_feet,
            'lotSizeSquareFeet': lot_size_square_feet,
            'yearBuilt': random.randint(1950, 2023),
            'latitude': round(latitude, 6),
            'longitude': round(longitude, 6),
            'timestamp': datetime.now().isoformat()
        }

    def generate_new_listing(self):
        property_data = self._generate_property_listing()
        self.producer.send('property-listings', key=property_data['propertyId'], value=property_data)
        self.active_properties.append(property_data['propertyId'])
        logger.info(f"Generated new listing: {property_data['propertyId']}")

    def generate_price_change(self):
        if not self.active_properties:
            return
        property_id = random.choice(self.active_properties)
        try:
            with self._get_db_connection() as conn:
                with conn.cursor() as cur:
                    cur.execute("SELECT price FROM property_listings WHERE property_id = %s", (property_id,))
                    result = cur.fetchone()
                    if not result:
                        return
                    current_price = float(result[0])
        except Exception as e:
            logger.error(f"Error getting current price: {e}")
            return

        change_pct = random.uniform(-0.10, 0.15)
        new_price = current_price * (1 + change_pct)

        price_change_data = {
            'propertyId': property_id,
            'oldPrice': current_price,
            'newPrice': round(new_price, 2),
            'changeAmount': round(new_price - current_price, 2),
            'changePercentage': round(change_pct * 100, 2),
            'changeReason': random.choice([
                'market_adjustment', 'seller_motivated', 'appraisal_update',
                'competitive_pricing', 'seasonal_adjustment', 'negotiation'
            ]),
            'timestamp': datetime.now().isoformat()
        }

        self.producer.send('price-changes', key=property_id, value=price_change_data)
        logger.info(f"Price change for {property_id}: {change_pct:.2%}")

    def generate_property_interaction(self):
        if not self.active_properties:
            return

        property_id = random.choice(self.active_properties)

        if random.random() < 0.3 and self.user_sessions:
            user_id = random.choice(list(self.user_sessions.keys()))
            session_id = self.user_sessions[user_id]
        else:
            user_id = f"USER_{uuid.uuid4().hex[:8]}"
            session_id = f"SESSION_{uuid.uuid4().hex[:8]}"
            self.user_sessions[user_id] = session_id

        interaction_data = {
            'propertyId': property_id,
            'userId': user_id,
            'sessionId': session_id,
            'interactionType': random.choice([
                'view', 'view', 'view', 'view', 'view',
                'favorite', 'contact', 'schedule_tour', 'share'
            ]),
            'deviceType': random.choice(['desktop', 'mobile', 'tablet']),
            'source': random.choice(['web', 'mobile_app', 'agent', 'email']),
            'timestamp': datetime.now().isoformat()
        }

        self.producer.send('property-interactions', key=property_id, value=interaction_data)
        logger.debug(f"Interaction: {interaction_data['interactionType']} on {property_id}")

    def generate_market_event(self):
        neighborhood = random.choice(self.neighborhoods)
        event_data = {
            'eventType': random.choice([
                'interest_rate_change', 'new_development_announced',
                'school_rating_update', 'transportation_improvement',
                'economic_indicator_change', 'seasonal_trend'
            ]),
            'neighborhoodId': neighborhood['id'],
            'neighborhoodName': neighborhood['name'],
            'impactScore': random.uniform(-1.0, 1.0),
            'description': f"Market event in {neighborhood['name']}",
            'timestamp': datetime.now().isoformat()
        }

        self.producer.send('market-events', key=str(neighborhood['id']), value=event_data)
        logger.info(f"Market event in {neighborhood['name']}: {event_data['eventType']}")

    def run_continuous_generation(self):
        logger.info("Starting continuous data generation...")
        while True:
            try:
                rand = random.random()
                if rand < 0.4:
                    self.generate_property_interaction()
                elif rand < 0.6:
                    self.generate_price_change()
                elif rand < 0.8:
                    self.generate_new_listing()
                else:
                    self.generate_market_event()
                time.sleep(random.uniform(1, 5))
            except Exception as e:
                logger.error(f"Error in continuous generation: {e}")
                time.sleep(10)

    def run_batch_generation(self):
        logger.info("Starting batch data generation...")
        while True:
            try:
                batch_size = random.randint(5, 20)
                logger.info(f"Generating batch of {batch_size} interactions")
                for _ in range(batch_size):
                    self.generate_property_interaction()
                    time.sleep(0.1)
                time.sleep(random.uniform(30, 120))
            except Exception as e:
                logger.error(f"Error in batch generation: {e}")
                time.sleep(30)

def main():
    logger.info("Initializing Real Estate Data Generator...")
    time.sleep(30)
    try:
        generator = RealEstateDataGenerator()
        continuous_thread = threading.Thread(target=generator.run_continuous_generation)
        continuous_thread.daemon = True
        continuous_thread.start()
        batch_thread = threading.Thread(target=generator.run_batch_generation)
        batch_thread.daemon = True
        batch_thread.start()
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
