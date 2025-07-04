package com.example.beam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.bigquery.model.TableRow;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.kafka.KafkaIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.StreamingOptions;
import org.apache.beam.sdk.transforms.Filter;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.TypeDescriptor;

public class MarketEventPipeline {
    public static void main(String[] args) {
        StreamingOptions options = PipelineOptionsFactory.fromArgs(args).withValidation().as(StreamingOptions.class);
        options.setStreaming(true);
        Pipeline p = Pipeline.create(options);

        String bootstrapServers = "localhost:9092";
        String kafkaTopic = "market-events";
        String bigQueryTable = "your-project:your_dataset.market_event"; // Change as needed

        ObjectMapper mapper = new ObjectMapper();

        p.apply("ReadFromKafka", KafkaIO.<String, String>read()
                .withBootstrapServers(bootstrapServers)
                .withTopic(kafkaTopic)
                .withKeyDeserializer(org.apache.kafka.common.serialization.StringDeserializer.class)
                .withValueDeserializer(org.apache.kafka.common.serialization.StringDeserializer.class)
                .withoutMetadata()
        )
        .apply("JsonToTableRow", MapElements.into(TypeDescriptor.of(TableRow.class))
            .via((KV<String, String> msg) -> {
                try {
                    return mapper.readValue(msg.getValue(), TableRow.class);
                } catch (Exception e) {
                    System.err.println("Failed to parse message: " + e.getMessage());
                    return null;
                }
            })
        )
        .apply("DropNulls", Filter.by((TableRow row) -> row != null))
        .apply("WriteToBigQuery", BigQueryIO.writeTableRows()
            .to(bigQueryTable)
            .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
            .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
        );

        p.run().waitUntilFinish();
    }
}
