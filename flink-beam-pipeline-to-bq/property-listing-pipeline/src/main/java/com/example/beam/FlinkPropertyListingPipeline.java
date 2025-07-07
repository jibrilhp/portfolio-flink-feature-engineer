package com.example.beam;

import org.apache.beam.runners.flink.FlinkPipelineOptions;
import org.apache.beam.runners.flink.FlinkRunner;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.kafka.KafkaIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.values.KV;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class FlinkPropertyListingPipeline {
    public static void main(String[] args) {
        // Ensure we have the required arguments
        List<String> argsList = new ArrayList<>(Arrays.asList(args));

        // Add default arguments if not present
        if (!argsList.contains("--runner=FlinkRunner") &&
                !argsList.stream().anyMatch(arg -> arg.startsWith("--runner="))) {
            argsList.add("--runner=FlinkRunner");
        }

        if (!argsList.contains("--streaming=true") &&
                !argsList.stream().anyMatch(arg -> arg.startsWith("--streaming="))) {
            argsList.add("--streaming=true");
        }

        String[] finalArgs = argsList.toArray(new String[0]);

        // Create and configure Flink pipeline options
        FlinkPipelineOptions options = PipelineOptionsFactory.fromArgs(finalArgs)
                .withValidation()
                .as(FlinkPipelineOptions.class);

        // Explicitly set the runner (backup)
        options.setRunner(FlinkRunner.class);
        options.setStreaming(true);
        options.setParallelism(1);
        options.setMaxParallelism(128);

        // Debug output
        System.out.println("Using runner: " + options.getRunner().getName());
        System.out.println("Streaming mode: " + options.isStreaming());
        System.out.println("Parallelism: " + options.getParallelism());

        Pipeline p = Pipeline.create(options);

        String bootstrapServers = "broker:29092";
        String kafkaTopic = "property-listings";

        p.apply("ReadFromKafka", KafkaIO.<String, String>read()
                .withBootstrapServers(bootstrapServers)
                .withTopic(kafkaTopic)
                .withKeyDeserializer(org.apache.kafka.common.serialization.StringDeserializer.class)
                .withValueDeserializer(org.apache.kafka.common.serialization.StringDeserializer.class)
                .withoutMetadata())
                .apply("PrintMessages", MapElements.via(new SimpleFunction<KV<String, String>, String>() {
                    @Override
                    public String apply(KV<String, String> input) {
                        System.out.println("Received: " + input.getValue());
                        return input.getValue();
                    }
                }));

        p.run().waitUntilFinish();
    }
}