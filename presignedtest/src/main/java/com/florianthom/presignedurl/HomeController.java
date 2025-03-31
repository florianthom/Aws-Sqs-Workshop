package com.florianthom.presignedurl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import org.springframework.web.reactive.function.client.WebClient;


import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;

@RestController("/")
public class HomeController{

    private final WebClient webClient = WebClient.builder().build();
    @Autowired
    public HomeController() {    }

    @GetMapping("")
    public ResponseEntity<String> index() {
        return ResponseEntity.ok("<h1>ping</h1>");
    }


    @GetMapping("presignedurl")
    public ResponseEntity<String> presignedurl() {
        var presigner = S3Presigner.builder()
                .region(Region.EU_CENTRAL_1)
                .credentialsProvider(
                    //  StaticCredentialsProvider.create(AwsBasicCredentials.create("apikey", "secret"))
                    ProfileCredentialsProvider.create()
                )
                .build();

        // old: generatePresignedurlrequest

        // Request mit Größenlimit
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket("ft-testbucket-tmp")
                .key("large_image.jpg")
                .contentType(MediaType.IMAGE_JPEG_VALUE)
                // .contentLength(100 * 1024 * 1024L) // 100 MB
                .build();
        // Presigned URL mit Ablaufzeit erstellen (z.B. 10 Minuten)
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putObjectRequest)
                //.signedHeaders(Map.of("content-length", String.valueOf(maxFileSize))) // Enforce size in the signature
                .build();
        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);

        // https://ft-testbucket-tmp.s3.eu-central-1.amazonaws.com/key.json
        //  ?X-Amz-Algorithm=AWS4-HMAC-SHA256
        //  &X-Amz-Date=20250322T112323Z
        //  &X-Amz-SignedHeaders=content-length%3Bhost
        //  &X-Amz-Credential=AKIA2HWSRWYJCVCYP4BE%2F20250322%2Feu-central-1%2Fs3%2Faws4_request
        //  &X-Amz-Expires=600&X-Amz-Signature=edde7f15814e70c13b98664cb5b915f72f40067ef46afc2dd6714ac155dbdc29
        var resultUrl = presignedRequest.url().toString();

        var file = loadFile("large_image.jpg");

        try {
        URL url = null;
        url = new URL(resultUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", MediaType.IMAGE_JPEG_VALUE);
        connection.getOutputStream().write(file);
        connection.getOutputStream().close();

        int responseCode = connection.getResponseCode();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // var webClientResult = webClient.put()
        //         .uri(resultUrl)
        //         .contentType(MediaType.IMAGE_JPEG)
        //         .bodyValue(file)
        //         .retrieve()
        //         .onStatus(HttpStatusCode::isError, clientResponse -> {
        //             System.out.println("Upload failed: " + clientResponse.statusCode());
        //             return clientResponse.createException();
        //         })
        //         .toBodilessEntity()
        //         .map(response -> {
        //             System.out.println("Upload status: " + response.getStatusCode());
        //             return response.getStatusCode().is2xxSuccessful();
        //         })
        //         .block();

        return ResponseEntity.ok(resultUrl);
    }

    public static <T> T loadFileWithType(String filePath, TypeReference<T> typeReference, ObjectMapper objectMapper){
        Resource resource = new ClassPathResource(filePath);
        T result = null;
        try {
            result = objectMapper.readValue(resource.getInputStream(), typeReference);
        }catch (IOException e){
            throw new RuntimeException(e);
        }
        return result;
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
}