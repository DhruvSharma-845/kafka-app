package com.dhruvsharma.kafka.consumer_app.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.dhruvsharma.kafka.consumer_app.dto.MessageResponse;
import com.dhruvsharma.kafka.consumer_app.dto.MessageResponse.KafkaMessage;

@Service
public class ConsumerServiceImpl implements ConsumerService {

    // Store messages by topic for retrieval via API
    private final Map<String, List<KafkaMessage>> messageStore = new ConcurrentHashMap<>();

    @Value("${app.kafka.topics}")
    private String topic;

    @KafkaListener(
        topics = "${app.kafka.topics:topic1}",
        groupId = "${spring.kafka.consumer.group-id}",
        concurrency = "3"  // Creates 3 consumer threads within this instance
    )
    public void listen(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        
        System.out.printf("Received message: key=%s, partition=%d, offset=%d, value=%s%n",
                key, partition, offset, message);

        // Store message
        messageStore.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>())
                .add(new KafkaMessage(key, message, partition, offset));
    }

    @Override
    public MessageResponse consume() {
        return new MessageResponse("Success", this.topic, messageStore.getOrDefault(this.topic, List.of()));
    }

    // @Override
    // public MessageResponse consumeFromTopic(String topic) {
    //     List<KafkaMessage> messages = new ArrayList<>();
        
    //     try (KafkaConsumer<String, String> consumer = 
    //             (KafkaConsumer<String, String>) consumerFactory.createConsumer()) {
            
    //         // Get partition info for the topic
    //         List<PartitionInfo> partitionInfos = consumer.partitionsFor(topic);
    //         if (partitionInfos == null || partitionInfos.isEmpty()) {
    //             return new MessageResponse("Topic not found or has no partitions", topic, messages);
    //         }
            
    //         // Create TopicPartition list from partition info
    //         List<TopicPartition> topicPartitions = partitionInfos.stream()
    //             .map(info -> new TopicPartition(topic, info.partition()))
    //             .collect(Collectors.toList());
            
    //         // Assign partitions directly (bypasses consumer group coordination)
    //         consumer.assign(topicPartitions);
            
    //         // Seek to the beginning of all partitions to read all messages
    //         consumer.seekToBeginning(topicPartitions);
            
    //         // Poll for messages (multiple polls may be needed)
    //         int maxPolls = 3;
    //         for (int i = 0; i < maxPolls; i++) {
    //             ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
                
    //             for (ConsumerRecord<String, String> record : records) {
    //                 messages.add(new KafkaMessage(
    //                     record.key(),
    //                     record.value(),
    //                     record.partition(),
    //                     record.offset()
    //                 ));
    //             }
                
    //             // If we got messages, we can stop polling
    //             if (!messages.isEmpty()) {
    //                 break;
    //             }
    //         }
    //     }
        
    //     return new MessageResponse(
    //         messages.isEmpty() ? "No messages found" : "Messages consumed successfully",
    //         topic,
    //         messages
    //     );
    // }
}