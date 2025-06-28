# flink-job-submitter/Dockerfile
FROM flink:1.18-scala_2.12-java17
COPY submit.sh /submit.sh
RUN chmod +x /submit.sh
CMD ["/submit.sh"]
