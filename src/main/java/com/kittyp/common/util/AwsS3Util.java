/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.kittyp.common.constants.AppConstant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * @author rrohan419@gmail.com 
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AwsS3Util {

	private final Environment env;
	
//	@Bean
//    public S3Client s3Client() {
//        return S3Client.builder()
//                .region(Region.of(env.getProperty(AppConstant.S3_REGION)))
//                .credentialsProvider(DefaultCredentialsProvider.create())
//                .build();
//    }
//	
//	 @Bean
//	    public S3Presigner s3Presigner() {
//	        return S3Presigner.builder()
//	                .region(Region.of(env.getProperty(AppConstant.S3_REGION)))
//	                .credentialsProvider(DefaultCredentialsProvider.create())
//	                .build();
//	    }
}
