package com.banknifty.signal.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.springframework.stereotype.Service;

import com.banknifty.signal.config.MarketProperties;
import com.banknifty.signal.model.Candle;
import com.banknifty.signal.model.Tick;

@Service
public class CandleEngineService {

    private final int candleCapacity;
    private final Deque<Candle> candles = new ArrayDeque<>();

    private Instant activeBucket;
    private double open;
    private double high;
    private double low;
    private double close;
    private double volume;

    public CandleEngineService(MarketProperties marketProperties) {
        this.candleCapacity = marketProperties.getCandleCapacity();
    }

    public synchronized void onTick(Tick tick) {
        Instant bucket = tick.timestamp().truncatedTo(ChronoUnit.MINUTES);
        if (activeBucket == null) {
            startBucket(bucket, tick.price(), tick.volume());
            return;
        }

        if (bucket.isAfter(activeBucket)) {
            closeActiveBucket();
            startBucket(bucket, tick.price(), tick.volume());
            return;
        }

        high = Math.max(high, tick.price());
        low = Math.min(low, tick.price());
        close = tick.price();
        volume += tick.volume();
    }

    public synchronized Candle finalizeCompletedBucket(Instant now) {
        if (activeBucket == null) {
            return null;
        }

        Instant currentBucket = now.truncatedTo(ChronoUnit.MINUTES);
        if (!currentBucket.isAfter(activeBucket)) {
            return null;
        }

        Candle finalized = closeActiveBucket();
        activeBucket = null;
        return finalized;
    }

    public synchronized List<Candle> getRecentCandles() {
        return List.copyOf(candles);
    }

    private void startBucket(Instant bucket, double price, double tickVolume) {
        activeBucket = bucket;
        open = price;
        high = price;
        low = price;
        close = price;
        volume = tickVolume;
    }

    private Candle closeActiveBucket() {
        Candle candle = new Candle(activeBucket, open, high, low, close, volume);
        candles.addLast(candle);
        while (candles.size() > candleCapacity) {
            candles.removeFirst();
        }
        return candle;
    }
}
