/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.springframework.web.multipart.MultipartFile;

import com.kittyp.common.dto.FileUpdateDto;
import com.kittyp.common.dto.FilterDto;
import com.kittyp.common.entity.FileRecord;
import com.kittyp.common.enums.FileGroup;
import com.kittyp.common.model.FileDownloadModel;
import com.kittyp.common.model.FileRecordModel;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.common.model.PresignedUrlModel;

import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * @author rrohan419@gmail.com 
 */
public interface FileService {

	FileRecordModel uploadSingleFile(String userUuid, FileGroup fileGroup,
			MultipartFile multipartFile, Boolean validateDuplicate);

	/**
	 * Generate presigned url
	 * 
	 * @author Mindbowser | ashitosh.mane@mindbowser.com
	 * @param userUuid
	 * @param dependentUserUuid
	 * @param fileUuid
	 * @return {@link PresignedUrlModel}
	 */

	PresignedUrlModel generatePresignedUrl(String userUuid, String fileUuid, String type);

	/**
	 * Delete file from S3
	 * 
	 * @author Mindbowser | ashitosh.mane@mindbowser.com
	 * @param userUuid
	 * @param dependentUserUuid
	 * @param fileUuid
	 */
	void deleteFileFromS3(String userUuid, String dependentUserUuid, String fileUuid);

	/**
	 * Download file from S3
	 * 
	 * @author rrohan419@gmaill.com
	 * @param userUuid
	 * @param dependentUserUuid
	 * @param fileUuid
	 * @return {@link FileDownloadModel}
	 */
	FileDownloadModel downloadFileFromS3(String userUuid, String fileUuid);

	/**
	 * Delete file from S3 and not from the file record
	 * 
	 * @author rrohan419@gmaill.com
	 * @param fileRecord
	 */
	void deleteFileFromS3(FileRecord fileRecord);

	/**
	 * update file name
	 * 
	 * @author rrohan419@gmaill.com
	 *
	 * @param fileUpdateDto
	 * @return {@link FileRecordModel}
	 */
	FileRecordModel updateFileRecord(FileUpdateDto fileUpdateDto);

	/**
	 * Generate presigned urls
	 * 
	 * @author rrohan419@gmaill.com
	 * @param userUuid
	 * @param dependentUserUuid
	 * @param fileUuids
	 * @return {@link List}
	 * @see PresignedUrlModel
	 */
	List<PresignedUrlModel> generatePresignedUrls(String userUuid,
			Set<String> fileUuids);

	/**
	 * Create presigned getObjectRequest
	 * 
	 * @author rrohan419@gmaill.com
	 * @param fileRecordUuid
	 * @param expirationTime
	 * @param bucketName
	 * @param key
	 * @return {@link PresignedGetObjectRequest}
	 */
	PresignedGetObjectRequest createPresignedGetObjectRequest(String fileRecordUuid, Integer expirationTime,
			String bucketName, String key);

	/**
	 * Upload single file on s3 with custom name
	 * 
	 * @author rrohan419@gmaill.com
	 * @param userUuid
	 * @param dependentUserUuid
	 * @param fileGroup
	 * @param multipartFile
	 * @return {@link FileRecordModel}
	 */
	FileRecordModel uploadSingleFileWithCustomName(String userUuid,
			FileGroup fileGroup, MultipartFile multipartFile, String aiFileId);

	/**
	 * Delete multiple files from S3
	 * 
	 * @author rrohan419@gmaill.com
	 * @param fileRecordUuids
	 */
	void deleteMultipleFilesFromS3(Set<String> fileRecordUuids);
	
	/**
	 * 
	 * @author rrohan419@gmaill.com
	 *
	 * @param userUuid
	 * @param filterDto
	 * @return {@link CompletableFuture}
	 * @see List
	 * @see FileRecordModel
	 */
	CompletableFuture<List<FileRecordModel>> filterFileRecords(String userUuid, FilterDto filterDto);
	
	/**
	 * 
	 * @author rohan.shrivastava@mindbowser.com
	 *
	 * @param userUuid
	 * @param pageNo
	 * @param pageSize
	 * @param sortBy
	 * @param filterBy
	 * @param searchText
	 * @param fileCollectionUuid
	 * @return {@link PaginationModel}
	 * @see FileRecordModel
	 */
	PaginationModel<FileRecordModel> fileRecordsByUserUuid(String userUuid, Integer pageNo, Integer pageSize,
			String sortBy, String filterBy, String searchText);
}
