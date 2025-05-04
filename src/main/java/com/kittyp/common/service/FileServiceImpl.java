///**
// * @author rrohan419@gmail.com
// */
//package com.kittyp.common.service;
//
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.time.Duration;
//import java.time.Instant;
//import java.time.temporal.ChronoUnit;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Set;
//import java.util.UUID;
//import java.util.concurrent.CompletableFuture;
//
//import org.springframework.core.env.Environment;
//import org.springframework.core.io.InputStreamResource;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import com.kittyp.common.constants.AppConstant;
//import com.kittyp.common.constants.ExceptionConstant;
//import com.kittyp.common.dao.FileRecordDao;
//import com.kittyp.common.dto.FileUpdateDto;
//import com.kittyp.common.dto.FilterDto;
//import com.kittyp.common.entity.FileRecord;
//import com.kittyp.common.enums.FileGroup;
//import com.kittyp.common.exception.CustomException;
//import com.kittyp.common.model.FileDownloadModel;
//import com.kittyp.common.model.FileRecordModel;
//import com.kittyp.common.model.PaginationModel;
//import com.kittyp.common.model.PresignedUrlModel;
//import com.kittyp.common.util.Mapper;
//
//import lombok.RequiredArgsConstructor;
//import software.amazon.awssdk.awscore.exception.AwsServiceException;
//import software.amazon.awssdk.core.ResponseInputStream;
//import software.amazon.awssdk.core.exception.SdkClientException;
//import software.amazon.awssdk.core.sync.RequestBody;
//import software.amazon.awssdk.services.s3.S3Client;
//import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
//import software.amazon.awssdk.services.s3.model.GetObjectRequest;
//import software.amazon.awssdk.services.s3.model.GetObjectResponse;
//import software.amazon.awssdk.services.s3.model.PutObjectRequest;
//import software.amazon.awssdk.services.s3.model.PutObjectResponse;
//import software.amazon.awssdk.services.s3.presigner.S3Presigner;
//import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
//
///**
// * @author rrohan419@gmail.com
// */
//@Service
//@RequiredArgsConstructor
//public class FileServiceImpl implements FileService {
//
//	private final Environment env;
//
//	private final S3Client s3Client;
//
//	private final Mapper mapper;
//
//	private final FileRecordDao fileRecordDao;
//
//	private final S3Presigner s3Presigner;
//
//	/**
//	 * @author rrohan419@gmail.com
//	 */
//	@Override
//	public FileRecordModel uploadSingleFile(String userUuid, FileGroup fileGroup, MultipartFile multipartFile,
//			Boolean validateDuplicate) {
//		String originalFileName = generateFileName(multipartFile);
//		String finalName = System.currentTimeMillis() + "-" + originalFileName;
//
//		return uploadAndSaveFileToS3(userUuid, fileGroup, multipartFile, originalFileName, finalName,
//				validateDuplicate);
//	}
//
//	private String generateFileName(MultipartFile multipartFile) {
//		return multipartFile.getOriginalFilename().toLowerCase().trim().replaceAll("\\s+", "-");
//	}
//
//	private FileRecordModel uploadAndSaveFileToS3(String userUuid, FileGroup fileGroup, MultipartFile multipartFile,
//			String originalFileName, String finalName, Boolean validateDuplicate) {
//
//		// Validate file size and name length
//		validateFileSize(multipartFile.getSize(), fileGroup);
//		validateFileNameLength(multipartFile.getOriginalFilename());
//
//		if (validateDuplicate) {
//			FileRecord existingRecord = fileRecordDao.fileRecordByUserUuidAndFileName(userUuid, originalFileName);
//			if (existingRecord != null) {
//				return mapper.convert(existingRecord, FileRecordModel.class);
//			}
//		}
//
//		String bucketName = (fileGroup == FileGroup.PROFILE) ? env.getProperty(AppConstant.S3_PUBLIC_BUCKET_NAME)
//				: env.getProperty(AppConstant.S3_BUCKET_NAME);
//
//		String folderName = fileGroup.folderName();
//		String region = env.getProperty(AppConstant.S3_REGION);
//		String fileLocation = String.format(env.getProperty(AppConstant.S3_BASE_URL), bucketName, region, folderName,
//				finalName);
//
//		PutObjectResponse putObjectResponse = uploadFileToS3(folderName, finalName, multipartFile, bucketName);
//
//		FileRecord fileRecord = buildFileRecord(originalFileName, finalName, fileLocation, multipartFile,
//				putObjectResponse.eTag(), userUuid);
//		fileRecord.setFileGroup(fileGroup);
//
//		fileRecord = fileRecordDao.saveFileRecord(fileRecord);
//
//		return mapper.convert(fileRecord, FileRecordModel.class);
//	}
//
//	private FileRecord buildFileRecord(String originalFileName, String finalName, String fileLocation,
//			MultipartFile multipartFile, String eTag, String userUuid) {
//
//		FileRecord fileRecord = new FileRecord();
//		fileRecord.setOriginalName(originalFileName);
//		fileRecord.setUuid(UUID.randomUUID().toString());
//		fileRecord.setFileLocation(fileLocation);
//		fileRecord.setFinalName(finalName);
//		fileRecord.setContentType(multipartFile.getContentType());
//		fileRecord.setSize(multipartFile.getSize());
//		fileRecord.setETag(eTag);
//		fileRecord.setUserUuid(userUuid);
//
//		return fileRecord;
//	}
//
//	private PutObjectResponse uploadFileToS3(String folderName, String finalName, MultipartFile multipartFile,
//			String bucketName) {
//
//		PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(bucketName)
//				.key(folderName + "/" + finalName).contentType(multipartFile.getContentType()).build();
//
//		try {
//			return s3Client.putObject(putObjectRequest, RequestBody.fromBytes(multipartFile.getBytes()));
//
//		} catch (AwsServiceException | SdkClientException | IOException e) {
//			throw new CustomException(env.getProperty(ExceptionConstant.FILE_UPLOAD_FAILED),
//					HttpStatus.INTERNAL_SERVER_ERROR);
//		}
//
//	}
//
//	private void validateFileSize(long size, FileGroup fileGroup) {
//
//		long maxSize = fileGroup == FileGroup.PROFILE
//				? Long.parseLong(env.getProperty(AppConstant.MAX_FILE_SIZE_PROFILE))
//				: Long.parseLong(env.getProperty(AppConstant.MAX_FILE_SIZE_DOCUMENT));
//
//		if (size > maxSize) {
//			throw new CustomException(
//					env.getProperty(ExceptionConstant.FILE_SIZE_EXCEEDED) + " " + (maxSize / 1024) / 1024,
//					HttpStatus.BAD_REQUEST);
//		}
//	}
//
//	private void validateFileNameLength(String fileName) {
//
//		int maxLength = Integer.parseInt(env.getProperty(AppConstant.MAX_FILE_NAME_LENGTH));
//
//		if (fileName != null && fileName.length() > maxLength) {
//			throw new CustomException(env.getProperty(ExceptionConstant.FILE_NAME_TOO_LONG) + " " + maxLength,
//					HttpStatus.BAD_REQUEST);
//		}
//	}
//
//	/**
//	 * @author rrohan419@gmail.com
//	 */
//	@Override
//	public PresignedUrlModel generatePresignedUrl(String userUuid, String fileUuid, String type) {
//		FileRecord fileRecord = fileRecordDao.fileRecordByUserAndUuid(userUuid, fileUuid);
//
//		Instant expiration = Instant.now().plus(
//				Integer.parseInt(env.getProperty(AppConstant.S3_PRESIGNED_URL_EXPIRATION_TIME)), ChronoUnit.MINUTES);
//
//		PresignedGetObjectRequest presignedGetObjectRequest = createPresignedGetObjectRequest(fileUuid,
//				Integer.parseInt(env.getProperty(AppConstant.S3_PRESIGNED_URL_EXPIRATION_TIME)),
//				env.getProperty(AppConstant.S3_BUCKET_NAME),
//				fileRecord.getFileGroup().folderName() + "/" + fileRecord.getFinalName());
//
//		return new PresignedUrlModel(fileRecord.getUuid(), presignedGetObjectRequest.url().toString(), expiration);
//	}
//
//	/**
//	 * @author rrohan419@gmail.com
//	 */
//	@Override
//	public void deleteFileFromS3(String userUuid, String dependentUserUuid, String fileUuid) {
//		FileRecord fileRecord = fileRecordDao.fileRecordByUserAndUuid(userUuid, fileUuid);
//
//		try {
//
//			DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
//					.bucket(env.getProperty(AppConstant.S3_BUCKET_NAME))
//					.key(fileRecord.getFileGroup().folderName() + "/" + fileRecord.getFinalName()).build();
//
//			s3Client.deleteObject(deleteObjectRequest);
//
//			fileRecordDao.deleteFile(fileRecord);
//
//		} catch (Exception e) {
//			throw new CustomException(env.getProperty(ExceptionConstant.FAILED_TO_DELETE_FILE),
//					HttpStatus.INTERNAL_SERVER_ERROR);
//		}
//
//	}
//
//	/**
//	 * @author rrohan419@gmail.com
//	 */
//	@Override
//	public FileDownloadModel downloadFileFromS3(String userUuid, String fileUuid) {
//		FileRecord fileRecord = fileRecordDao.fileRecordByUserAndUuid(userUuid, fileUuid);
//
//		try {
//
//			GetObjectRequest getObjectRequest = GetObjectRequest.builder()
//					.bucket(env.getProperty(AppConstant.S3_BUCKET_NAME))
//					.key(fileRecord.getFileGroup().folderName() + "/" + fileRecord.getFinalName()).build();
//
//			ResponseInputStream<GetObjectResponse> s3ObjectStream = s3Client.getObject(getObjectRequest);
//
//			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(s3ObjectStream.readAllBytes());
//
//			return new FileDownloadModel(fileRecord.getFinalName(), fileRecord.getContentType(),
//					new InputStreamResource(byteArrayInputStream));
//
//		} catch (Exception e) {
//			throw new CustomException(env.getProperty(ExceptionConstant.FAILED_TO_DOWNLOAD_FILE),
//					HttpStatus.INTERNAL_SERVER_ERROR);
//		}
//	}
//
//	/**
//	 * @author rrohan419@gmail.com
//	 */
//	@Override
//	public void deleteFileFromS3(FileRecord fileRecord) {
//		// TODO Auto-generated method stub
//
//	}
//
//	/**
//	 * @author rrohan419@gmail.com
//	 */
//	@Override
//	public FileRecordModel updateFileRecord(FileUpdateDto fileUpdateDto) {
//		FileRecord fileRecord = fileRecordDao.fileRecordByUuid(fileUpdateDto.getFileUuid());
//
//		if (fileRecord == null) {
//			throw new CustomException(env.getProperty(ExceptionConstant.FILE_NOT_FOUND), HttpStatus.NOT_FOUND);
//		}
//
//		fileRecord.setOriginalName(fileUpdateDto.getName());
//
//		return mapper.convert(fileRecordDao.saveFileRecord(fileRecord), FileRecordModel.class);
//	}
//
//	/**
//	 * @author rrohan419@gmail.com
//	 */
//	@Override
//	public List<PresignedUrlModel> generatePresignedUrls(String userUuid, Set<String> fileUuids) {
//		List<FileRecord> fileRecords = fileRecordDao.fileRecordsByUserAndUuids(userUuid, fileUuids);
//
//		int expirationTime = Integer.parseInt(env.getProperty(AppConstant.S3_PRESIGNED_URL_EXPIRATION_TIME));
//		Instant expiration = Instant.now().plus(expirationTime, ChronoUnit.MINUTES);
//
//		List<PresignedUrlModel> presignedUrls = new ArrayList<>();
//
//		for (FileRecord fileRecord : fileRecords) {
//			PresignedGetObjectRequest presignedGetObjectRequest = createPresignedGetObjectRequest(fileRecord.getUuid(),
//					expirationTime, env.getProperty(AppConstant.S3_BUCKET_NAME),
//					fileRecord.getFileGroup().folderName() + "/" + fileRecord.getFinalName());
//
//			presignedUrls.add(new PresignedUrlModel(fileRecord.getUuid(), presignedGetObjectRequest.url().toString(),
//					expiration));
//		}
//
//		return presignedUrls;
//	}
//
//	/**
//	 * @author rrohan419@gmail.com
//	 */
//	@Override
//	public PresignedGetObjectRequest createPresignedGetObjectRequest(String fileRecordUuid, Integer expirationTime,
//			String bucketName, String key) {
//		try {
//			return s3Presigner.presignGetObject(por -> {
//				por.signatureDuration(Duration.ofMinutes((expirationTime)));
//				por.getObjectRequest(gor -> gor.bucket(bucketName).key(key));
//			});
//
//		} catch (Exception e) {
//			throw new CustomException(env.getProperty(ExceptionConstant.FAILED_TO_GENERATE_URL) + "" + fileRecordUuid,
//					HttpStatus.INTERNAL_SERVER_ERROR);
//		}
//	}
//
//	/**
//	 * @author rrohan419@gmail.com
//	 */
//	@Override
//	public FileRecordModel uploadSingleFileWithCustomName(String userUuid, FileGroup fileGroup,
//			MultipartFile multipartFile, String aiFileId) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	/**
//	 * @author rrohan419@gmail.com
//	 */
//	@Override
//	public void deleteMultipleFilesFromS3(Set<String> fileRecordUuids) {
//		// TODO Auto-generated method stub
//
//	}
//
//	/**
//	 * @author rrohan419@gmail.com
//	 */
//	@Override
//	public CompletableFuture<List<FileRecordModel>> filterFileRecords(String userUuid, FilterDto filterDto) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	/**
//	 * @author rrohan419@gmail.com
//	 */
//	@Override
//	public PaginationModel<FileRecordModel> fileRecordsByUserUuid(String userUuid, Integer pageNo, Integer pageSize,
//			String sortBy, String filterBy, String searchText) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//}
