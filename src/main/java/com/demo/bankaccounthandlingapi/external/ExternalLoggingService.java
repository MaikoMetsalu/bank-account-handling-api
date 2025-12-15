package com.demo.bankaccounthandlingapi.external;

import com.demo.bankaccounthandlingapi.exceptions.ExternalSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Service
public class ExternalLoggingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalLoggingService.class);

    private final RestClient restClient;
    private final String externalUrl;

    public ExternalLoggingService(@Value("${external.logging.url}") String externalUrl) {
        this.externalUrl = externalUrl;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(2));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(externalUrl)
                .build();
    }

    public void sendLog() {
        LOGGER.info("Sending request to external system at {}", externalUrl);
        try {
            restClient.get()
                    .retrieve()
                    .toBodilessEntity();
            LOGGER.info("External request sent.");
        } catch (Exception e) {
            LOGGER.error("External request failed: {}", e.getMessage());
            throw new ExternalSystemException("External system call failed. Transaction aborted.");
        }
    }
}