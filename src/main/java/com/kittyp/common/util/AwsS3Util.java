/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.kittyp.common.constants.AppConstant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
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

	@Bean
	public S3Client s3Client() {
		 String accessKey = env.getProperty(AppConstant.AWS_ACCESS_KEY_ID);
		 String secretKey = env.getProperty(AppConstant.AWS_SECRET_KEY);
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);
        
		log.info("initilizing aws s3 slient for regiion ===> "+ env.getProperty(AppConstant.S3_REGION));
//		return S3Client.builder().region(Region.of(env.getProperty(AppConstant.S3_REGION)))
//				.credentialsProvider(DefaultCredentialsProvider.create()).build();
		
		return S3Client.builder()
                .region(Region.of(env.getProperty(AppConstant.S3_REGION)))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
	}

	@Bean
	public S3Presigner s3Presigner() {
		
		 String accessKey = env.getProperty(AppConstant.AWS_ACCESS_KEY_ID);
		 String secretKey = env.getProperty(AppConstant.AWS_SECRET_KEY);
        
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);
        
//		return S3Presigner.builder().region(Region.of(env.getProperty(AppConstant.S3_REGION)))
//				.credentialsProvider(DefaultCredentialsProvider.create()).build();
        return S3Presigner.builder()
                .region(Region.of(env.getProperty(AppConstant.S3_REGION)))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
	}
}
