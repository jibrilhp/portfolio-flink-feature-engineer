package com.flink.poc;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

import java.util.Properties;

public class App {
    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "broker:29092");
        properties.setProperty("group.id", "flink-property-group");

        FlinkKafkaConsumer<String> consumer = new FlinkKafkaConsumer<>(
                "property-listings",
                new SimpleStringSchema(),
                properties);

        env.addSource(consumer)
                .name("Kafka Property Listings Source")
                .print();

        env.execute("Flink Property Listings Consumer");
    }
}
