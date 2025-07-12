import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.types.Row;

public class CDCToBigQueryJob {

    public static void main(String[] args) throws Exception {
        // Set up the streaming execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        // Configure Kafka source
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("kafka:29092")
                .setTopics("cdc.public.users", "cdc.public.orders", "cdc.public.products")
                .setGroupId("flink-bigquery-consumer")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // Create data stream from Kafka
        DataStream<String> kafkaStream = env.fromSource(source,
                WatermarkStrategy.noWatermarks(), "Kafka Source");

        // Process CDC events
        DataStream<Row> processedStream = kafkaStream
                .map(new CDCEventProcessor())
                .filter(row -> row != null); // Filter out null rows

        // Register the stream as a table
        Table cdcTable = tableEnv.fromDataStream(processedStream);
        tableEnv.createTemporaryView("cdc_events", cdcTable);

        // Create BigQuery sink table
        tableEnv.executeSql(
                "CREATE TABLE bigquery_sink (" +
                        "  table_name STRING," +
                        "  operation STRING," +
                        "  record_id STRING," +
                        "  data STRING," +
                        "  timestamp_ms BIGINT," +
                        "  processing_time AS PROCTIME()" +
                        ") WITH (" +
                        "  'connector' = 'bigquery'," +
                        "  'project' = 'data-dev-270120'," +
                        "  'dataset' = 'playground'," +
                        "  'table' = 'cdc_events'," +
                        "  'credentials.path' = '/opt/flink/gcp-credentials/service-account.json'" +
                        ")");

        // Insert processed data into BigQuery
        tableEnv.executeSql(
                "INSERT INTO bigquery_sink " +
                        "SELECT table_name, operation, record_id, data, timestamp_ms " +
                        "FROM cdc_events");

        // Execute the job
        env.execute("CDC to BigQuery Streaming Job");
    }

    // Custom map function to process CDC events
    public static class CDCEventProcessor implements MapFunction<String, Row> {
        private static final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public Row map(String value) throws Exception {
            try {
                JsonNode jsonNode = objectMapper.readTree(value);

                // Extract CDC event information
                String tableName = extractTableName(jsonNode);
                String operation = jsonNode.has("__op") ? jsonNode.get("__op").asText() : "unknown";
                String recordId = extractRecordId(jsonNode);
                String data = jsonNode.toString();
                long timestampMs = jsonNode.has("__ts_ms") ? jsonNode.get("__ts_ms").asLong()
                        : System.currentTimeMillis();

                return Row.of(tableName, operation, recordId, data, timestampMs);

            } catch (Exception e) {
                System.err.println("Error processing CDC event: " + e.getMessage());
                return null;
            }
        }

        private String extractTableName(JsonNode jsonNode) {
            // Extract table name from the CDC event
            if (jsonNode.has("__source") && jsonNode.get("__source").has("table")) {
                return jsonNode.get("__source").get("table").asText();
            }
            return "unknown";
        }

        private String extractRecordId(JsonNode jsonNode) {
            // Try to extract ID field
            if (jsonNode.has("id")) {
                return jsonNode.get("id").asText();
            }
            return "unknown";
        }
    }
}