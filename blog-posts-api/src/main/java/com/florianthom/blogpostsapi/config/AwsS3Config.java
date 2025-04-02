package com.florianthom.blogpostsapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

@Configuration
public class AwsS3Config {

    @Value("${blogpostsapi.data.bucket.endpoint}")
    private String bucketEndpoint;

    @Value("${blogpostsapi.data.bucket.accesskey}")
    private String accessKey;

    @Value("${blogpostsapi.data.bucket.secretkey}")
    private String secretKey;

    @Value("${blogpostsapi.data.bucket.region}")
    private String region;

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                // .credentialsProvider(ProfileCredentialsProvider.create())
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .serviceConfiguration(S3Configuration.builder()
                        // required for localstack
                        .pathStyleAccessEnabled(true)
                        .build()
                )
                .endpointOverride(URI.create(bucketEndpoint))
                .build();
    }
}