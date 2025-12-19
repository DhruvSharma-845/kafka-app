package com.dhruvsharma.kafka.consumer_app.service;

import com.dhruvsharma.kafka.consumer_app.dto.MessageResponse;

public interface ConsumerService {
    MessageResponse consume();
}
