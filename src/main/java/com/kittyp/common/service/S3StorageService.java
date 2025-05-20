/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.service;

import java.net.URL;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.kittyp.common.exception.CustomException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * @author rrohan419@gmail.com 
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService {

    @Value("${aws.invoice.bucket.name}")
    private String bucket;

    private final S3Client s3;
    
    private final S3Presigner s3Presigner;
    
    private final S3Client s3Client;
    
    public String uploadInvoice(String orderId, byte[] pdfBytes) {
    	String key = "invoices/" + orderId + ".pdf";
    	try {
           PutObjectRequest put = PutObjectRequest.builder()
                   .bucket(bucket)
                   .key(key)
                   .contentType("application/pdf")
                   .build();
           s3.putObject(put, RequestBody.fromBytes(pdfBytes));
		} catch (Exception e) {
			log.error("Error generating invoice for order number : "+orderId+" with message ====> "+e.getMessage());
		}
       
        return key;
    }
    
//    public URL presignedUrl(String orderId, Duration ttl) {
//    	String key = "invoices/" + orderId + ".pdf";
//        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(builder -> 
//            builder.signatureDuration(ttl)
//                   .getObjectRequest(getReq -> 
//                       getReq.bucket(bucket)
//                             .key(key)
//                   )
//        );
//
//        return presignedRequest.url();
//    }
    
    public URL presignedUrl(String orderId, Duration ttl) {
        String key = "invoices/" + orderId + ".pdf";

        // Check if object exists
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch ( S3Exception e) {
            if (e.awsErrorDetails() != null &&
                "Not Found".equalsIgnoreCase(e.awsErrorDetails().errorMessage()) ||
                e.statusCode() == 404) {
                log.warn("Invoice not found in S3 for order ID: {}", orderId);
                throw new CustomException("Invoice not avaliable for order number : "+orderId, HttpStatus.NOT_FOUND);
            }
            throw e; // other AWS errors (e.g., credentials issue)
        }

        // Generate presigned URL
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

