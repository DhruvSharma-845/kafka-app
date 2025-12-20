package com.dhruvsharma.kafka.producer_app.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@ExtendWith(MockitoExtension.class)
class ProducerServiceImplTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private ProducerServiceImpl producerService;

    private static Schema payloadSchema;

    @BeforeAll
    static void loadAsyncApiSchema() throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        JsonNode asyncApiDoc = yamlMapper.readTree(
                new ClassPathResource("asyncapi/producer-asyncapi.yml").getInputStream());

        JsonNode payloadNode = asyncApiDoc
                .path("channels")
                .path("kafka.message")
                .path("messages")
                .path("KafkaMessage")
                .path("payload");

        if (payloadNode.isMissingNode() || payloadNode.isNull()) {
            throw new IllegalStateException("AsyncAPI payload schema not found for channel kafka.message");
        }

        // Convert the JSON schema section into a Schema validator.
        JSONObject rawSchema = new JSONObject(yamlMapper.convertValue(payloadNode, Map.class));
        payloadSchema = SchemaLoader.load(rawSchema);
    }

    @Test
    void sendMessageUsesKeyAndMessageThatConformToAsyncApiSchema() {
        String topic = "orders";
        String key = "order-1";
        String message = "payload-for-order-1";

        when(kafkaTemplate.send(topic, key, message)).thenReturn(new CompletableFuture<>());

        producerService.sendMessage(topic, key, message);

        JSONObject payload = new JSONObject(Map.of("key", key, "message", message));
        payloadSchema.validate(payload);

        verify(kafkaTemplate).send(eq(topic), eq(key), eq(message));
    }

    @Test
    void sendMessageWithInvalidPayloadFailsSchemaValidation() {
        String topic = "orders";
        String key = ""; // violates minLength in AsyncAPI schema
        String message = "";

        when(kafkaTemplate.send(topic, key, message)).thenReturn(new CompletableFuture<>());

        producerService.sendMessage(topic, key, message);

        JSONObject payload = new JSONObject(Map.of("key", key, "message", message));
        assertThatThrownBy(() -> payloadSchema.validate(payload))
                .isInstanceOf(org.everit.json.schema.ValidationException.class);
    }
}


