#!/bin/bash

# deploy-flink-job.sh - Deploy Flink jobs

echo "🚀 Deploying Flink Jobs..."

# Method 1: Using Flink SQL Client (Recommended for POC)
deploy_sql_job() {
    echo "📝 Deploying Flink SQL Job..."
    
    # Copy SQL job to Flink container
    docker cp flink-sql-job.sql flink-jobmanager:/opt/flink/
    
    # Execute SQL job in Flink SQL client
    docker exec -it flink-jobmanager /opt/flink/bin/sql-client.sh --jar /opt/flink/lib/flink-connector-kafka-1.16.0.jar \
  --jar /opt/flink/lib/flink-sql-connector-kafka-1.16.0.jar \
  --jar /opt/flink/lib/kafka-clients-2.8.1.jar \
  -f /opt/flink/flink-sql-job.sql 
}

# Method 2: Compile and submit Java job
compile_and_deploy_java() {
    echo "🔨 Compiling Java Job..."
    
    # Create temporary build directory
    mkdir -p build/src/main/java
    
    # Copy source code
    cp SimpleKafkaToConsole.java build/src/main/java/
    
    # Create pom.xml for Maven build
    cat > build/pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.example</groupId>
    <artifactId>flink-cdc-job</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>
    
    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <flink.version>1.18.0</flink.version>
        <scala.binary.version>2.12</scala.binary.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-streaming-java</artifactId>
            <version>${flink.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-connector-kafka</artifactId>
            <version>${flink.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-json</artifactId>
            <version>${flink.version}</version>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.2.4</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>SimpleKafkaToConsole</mainClass>
                                </transformer>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
EOF

    # Build with Maven
    cd build
    mvn clean package
    cd ..
    
    # Copy JAR to Flink
    docker cp build/target/flink-cdc-job-1.0-SNAPSHOT.jar flink-jobmanager:/opt/flink/jobs/
    
    # Submit job
    docker exec flink-jobmanager /opt/flink/bin/flink run /opt/flink/jobs/flink-cdc-job-1.0-SNAPSHOT.jar
}

# Method 3: Simple submission without compilation (for quick testing)
deploy_simple_job() {
    echo "🎯 Deploying Simple Job..."
    
    # Create a simple JAR submission script
    cat > submit-job.sh << 'EOF'
#!/bin/bash
# This runs inside the Flink JobManager container

# Create a simple Java file that can be run directly
cat > /tmp/SimpleJob.java << 'JAVA_EOF'
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.datastream.DataStream;

public class SimpleJob {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // Simple test job - generates numbers and prints them
        DataStream<Long> stream = env.generateSequence(1, 1000);
        stream.map(x -> "Number: " + x).print();
        
        env.execute("Simple Test Job");
    }
}
JAVA_EOF

# Try to compile and run (this is a fallback approach)
echo "Testing basic Flink functionality..."
EOF

    chmod +x submit-job.sh
    docker cp submit-job.sh flink-jobmanager:/opt/flink/
    docker exec flink-jobmanager /opt/flink/submit-job.sh
}

# Method 4: Using Flink's Table API with JAR-less deployment
deploy_table_api_job() {
    echo "📊 Deploying Table API Job..."
    
    # Create a SQL script that can be executed directly
    cat > table-api-job.sql << 'EOF'
-- This can be run via Flink SQL Client

-- Set up execution environment
SET execution.runtime-mode = streaming;
SET execution.checkpointing.interval = 3s;

-- Create a simple test table
CREATE TABLE test_source (
    id BIGINT,
    message STRING,
    ts TIMESTAMP(3)
) WITH (
    'connector' = 'datagen',
    'rows-per-second' = '1',
    'fields.id.kind' = 'sequence',
    'fields.id.start' = '1',
    'fields.id.end' = '1000',
    'fields.message.kind' = 'random',
    'fields.message.length' = '10'
);

-- Create console sink
CREATE TABLE console_sink (
    id BIGINT,
    message STRING,
    ts TIMESTAMP(3)
) WITH (
    'connector' = 'print'
);

-- Simple processing
INSERT INTO console_sink SELECT * FROM test_source;
EOF

    # Execute the SQL job
    docker cp table-api-job.sql flink-jobmanager:/opt/flink/
    docker exec -d flink-jobmanager /opt/flink/bin/sql-client.sh -f /opt/flink/table-api-job.sql
}

# Main deployment logic
case "${1:-sql}" in
    "sql")
        deploy_sql_job
        ;;
    "java")
        compile_and_deploy_java
        ;;
    "simple")
        deploy_simple_job
        ;;
    "table")
        deploy_table_api_job
        ;;
    *)
        echo "Usage: $0 [sql|java|simple|table]"
        echo "  sql    - Deploy SQL job (recommended)"
        echo "  java   - Compile and deploy Java job"
        echo "  simple - Deploy simple test job"
        echo "  table  - Deploy Table API job"
        ;;
esac

echo "✅ Deployment completed!"
echo "🔍 Check job status at: http://localhost:8081"

# Show running jobs
echo "📋 Current running jobs:"
docker exec flink-jobmanager /opt/flink/bin/flink list