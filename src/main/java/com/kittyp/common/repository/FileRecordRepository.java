package com.kittyp.common.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.common.entity.FileRecord;
import com.kittyp.common.model.IFileRecord;


/**
 * @author Mindbowser | ashitosh.mane@mindbowser.com
 */
public interface FileRecordRepository extends JpaRepository<FileRecord, Long>, JpaSpecificationExecutor<FileRecord> {

	Optional<FileRecord> findByUserUuidAndUuid(String userUuid, String fileUuid);

//	Optional<FileRecord> findByCaregiverUuidAndUserUuidAndUuid(String caregiverUuid, String userUuid, String fileUuid);

	@Query(value = "SELECT * FROM file_records fr WHERE fr.user_uuid = :userUuid AND fr.file_collection_uuid IS NULL AND fr.file_group IN (:fileGroups)", nativeQuery = true)
	List<FileRecord> filesByUserUuidAndFileCollectionUuidNullAndFileGroupIn(@Param("userUuid") String userUuid,
			@Param("fileGroups") Set<String> fileGroups);

	FileRecord findByUuid(String uuid);

	List<FileRecord> findByUserUuidAndUuidIn(String userUuid, Set<String> fileUuids);

//	List<FileRecord> findByCaregiverUuidAndUserUuidAndUuidIn(String userUuid, String dependentUserUuid,
//			Set<String> fileUuids);

	FileRecord findFirstByUserUuidAndFinalName(String userUuid, String finalName);

	List<FileRecord> findByUuidIn(Set<String> uuids);

//	Optional<FileRecord> findByUserUuidAndAiFileId(String userUuid, String fileUuid);

//	Optional<FileRecord> findByCaregiverUuidAndUserUuidAndAiFileId(String caregiverUuid, String userUuid,
//			String fileUuid);
	
	@Query(value = "SELECT " +
	        "   fr.original_name, " +
	        "   fr.file_collection_uuid, " +
	        "   fr.size, " +
	        "   fr.uuid, " +
	        "   fr.file_location, " +
	        "   fr.content_type, " +
	        "   fr.updated_at, " +
	        "   fr.created_at, " +
	        "   fr.is_password_protected, " +
	        "   fr.final_name " +
	        "FROM " +
	        "   file_records fr " +
	        "WHERE " +
	        "   fr.user_uuid = ?1 " +
	        "   AND fr.file_collection_uuid IS NOT NULL " +
	        "   AND LOWER(fr.original_name) LIKE LOWER(CONCAT('%', ?2, '%'))",
	        nativeQuery = true)
	List<IFileRecord> filterFileRecords(String userUuid, String originalName);

	@Query(value = "SELECT " +
	        "   fr.original_name, " +
	        "   fr.size, " +
	        "   fr.uuid, " +
	        "   fr.file_location, " +
	        "   fr.content_type, " +
	        "   fr.updated_at, " +
	        "   fr.created_at, " +
	        "   fr.final_name " +
	        "FROM " +
	        "   file_records fr " +
	        "WHERE " +
	        "   fr.user_uuid = ?1 " +
	        "   AND LOWER(fr.original_name) LIKE LOWER(CONCAT('%', ?3, '%'))", 
	        nativeQuery = true)
	Page<IFileRecord> filterFileRecordsByFileCollection(String userUuid, String originalName, Pageable pageable);

	@Query(value = "SELECT " +
	        "   fr.original_name, " +
	        "   fr.file_collection_uuid, " +
	        "   fr.size, " +
	        "   fr.uuid, " +
	        "   fr.file_location, " +
	        "   fr.content_type, " +
	        "   fr.updated_at, " +
	        "   fr.created_at, " +
	        "   fr.final_name " +
	        "FROM " +
	        "   file_records fr " +
	        "WHERE " +
	        "   fr.user_uuid = (?1) ",
	        nativeQuery = true)
	List<IFileRecord> fileRecordsByFileCollection(String userUuid);
	
	@Query(value = "SELECT " +
	        "   fr.original_name, " +
	        "   fr.file_collection_uuid, " +
	        "   fr.size, " +
	        "   fr.uuid, " +
	        "   fr.file_location, " +
	        "   fr.content_type, " +
	        "   fr.updated_at, " +
	        "   fr.created_at, " +
	        "   fr.is_password_protected, " +
	        "   fr.final_name " +
	        "FROM " +
	        "   file_records fr " +
	        "WHERE " +
	        "   fr.user_uuid = (?1) " +
	        "   AND fr.uuid = (?2) ",
	        nativeQuery = true)
	IFileRecord fileRecordByUuid(String userUuid, String fileRecordUuid);
	
}