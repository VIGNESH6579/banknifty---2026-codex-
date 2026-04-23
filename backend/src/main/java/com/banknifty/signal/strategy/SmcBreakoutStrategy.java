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

        if (!isWithinTradingWindow()) {
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
        double consolidationAtr = averageTrueRange(consolidation, Math.min(14, consolidation.size() - 1));
        double compressionRatio = range / Math.max(avgClose, 1.0);
        double averageBody = consolidation.stream().mapToDouble(Candle::getBodySize).average().orElse(0.0);
        double momentumBias = confirmation.getClose() - consolidation.get(0).getClose();

        if (avgClose <= 0 || atr <= 0 || consolidationAtr <= 0) {
            return Optional.empty();
        }

        boolean tightRange = compressionRatio <= 0.0055 && range <= consolidationAtr * 1.8;
        boolean quietBodies = averageBody <= atr * 0.55;
        boolean sweepHigh = consolidation.stream().anyMatch(candle -> candle.getHigh() >= rangeHigh)
                && breakout.getHigh() > rangeHigh
                && breakout.getClose() < rangeHigh
                && breakout.getBodySize() <= breakout.getRange() * 0.45;
        boolean sweepLow = consolidation.stream().anyMatch(candle -> candle.getLow() <= rangeLow)
                && breakout.getLow() < rangeLow
                && breakout.getClose() > rangeLow
                && breakout.getBodySize() <= breakout.getRange() * 0.45;

        boolean breakoutExpansion = breakout.getRange() >= atr * 1.15;
        boolean confirmationExpansion = confirmation.getRange() >= atr * 1.1;
        boolean volatilityExpansion = breakoutExpansion || confirmationExpansion;
        boolean bullishBreak = confirmation.getClose() > rangeHigh
                && confirmation.isBullish()
                && confirmation.getBodySize() >= confirmation.getRange() * 0.55
                && confirmation.getClose() >= breakout.getHigh()
                && confirmation.getVolume() >= avgVolume * 1.7
                && momentumBias >= 0;
        boolean bearishBreak = confirmation.getClose() < rangeLow
                && confirmation.isBearish()
                && confirmation.getBodySize() >= confirmation.getRange() * 0.55
                && confirmation.getClose() <= breakout.getLow()
                && confirmation.getVolume() >= avgVolume * 1.7
                && momentumBias <= 0;

        if (!(tightRange && quietBodies && volatilityExpansion)) {
            return Optional.empty();
        }

        if (bullishBreak && sweepLow) {
            return Optional.of(buildSignal(SignalType.BUY, confirmation.getClose(), rangeLow, atr, range));
        }

        if (bearishBreak && sweepHigh) {
            return Optional.of(buildSignal(SignalType.SELL, confirmation.getClose(), rangeHigh, atr, range));
        }

        return Optional.empty();
    }

    private StrategySignal buildSignal(SignalType type, double entry, double structuralStop, double atr, double range) {
        double stopDistance = Math.max(Math.max(atr * 0.85, range * 0.45), 20.0);
        double stopLoss = type == SignalType.BUY
                ? Math.min(structuralStop, entry - stopDistance)
                : Math.max(structuralStop, entry + stopDistance);
        double target = type == SignalType.BUY
                ? entry + (entry - stopLoss) * 2.0
                : entry - (stopLoss - entry) * 2.0;
        double structuralQuality = Math.min(1.0, range / Math.max(atr, 1.0));
        double confidence = Math.min(0.95, 0.64 + (atr / Math.max(entry, 1.0)) * 40.0 + structuralQuality * 0.06);

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

    private boolean isWithinTradingWindow() {
        LocalTime now = LocalTime.now(INDIA);
        return !now.isBefore(LocalTime.of(9, 20)) && !now.isAfter(LocalTime.of(15, 5));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
