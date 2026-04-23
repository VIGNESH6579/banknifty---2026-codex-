package com.banknifty.signal.scheduler;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.banknifty.signal.config.MarketProperties;
import com.banknifty.signal.dto.SignalDto;
import com.banknifty.signal.model.Candle;
import com.banknifty.signal.service.AngelOneMarketDataService;
import com.banknifty.signal.service.CandleEngineService;
import com.banknifty.signal.service.MarketBroadcastService;
import com.banknifty.signal.service.NtfyNotificationService;
import com.banknifty.signal.service.SignalPersistenceService;
import com.banknifty.signal.strategy.SmcBreakoutStrategy;

@Component
public class SignalScheduler {

    private static final Logger log = LoggerFactory.getLogger(SignalScheduler.class);

    private final CandleEngineService candleEngineService;
    private final SmcBreakoutStrategy strategy;
    private final SignalPersistenceService signalPersistenceService;
    private final NtfyNotificationService notificationService;
    private final AngelOneMarketDataService marketDataService;
    private final MarketBroadcastService marketBroadcastService;
    private final MarketProperties marketProperties;

    public SignalScheduler(
            CandleEngineService candleEngineService,
            SmcBreakoutStrategy strategy,
            SignalPersistenceService signalPersistenceService,
            NtfyNotificationService notificationService,
            AngelOneMarketDataService marketDataService,
            MarketBroadcastService marketBroadcastService,
            MarketProperties marketProperties) {
        this.candleEngineService = candleEngineService;
        this.strategy = strategy;
        this.signalPersistenceService = signalPersistenceService;
        this.notificationService = notificationService;
        this.marketDataService = marketDataService;
        this.marketBroadcastService = marketBroadcastService;
        this.marketProperties = marketProperties;
    }

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Kolkata")
    public void finalizeAndEvaluate() {
        Candle finalized = candleEngineService.finalizeCompletedBucket(Instant.now());
        if (finalized == null) {
            return;
        }

        List<Candle> candles = candleEngineService.getRecentCandles();
        strategy.evaluate(candles)
                .flatMap(signalPersistenceService::saveIfFresh)
                .ifPresent(signal -> {
                    log.info("Generated {} signal at {}", signal.type(), signal.entry());
                    notificationService.sendSignal(signal);
                });

        SignalDto latestSignal = signalPersistenceService.getLatestSignal(marketProperties.getSymbol());
        marketBroadcastService.publishPrice(
                marketDataService.getLivePrice(),
                candles,
                latestSignal,
                marketDataService.getLastTickTime()
        );
    }
}
