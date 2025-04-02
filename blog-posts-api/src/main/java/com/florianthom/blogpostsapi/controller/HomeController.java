package com.florianthom.blogpostsapi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.florianthom.blogpostsapi.domain.BlogPostJobRequestDto;
import com.florianthom.blogpostsapi.services.SqsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.util.UUID;

@RestController("/")
public class HomeController {

    private final S3Presigner presigner;

    private final SqsService sqsService;

    private final ObjectMapper objectMapper;

    @Value("${blogpostsapi.data.bucket.name}")
    private String bucketname;

    @Autowired
    public HomeController(S3Presigner presigner, SqsService sqsService, ObjectMapper objectMapper) {
        this.presigner = presigner;
        this.sqsService = sqsService;
        this.objectMapper = objectMapper;
    }

    public static String getPresignedUrl(S3Presigner presigner, String bucketname) {
        var presignPutRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(
                        PutObjectRequest.builder()
                                .bucket(bucketname)
                                .key("item.jpg")
                                .contentType(MediaType.IMAGE_JPEG_VALUE)
                                .build()
                ).build();

        var presignedPutRequest = presigner.presignPutObject(presignPutRequest);

        // result e.g. https://ft-testbucket-tmp.s3.eu-central-1.amazonaws.com/key.json
        //  ?X-Amz-Algorithm=AWS4-HMAC-SHA256
        //  &X-Amz-Date=20250322T112323Z
        //  &X-Amz-SignedHeaders=content-length%3Bhost
        //  &X-Amz-Credential=AKIA2HWSRWYJCVCYP4BE%2F20250322%2Feu-central-1%2Fs3%2Faws4_request
        //  &X-Amz-Expires=600&X-Amz-Signature=edde7f15814e70c13b98664cb5b915f72f40067ef46afc2dd6714ac155dbdc29
        var presignedUrlString = presignedPutRequest.url().toString();

        return presignedUrlString;
    }

    public static int uploadFile(byte[] file, String targeturl) {
        int responseCode = -1;
        try {
            URL url = new URL(targeturl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", MediaType.IMAGE_JPEG_VALUE);
            connection.getOutputStream().write(file);
            connection.getOutputStream().close();
            responseCode = connection.getResponseCode();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return responseCode;
    }

    public static byte[] loadFile(String filePath) {
        Resource resource = new ClassPathResource(filePath);
        byte[] content = null;
        try {
            File file = resource.getFile();
            content = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return content;
    }

    @GetMapping("")
    public ResponseEntity<String> index() {
        return ResponseEntity.ok("<h1>ping</h1>");
    }

    @GetMapping("testpresignedurl")
    public ResponseEntity<String> presignedurl() {
        var url = getPresignedUrl(this.presigner, this.bucketname);

        var file = loadFile("large_image.jpg");

        uploadFile(file, url);

        return ResponseEntity.ok("finished testpresignedurl");
    }

    // curl localhost:8080/createsqsjob
    @GetMapping("createsqsjob")
    public ResponseEntity<String> createSqsJob() throws JsonProcessingException {
        var jobId = UUID.randomUUID();
        var blogPostJobRequest = this.objectMapper.writeValueAsString(
                new BlogPostJobRequestDto("my blog post job " + jobId, "my blog post content")
        );

        this.sqsService.createMessage(blogPostJobRequest);

        return ResponseEntity.ok("created job createsqsjob " + jobId);
    }
}