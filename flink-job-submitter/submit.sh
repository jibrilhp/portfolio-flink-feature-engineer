#!/bin/sh

# Wait for the JobManager to be available
# until curl -s http://jobmanager:8081/overview | grep -q '{"taskmanagers":.*'; do
#   echo "Waiting for Flink JobManager..."
#   sleep 5
# done

echo "Submitting Flink job..."

exec /opt/flink/bin/flink run \
    -c com.example.beam.FlinkPropertyListingPipeline \
    -d \
    /jobs/property-listing-pipeline-bundled-1.0-SNAPSHOT.jar

echo "Job submitted."

tail -f /dev/null
