package com.flink.realestate.functions;

import com.flink.realestate.models.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;

import java.time.LocalDateTime;

public class PropertyStatsAggregator implements AggregateFunction<PropertyListing, PropertyStatsAggregator.PropertyStatsAccumulator, Tuple2<String, PropertyStats>> {
    public static class PropertyStatsAccumulator {
        public String neighborhoodId;
        public long count = 0;
        public double totalPrice = 0;
        public double totalSquareFeet = 0;
        public double minPrice = Double.MAX_VALUE;
        public double maxPrice = Double.MIN_VALUE;
    }
    @Override
    public PropertyStatsAccumulator createAccumulator() { return new PropertyStatsAccumulator(); }
    @Override
    public PropertyStatsAccumulator add(PropertyListing listing, PropertyStatsAccumulator acc) {
        acc.neighborhoodId = listing.getNeighborhoodId();
        acc.count++;
        acc.totalPrice += listing.getPrice();
        acc.totalSquareFeet += listing.getSquareFeet();
        if (acc.count == 1) {
            acc.minPrice = listing.getPrice();
            acc.maxPrice = listing.getPrice();
        } else {
            acc.minPrice = Math.min(acc.minPrice, listing.getPrice());
            acc.maxPrice = Math.max(acc.maxPrice, listing.getPrice());
        }
        return acc;
    }
    @Override
    public Tuple2<String, PropertyStats> getResult(PropertyStatsAccumulator acc) {
        PropertyStats stats = new PropertyStats(
            acc.neighborhoodId,
            acc.count,
            acc.totalPrice / acc.count,
            acc.minPrice,
            acc.maxPrice,
            acc.totalSquareFeet / acc.count
        );
        return new Tuple2<>(acc.neighborhoodId, stats);
    }
    @Override
    public PropertyStatsAccumulator merge(PropertyStatsAccumulator a, PropertyStatsAccumulator b) {
        PropertyStatsAccumulator merged = new PropertyStatsAccumulator();
        merged.neighborhoodId = a.neighborhoodId;
        merged.count = a.count + b.count;
        merged.totalPrice = a.totalPrice + b.totalPrice;
        merged.totalSquareFeet = a.totalSquareFeet + b.totalSquareFeet;
        merged.minPrice = Math.min(a.minPrice, b.minPrice);
        merged.maxPrice = Math.max(a.maxPrice, b.maxPrice);
        return merged;
    }
}
