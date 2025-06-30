package com.flink.realestate.functions;

import com.flink.realestate.models.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;

import java.time.LocalDateTime;
public class MarketTrendAggregator implements AggregateFunction<MarketEvent, MarketTrendAggregator.MarketTrendAccumulator, MarketTrend> {
    public static class MarketTrendAccumulator {
        public String neighborhoodId;
        public long count = 0;
        public double totalImpactScore = 0;
    }
    @Override
    public MarketTrendAccumulator createAccumulator() { return new MarketTrendAccumulator(); }
    @Override
    public MarketTrendAccumulator add(MarketEvent event, MarketTrendAccumulator acc) {
        acc.neighborhoodId = event.getNeighborhoodId();
        acc.count++;
        acc.totalImpactScore += event.getImpactScore();
        return acc;
    }
    @Override
    public MarketTrend getResult(MarketTrendAccumulator acc) {
        double avgImpact = acc.totalImpactScore / acc.count;
        String trend;
        if (avgImpact > 0.3) trend = "POSITIVE";
        else if (avgImpact < -0.3) trend = "NEGATIVE";
        else trend = "NEUTRAL";
        return new MarketTrend(acc.neighborhoodId, avgImpact, acc.count, trend, LocalDateTime.now());
    }
    @Override
    public MarketTrendAccumulator merge(MarketTrendAccumulator a, MarketTrendAccumulator b) {
        MarketTrendAccumulator merged = new MarketTrendAccumulator();
        merged.neighborhoodId = a.neighborhoodId;
        merged.count = a.count + b.count;
        merged.totalImpactScore = a.totalImpactScore + b.totalImpactScore;
        return merged;
    }
}
