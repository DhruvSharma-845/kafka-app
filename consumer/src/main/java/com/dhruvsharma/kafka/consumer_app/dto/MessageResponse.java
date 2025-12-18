package com.dhruvsharma.kafka.consumer_app.dto;

import java.util.List;

public record MessageResponse(String status, String topic, List<KafkaMessage> messages) {
    
    public record KafkaMessage(String key, String value, int partition, long offset) {}
}