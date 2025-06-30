package com.flink.realestate;

import com.flink.realestate.functions.*;
import com.flink.realestate.models.*;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

import java.util.Properties;

public class RealEstateStreamingApp {
    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1); // Set for testing, increase for production

        Properties kafkaProps = new Properties();
        kafkaProps.setProperty("bootstrap.servers", "broker:9092");
        kafkaProps.setProperty("group.id", "flink-realestate-group");
        kafkaProps.setProperty("auto.offset.reset", "latest");

        DataStream<String> propertyListingsStream = env
            .addSource(new FlinkKafkaConsumer<>("property-listings", new SimpleStringSchema(), kafkaProps))
            .name("Property Listings Source");
        DataStream<String> priceChangesStream = env
            .addSource(new FlinkKafkaConsumer<>("price-changes", new SimpleStringSchema(), kafkaProps))
            .name("Price Changes Source");
        DataStream<String> interactionsStream = env
            .addSource(new FlinkKafkaConsumer<>("property-interactions", new SimpleStringSchema(), kafkaProps))
            .name("Property Interactions Source");
        DataStream<String> marketEventsStream = env
            .addSource(new FlinkKafkaConsumer<>("market-events", new SimpleStringSchema(), kafkaProps))
            .name("Market Events Source");

        DataStream<PropertyListing> parsedListings = propertyListingsStream
            .map(new PropertyListingParser())
            .name("Parse Property Listings");
        DataStream<PriceChange> parsedPriceChanges = priceChangesStream
            .map(new PriceChangeParser())
            .name("Parse Price Changes");
        DataStream<PropertyInteraction> parsedInteractions = interactionsStream
            .map(new PropertyInteractionParser())
            .name("Parse Property Interactions");
        DataStream<MarketEvent> parsedMarketEvents = marketEventsStream
            .map(new MarketEventParser())
            .name("Parse Market Events");

        DataStream<Tuple2<String, PropertyStats>> neighborhoodStats = parsedListings
            .keyBy(PropertyListing::getNeighborhoodId)
            .window(TumblingProcessingTimeWindows.of(Time.minutes(5)))
            .aggregate(new PropertyStatsAggregator())
            .name("Neighborhood Property Stats");
        DataStream<PriceAlert> priceAlerts = parsedPriceChanges
            .filter(change -> Math.abs(change.getChangePercentage()) > 10.0)
            .map(new PriceAlertMapper())
            .name("Price Change Alerts");
        DataStream<Tuple2<String, Long>> hotProperties = parsedInteractions
            .keyBy(PropertyInteraction::getPropertyId)
            .window(TumblingProcessingTimeWindows.of(Time.minutes(10)))
            .aggregate(new InteractionCountAggregator())
            .filter(tuple -> tuple.f1 > 5)
            .name("Hot Properties Detection");
        DataStream<MarketTrend> marketTrends = parsedMarketEvents
            .keyBy(MarketEvent::getNeighborhoodId)
            .window(TumblingProcessingTimeWindows.of(Time.hours(1)))
            .aggregate(new MarketTrendAggregator())
            .name("Market Trend Analysis");

        parsedListings.print("NEW_LISTING");
        priceAlerts.print("PRICE_ALERT");
        hotProperties.print("HOT_PROPERTY");
        neighborhoodStats.print("NEIGHBORHOOD_STATS");
        marketTrends.print("MARKET_TREND");

        env.execute("Real Estate Streaming Analytics POC");
    }
}
