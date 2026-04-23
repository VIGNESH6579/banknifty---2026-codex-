package com.banknifty.signal.strategy;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.banknifty.signal.config.MarketProperties;
import com.banknifty.signal.model.Candle;
import com.banknifty.signal.model.SignalType;
import com.banknifty.signal.model.StrategySignal;

@Component
public class SmcBreakoutStrategy {

    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

    private final MarketProperties marketProperties;

    public SmcBreakoutStrategy(MarketProperties marketProperties) {
        this.marketProperties = marketProperties;
    }

    public Optional<StrategySignal> evaluate(List<Candle> candles) {
        if (candles.size() < 17) {
            return Optional.empty();
        }

        List<Candle> consolidation = candles.subList(candles.size() - 17, candles.size() - 2);
        Candle breakout = candles.get(candles.size() - 2);
        Candle confirmation = candles.get(candles.size() - 1);

        double rangeHigh = consolidation.stream().mapToDouble(Candle::getHigh).max().orElse(0.0);
        double rangeLow = consolidation.stream().mapToDouble(Candle::getLow).min().orElse(0.0);
        double range = rangeHigh - rangeLow;
        double avgClose = consolidation.stream().mapToDouble(Candle::getClose).average().orElse(0.0);
        double avgVolume = consolidation.stream().mapToDouble(Candle::getVolume).average().orElse(0.0);
        double atr = averageTrueRange(candles, 14);

        if (avgClose <= 0 || atr <= 0) {
            return Optional.empty();
        }

        boolean tightRange = range <= avgClose * 0.0065;
        boolean sweepHigh = consolidation.stream().anyMatch(candle -> candle.getHigh() >= rangeHigh)
                && breakout.getHigh() > rangeHigh
                && breakout.getClose() < rangeHigh;
        boolean sweepLow = consolidation.stream().anyMatch(candle -> candle.getLow() <= rangeLow)
                && breakout.getLow() < rangeLow
                && breakout.getClose() > rangeLow;

        boolean volatilityExpansion = breakout.getRange() >= atr * 1.2 || confirmation.getRange() >= atr * 1.2;
        boolean bullishBreak = confirmation.getClose() > rangeHigh
                && confirmation.isBullish()
                && confirmation.getBodySize() >= confirmation.getRange() * 0.55
                && confirmation.getVolume() >= avgVolume * 1.5;
        boolean bearishBreak = confirmation.getClose() < rangeLow
                && confirmation.isBearish()
                && confirmation.getBodySize() >= confirmation.getRange() * 0.55
                && confirmation.getVolume() >= avgVolume * 1.5;

        if (!(tightRange && volatilityExpansion)) {
            return Optional.empty();
        }

        if (bullishBreak && sweepLow) {
            return Optional.of(buildSignal(SignalType.BUY, confirmation.getClose(), rangeLow, atr));
        }

        if (bearishBreak && sweepHigh) {
            return Optional.of(buildSignal(SignalType.SELL, confirmation.getClose(), rangeHigh, atr));
        }

        return Optional.empty();
    }

    private StrategySignal buildSignal(SignalType type, double entry, double structuralStop, double atr) {
        double stopDistance = Math.max(atr * 0.8, 20.0);
        double stopLoss = type == SignalType.BUY
                ? Math.min(structuralStop, entry - stopDistance)
                : Math.max(structuralStop, entry + stopDistance);
        double target = type == SignalType.BUY
                ? entry + (entry - stopLoss) * 1.8
                : entry - (stopLoss - entry) * 1.8;
        double confidence = Math.min(0.95, 0.6 + (atr / Math.max(entry, 1.0)) * 50.0);

        return new StrategySignal(
                marketProperties.getSymbol(),
                type,
                round(entry),
                round(stopLoss),
                round(target),
                round(confidence),
                nearestWeeklyExpiry(),
                Instant.now()
        );
    }

    private double averageTrueRange(List<Candle> candles, int period) {
        int start = Math.max(1, candles.size() - period);
        double sum = 0.0;
        int count = 0;
        for (int i = start; i < candles.size(); i++) {
            Candle current = candles.get(i);
            Candle previous = candles.get(i - 1);
            double tr = Math.max(
                    current.getHigh() - current.getLow(),
                    Math.max(
                            Math.abs(current.getHigh() - previous.getClose()),
                            Math.abs(current.getLow() - previous.getClose())
                    )
            );
            sum += tr;
            count++;
        }
        return count == 0 ? 0.0 : sum / count;
    }

    private LocalDateTime nearestWeeklyExpiry() {
        LocalDate date = LocalDate.now(INDIA);
        while (date.getDayOfWeek() != DayOfWeek.THURSDAY) {
            date = date.plusDays(1);
        }
        return LocalDateTime.of(date, LocalTime.of(15, 30));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
