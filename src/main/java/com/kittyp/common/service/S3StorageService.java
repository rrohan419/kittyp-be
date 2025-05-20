/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.service;

import java.net.URL;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * @author rrohan419@gmail.com 
 */
@Service
@RequiredArgsConstructor
public class S3StorageService {

    @Value("${app.aws-invoice-bucket}")
    private String bucket;

    private final S3Client s3;
    
    private final S3Presigner s3Presigner;

    public String upload(String orderId, byte[] pdfBytes) {
        String key = "invoices/" + orderId + ".pdf";

        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("application/pdf")
                .build();

        s3.putObject(put, RequestBody.fromBytes(pdfBytes));
        return key;
    }
    
    public URL presignedUrl(String key, Duration ttl) {
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(builder -> 
            builder.signatureDuration(ttl)
                   .getObjectRequest(getReq -> 
                       getReq.bucket(bucket)
                             .key(key)
                   )
        );

        return presignedRequest.url();
    }


}

