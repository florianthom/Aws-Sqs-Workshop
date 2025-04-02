package com.florianthom.blogpostsapi.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.florianthom.blogpostsapi.domain.BlogPostJobResultDto;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Service
public class SqsService {
    private final SqsClient sqsClient;

    private final ObjectMapper objectMapper;

    @Value("${blogpostsapi.worker.cruncher.queue.inputqueueurl}")
    private String inputQueueUrl;

    @Value("${blogpostsapi.worker.cruncher.queue.outputqueueurl}")
    private String outputQueueUrl;

    @Autowired
    public SqsService(SqsClient sqsClient, ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void startListening() {
        new Thread(this::getMessages).start();
    }

    public void createMessage(String messageBody) {
        SendMessageResponse response = sqsClient.sendMessage(
                SendMessageRequest.builder()
                        .queueUrl(inputQueueUrl).messageBody(messageBody)
                        .build());

        System.out.println("Message sent with ID: " + response.messageId());
    }

    private void getMessages() {
        while (true) {
            var messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                            .queueUrl(outputQueueUrl).maxNumberOfMessages(1)
                            .waitTimeSeconds(20).visibilityTimeout(300).build())
                    .messages();

            messages.forEach(a -> {
                try {
                    processResultMessage(objectMapper.readValue(a.body(), BlogPostJobResultDto.class));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(outputQueueUrl)
                        .receiptHandle(a.receiptHandle()).build());
            });
        }
    }

    private void processResultMessage(BlogPostJobResultDto message) {
        System.out.println("Received processed message: " + message);
    }
}
