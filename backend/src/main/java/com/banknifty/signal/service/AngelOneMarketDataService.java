package com.banknifty.signal.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import com.angelbroking.smartapi.SmartConnect;
import com.angelbroking.smartapi.http.SessionExpiryHook;
import com.angelbroking.smartapi.models.User;
import com.angelbroking.smartapi.smartstream.models.ExchangeType;
import com.angelbroking.smartapi.smartstream.models.LTP;
import com.angelbroking.smartapi.smartstream.models.Quote;
import com.angelbroking.smartapi.smartstream.models.SmartStreamError;
import com.angelbroking.smartapi.smartstream.models.SmartStreamSubsMode;
import com.angelbroking.smartapi.smartstream.models.SnapQuote;
import com.angelbroking.smartapi.smartstream.models.TokenID;
import com.angelbroking.smartapi.smartstream.ticker.SmartStreamListener;
import com.angelbroking.smartapi.smartstream.ticker.SmartStreamTicker;
import com.banknifty.signal.config.MarketProperties;
import com.banknifty.signal.dto.SignalDto;
import com.banknifty.signal.model.Tick;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;

@Service
public class AngelOneMarketDataService {

    private static final Logger log = LoggerFactory.getLogger(AngelOneMarketDataService.class);

    private final MarketProperties marketProperties;
    private final CandleEngineService candleEngineService;
    private final MarketBroadcastService marketBroadcastService;
    private final SignalPersistenceService signalPersistenceService;
    private final TaskScheduler taskScheduler;

    private final AtomicReference<Double> livePrice = new AtomicReference<>(0.0);
    private final AtomicReference<Instant> lastTickTime = new AtomicReference<>(Instant.now());
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator();
    private final SystemTimeProvider timeProvider = new SystemTimeProvider();

    private volatile SmartStreamTicker smartStreamTicker;

    public AngelOneMarketDataService(
            MarketProperties marketProperties,
            CandleEngineService candleEngineService,
            MarketBroadcastService marketBroadcastService,
            SignalPersistenceService signalPersistenceService,
            TaskScheduler taskScheduler) {
        this.marketProperties = marketProperties;
        this.candleEngineService = candleEngineService;
        this.marketBroadcastService = marketBroadcastService;
        this.signalPersistenceService = signalPersistenceService;
        this.taskScheduler = taskScheduler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void connectAfterStartup() {
        if (!marketProperties.isStreamEnabled()) {
            log.info("Market stream disabled via configuration");
            return;
        }
        scheduleConnect(Instant.now().plusSeconds(5));
    }

    @PreDestroy
    public void disconnect() {
        if (smartStreamTicker != null) {
            smartStreamTicker.disconnect();
        }
    }

    public double getLivePrice() {
        return livePrice.get();
    }

    public Instant getLastTickTime() {
        return lastTickTime.get();
    }

    private void scheduleConnect(Instant when) {
        taskScheduler.schedule(this::openStreamSafely, when);
    }

    private void openStreamSafely() {
        if (!connecting.compareAndSet(false, true)) {
            return;
        }

        try {
            SmartConnect smartConnect = new SmartConnect();
            smartConnect.setApiKey(requireEnv("ANGEL_API_KEY"));
            smartConnect.setSessionExpiryHook(new SessionExpiryHook() {
                @Override
                public void sessionExpired() {
                    log.warn("Angel One session expired, reconnecting");
                    reconnectLater();
                }
            });

            User user = smartConnect.generateSession(
                    requireEnv("ANGEL_CLIENT_ID"),
                    requireEnv("ANGEL_PASSWORD"),
                    generateCurrentTotp()
            );
            if (user == null) {
                throw new IllegalStateException("Angel One authentication failed");
            }

            smartConnect.setAccessToken(user.getAccessToken());
            smartConnect.setUserId(user.getUserId());

            SmartStreamListener listener = new SmartStreamListener() {
                @Override
                public void onLTPArrival(LTP ltp) {
                    double price = ltp.getLastTradedPrice() / marketProperties.getPriceScale();
                    Instant tickTime = Instant.ofEpochMilli(ltp.getExchangeFeedTimeEpochMillis());
                    Tick tick = new Tick(marketProperties.getSymbol(), price, 1.0, tickTime);
                    candleEngineService.onTick(tick);
                    livePrice.set(price);
                    lastTickTime.set(tickTime);
                    SignalDto latestSignal = signalPersistenceService.getLatestSignal(marketProperties.getSymbol());
                    marketBroadcastService.publishPrice(price, candleEngineService.getRecentCandles(), latestSignal, tickTime);
                }

                @Override
                public void onQuoteArrival(Quote quote) {
                }

                @Override
                public void onSnapQuoteArrival(SnapQuote snapQuote) {
                }

                @Override
                public void onDepthArrival(com.angelbroking.smartapi.smartstream.models.Depth depth) {
                }

                @Override
                public void onConnected() {
                    log.info("Connected to Angel One SmartAPI stream");
                }

                @Override
                public void onDisconnected() {
                    log.warn("Angel One stream disconnected");
                    reconnectLater();
                }

                @Override
                public void onError(SmartStreamError smartStreamError) {
                    log.error("Angel One stream error", smartStreamError.getException());
                    reconnectLater();
                }

                @Override
                public void onPong() {
                    log.debug("Received SmartAPI heartbeat pong");
                }

                @Override
                public SmartStreamError onErrorCustom() {
                    SmartStreamError error = new SmartStreamError();
                    log.error("Angel One custom websocket error");
                    return error;
                }
            };

            SmartStreamTicker ticker = new SmartStreamTicker(requireEnv("ANGEL_CLIENT_ID"), user.getFeedToken(), listener);
            ticker.connect();
            ticker.subscribe(
                    SmartStreamSubsMode.LTP,
                    Set.of(new TokenID(ExchangeType.NSE_CM, marketProperties.getToken()))
            );
            this.smartStreamTicker = ticker;
        } catch (Exception exception) {
            log.error("Unable to connect to Angel One SmartAPI", exception);
            reconnectLater();
        } finally {
            connecting.set(false);
        }
    }

    private void reconnectLater() {
        Instant when = Instant.now().plus(marketProperties.getReconnectDelay());
        scheduleConnect(when);
    }

    private String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }
        return value;
    }

    private String generateCurrentTotp() {
        try {
            return codeGenerator.generate(requireEnv("ANGEL_TOTP_SECRET"), timeProvider.getTime());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate current TOTP from ANGEL_TOTP_SECRET", exception);
        }
    }
}
