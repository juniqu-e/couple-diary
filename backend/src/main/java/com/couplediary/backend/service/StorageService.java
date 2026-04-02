package com.couplediary.backend.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class StorageService {

    private final S3Client s3Client;

    @Value("${storage.bucket}")
    private String bucketName;

    public StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @PostConstruct
    private void createBucketIfNotExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        }
    }

    public String uploadFile(MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileName)
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

        return fileName;
    }

    public InputStream downloadFile(String fileName) {
        ResponseInputStream<GetObjectResponse> response = s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileName)
                        .build()
        );
        return response;
    }

    public void deleteFile(String fileName) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileName)
                        .build()
        );
    }

    public String getFileUrl(String fileName) {
        return bucketName + "/" + fileName;
    }
}
