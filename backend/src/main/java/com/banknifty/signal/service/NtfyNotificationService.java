package com.banknifty.signal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.banknifty.signal.config.NotificationProperties;
import com.banknifty.signal.dto.SignalDto;

@Service
public class NtfyNotificationService {

    private static final Logger log = LoggerFactory.getLogger(NtfyNotificationService.class);

    private final NotificationProperties notificationProperties;
    private final WebClient webClient;

    public NtfyNotificationService(NotificationProperties notificationProperties, WebClient.Builder webClientBuilder) {
        this.notificationProperties = notificationProperties;
        this.webClient = webClientBuilder.baseUrl("https://ntfy.sh").build();
    }

    public void sendSignal(SignalDto signal) {
        if (!StringUtils.hasText(notificationProperties.getNtfyTopic())) {
            return;
        }

        String body = "%s BANKNIFTY%nEntry: %.2f%nSL: %.2f%nTarget: %.2f%nExpiry: %s"
                .formatted(signal.type(), signal.entry(), signal.stopLoss(), signal.target(), signal.expiry());

        webClient.post()
                .uri("/" + notificationProperties.getNtfyTopic())
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .doOnError(error -> log.warn("Failed to post ntfy notification", error))
                .subscribe();
    }
}
