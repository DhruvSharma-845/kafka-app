package com.dhruvsharma.kafka.producer_app.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dhruvsharma.kafka.producer_app.dto.MessageRequest;
import com.dhruvsharma.kafka.producer_app.service.ProducerService;

@RestController
@RequestMapping("/api/kafka")
public class ProducerController {

    private final ProducerService producerService;

    @Autowired
    public ProducerController(ProducerService producerService) {
        this.producerService = producerService;
    }

    @PostMapping("/publish/{topic}")
    public ResponseEntity<Map<String, String>> publishToTopic(
            @PathVariable String topic,
            @RequestBody MessageRequest request) {
        producerService.sendMessage(topic, request.key(), request.message())
        .whenComplete((result, ex) -> {
            if (ex != null) {
                System.err.println("Failed to send message: " + ex.getMessage());
            } else {
                System.out.println("Message sent to topic: " + result.getRecordMetadata().topic() +
                    ", partition: " + result.getRecordMetadata().partition() +
                    ", offset: " + result.getRecordMetadata().offset());
            }
        });
        return ResponseEntity.ok(Map.of(
            "status", "Message sent",
            "topic", topic
        ));
    }
}