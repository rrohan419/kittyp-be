package com.kittyp.common.dao;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.kittyp.common.entity.FileRecord;
import com.kittyp.common.model.IFileRecord;


/**
 * @author Mindbowser | ashitosh.mane@mindbowser.com
 */
public interface FileRecordDao {

    /**
     * save fileRecord
     * 
     * @author 
     * @param fileRecord
     * @return {@link FileRecord}
     */
    FileRecord saveFileRecord(FileRecord fileRecord);

    /**
     * File record by userUuid and fileUuid or caregiverUuid and userUuid and
     * fileUuid
     * 
     * @author 
     * @param userUuid
     * @param fileUuid
     * @return {@link FileRecord}
     */
    FileRecord fileRecordByUserAndUuid(String userUuid, String fileUuid);


    /**
     * Delete file
     * 
     * @author Mindbowser | ashitosh.mane@mindbowser.com
     * @param fileRecord
     * @return {@link FileRecord}
     */
    void deleteFile(FileRecord fileRecord);

    /**
     * get list of file record by user uuid and fileCollection uuid is null
     * 
     * @author rohan.shrivastava@mindbowser.com
     *
     * @param userUuid
     * @return {@link List}
     * @see FileRecord
     */
    List<FileRecord> fileRecordByUserUuidAndFileCollectionIsNull(String userUuid);

    /**
     * 
     * @author rohan.shrivastava@mindbowser.com
     *
     * @param uuid
     * @return {@link FileRecord}
     */
    FileRecord fileRecordByUuid(String uuid);

    /**
     * File records by userUuid and fileUuids or caregiverUuid and userUuid and
     * fileUuids
     * 
     * @author 
     * @param userUuid
     * @param fileUuids
     * @return {@link FileRecord}
     */
    List<FileRecord> fileRecordsByUserAndUuids(String userUuid, Set<String> fileUuids);

    /**
     * 
     * @author rohan.shrivastava@mindbowser.com
     *
     * @param specification
     * @return {@link List}
     * @see FileRecord
     */
    List<FileRecord> filterFileRecord(Specification<FileRecord> specification);

    /**
     * FileRecords by uuids
     * 
     * @author Mindbowser | ashitosh.mane@mindbowser.com
     *
     * @param uuids
     * @return {@link List}
     * @see FileRecord
     */
    List<FileRecord> fileRecordsByUuids(Set<String> uuids);

    /**
     * Delete fileRecords
     * 
     * @author Mindbowser | ashitosh.mane@mindbowser.com
     *
     * @param fileRecords
     */
    void deleteFileRecords(List<FileRecord> fileRecords);

    /**
     * 
     * @author rohan.shrivastava@mindbowser.com
     *
     * @param userUuid
     * @param fileName
     * @return {@link FileRecord}
     */
    FileRecord fileRecordByUserUuidAndFileName(String userUuid, String fileName);
    
    /**
     * 
     * @author rohan.shrivastava@mindbowser.com
     *
     * @param userUuid
     * @param searchWord
     * @return {@link List}
     * @see IFileRecord
     */
    List<IFileRecord> filterByFileRecords(String userUuid, String searchWord);

    /**
     * 
     * @author rohan.shrivastava@mindbowser.com
     *
     * @param userUuid
     * @param searchText
     * @param fileCollectionUuid
     * @param pageable
     * @return {@link Page}
     * @see IFileRecord
     */
    Page<IFileRecord> fileRecordByFileCollectionUuid(String userUuid, String searchText, Pageable pageable);

    /**
     * 
     * @author rohan.shrivastava@mindbowser.com
     *
     * @param userUuid
     * @param searchWord
     * @return {@link List}
     * @see IFileRecord
     */
    List<IFileRecord> fileRecordsByFileCollectionUuid(String userUuid);
    
    /**
     * 
     * @author rohan.shrivastava@mindbowser.com
     *
     * @param userUuid
     * @param fileRecordUuid
     * @return {@link IFileRecord}
     */
    IFileRecord fileRecordByUuid(String userUuid, String fileRecordUuid);
}