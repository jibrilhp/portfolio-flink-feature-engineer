package com.flink.realestate.functions;

import com.flink.realestate.models.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;

import java.time.LocalDateTime;

public class PropertyInteractionParser implements MapFunction<String, PropertyInteraction> {
    private static final ObjectMapper mapper = new ObjectMapper();
    static { mapper.registerModule(new JavaTimeModule()); }
    @Override
    public PropertyInteraction map(String json) throws Exception {
        return mapper.readValue(json, PropertyInteraction.class);
    }
}
