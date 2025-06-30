package com.flink.realestate.functions;

import com.flink.realestate.models.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;

import java.time.LocalDateTime;
public class InteractionCountAggregator implements AggregateFunction<PropertyInteraction, Long, Tuple2<String, Long>> {
    @Override public Long createAccumulator() { return 0L; }
    @Override public Long add(PropertyInteraction interaction, Long acc) { return acc + 1; }
    @Override public Tuple2<String, Long> getResult(Long acc) { return new Tuple2<>("", acc); }
    @Override public Long merge(Long a, Long b) { return a + b; }
}
