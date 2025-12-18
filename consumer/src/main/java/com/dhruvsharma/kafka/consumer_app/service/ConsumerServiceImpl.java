package com.dhruvsharma.kafka.consumer_app.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Service;

import com.dhruvsharma.kafka.consumer_app.dto.MessageResponse;
import com.dhruvsharma.kafka.consumer_app.dto.MessageResponse.KafkaMessage;

@Service
public class ConsumerServiceImpl implements ConsumerService {

    private final ConsumerFactory<String, String> consumerFactory;

    @Autowired
    public ConsumerServiceImpl(ConsumerFactory<String, String> consumerFactory) {
        this.consumerFactory = consumerFactory;
    }

    @Override
    public MessageResponse consumeFromTopic(String topic) {
        List<KafkaMessage> messages = new ArrayList<>();
        
        try (KafkaConsumer<String, String> consumer = 
                (KafkaConsumer<String, String>) consumerFactory.createConsumer()) {
            
            // Get partition info for the topic
            List<PartitionInfo> partitionInfos = consumer.partitionsFor(topic);
            if (partitionInfos == null || partitionInfos.isEmpty()) {
                return new MessageResponse("Topic not found or has no partitions", topic, messages);
            }
            
            // Create TopicPartition list from partition info
            List<TopicPartition> topicPartitions = partitionInfos.stream()
                .map(info -> new TopicPartition(topic, info.partition()))
                .collect(Collectors.toList());
            
            // Assign partitions directly (bypasses consumer group coordination)
            consumer.assign(topicPartitions);
            
            // Seek to the beginning of all partitions to read all messages
            consumer.seekToBeginning(topicPartitions);
            
            // Poll for messages (multiple polls may be needed)
            int maxPolls = 3;
            for (int i = 0; i < maxPolls; i++) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
                
                for (ConsumerRecord<String, String> record : records) {
                    messages.add(new KafkaMessage(
                        record.key(),
                        record.value(),
                        record.partition(),
                        record.offset()
                    ));
                }
                
                // If we got messages, we can stop polling
                if (!messages.isEmpty()) {
                    break;
                }
            }
        }
        
        return new MessageResponse(
            messages.isEmpty() ? "No messages found" : "Messages consumed successfully",
            topic,
            messages
        );
    }
}