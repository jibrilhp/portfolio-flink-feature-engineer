package com.flink.realestate.functions;

import com.flink.realestate.models.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;

import java.time.LocalDateTime;

public class PriceAlertMapper implements MapFunction<PriceChange, PriceAlert> {
    @Override
    public PriceAlert map(PriceChange change) throws Exception {
        String alertLevel;
        double absChange = Math.abs(change.getChangePercentage());
        if (absChange > 20) alertLevel = "HIGH";
        else if (absChange > 15) alertLevel = "MEDIUM";
        else alertLevel = "LOW";
        return new PriceAlert(change.getPropertyId(), change.getChangePercentage(), change.getChangeReason(), alertLevel, change.getTimestamp());
    }
}
