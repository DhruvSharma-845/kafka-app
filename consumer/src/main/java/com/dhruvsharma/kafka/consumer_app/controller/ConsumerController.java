package com.dhruvsharma.kafka.consumer_app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dhruvsharma.kafka.consumer_app.dto.MessageResponse;
import com.dhruvsharma.kafka.consumer_app.service.ConsumerService;

@RestController
@RequestMapping("/api/kafka")
public class ConsumerController {

    private final ConsumerService consumerService;

    @Autowired
    public ConsumerController(ConsumerService consumerService) {
        this.consumerService = consumerService;
    }

    @GetMapping("/consume")
    public ResponseEntity<MessageResponse> consume() {
        MessageResponse response = consumerService.consume();
        return ResponseEntity.ok(response);
    }
}