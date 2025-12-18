package com.dhruvsharma.kafka.producer_app.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.support.SendResult;

public interface ProducerService {
    CompletableFuture<SendResult<String, String>> sendMessage(String topic, String key, String message);
}
