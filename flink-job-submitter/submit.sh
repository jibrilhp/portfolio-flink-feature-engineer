#!/bin/sh

# Wait for the JobManager to be available
until curl -s http://jobmanager:8080/overview | grep -q '{"taskmanagers":.*'; do
  echo "Waiting for Flink JobManager..."
  sleep 5
done

echo "Submitting Flink job..."

# Submit the job
/opt/flink/bin/flink run \
    -d \
    /jobs/realestate-flink-1.0-SNAPSHOT.jar

echo "Job submitted."

# Keep container alive to view logs
tail -f /dev