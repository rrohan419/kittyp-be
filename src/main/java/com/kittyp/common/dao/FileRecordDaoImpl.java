package com.kittyp.common.dao;

import java.util.List;
import java.util.Set;

import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.entity.FileRecord;
import com.kittyp.common.enums.FileGroup;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.model.IFileRecord;
import com.kittyp.common.repository.FileRecordRepository;

import lombok.RequiredArgsConstructor;

/**
 * @author Mindbowser | ashitosh.mane@mindbowser.com
 */
@Repository
@RequiredArgsConstructor
public class FileRecordDaoImpl implements FileRecordDao {

	private final FileRecordRepository fileRecordRepository;

	private final Environment environment;

	/**
	 * save fileRecord
	 * 
	 * @author Mindbowser | ashitosh.mane@mindbowser.com
	 * @param fileRecord
	 * @return {@link FileRecord}
	 */
	@Override
	public FileRecord saveFileRecord(FileRecord fileRecord) {

		try {
			return fileRecordRepository.save(fileRecord);
		} catch (Exception e) {
			throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR.name(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	/**
	 * File record by userUuid and fileUuid or caregiverUuid and userUuid and
	 * fileUuid
	 * 
	 * @author
	 * @param userUuid
	 * @param dependentUserUuid
	 * @param fileUuid
	 * @return {@link FileRecord}
	 */
	@Override
	public FileRecord fileRecordByUserAndUuid(String userUuid, String fileUuid) {

			return fileRecordRepository.findByUserUuidAndUuid(userUuid, fileUuid)
					.orElseThrow(() -> new CustomException(environment.getProperty(ExceptionConstant.FILE_NOT_FOUND),
							HttpStatus.NOT_FOUND));


	}

	/**
	 * Delete file
	 * 
	 * @author Mindbowser | ashitosh.mane@mindbowser.com
	 * @param fileRecord
	 * @return {@link FileRecord}
	 */
	@Override
	public void deleteFile(FileRecord fileRecord) {

		try {
			fileRecordRepository.delete(fileRecord);
		} catch (Exception e) {
			throw new CustomException(environment.getProperty(ExceptionConstant.INTERNAL_SERVER_ERROR),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * get list of file record by user uuid and fileCollection uuid is null
	 * 
	 * @author rohan.shrivastava@mindbowser.com
	 *
	 * @param userUuid
	 * @return {@link List}
	 * @see FileRecord
	 */
	@Override
	public List<FileRecord> fileRecordByUserUuidAndFileCollectionIsNull(String userUuid) {
		try {
			return fileRecordRepository.filesByUserUuidAndFileCollectionUuidNullAndFileGroupIn(userUuid,
					Set.of(FileGroup.DOCUMENT.name(), FileGroup.OTHER.name()));
		} catch (Exception e) {
			throw new CustomException(environment.getProperty(ExceptionConstant.INTERNAL_SERVER_ERROR),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 
	 * @author rohan.shrivastava@mindbowser.com
	 *
	 * @param uuid
	 * @return {@link FileRecord}
	 */
	@Override
	public FileRecord fileRecordByUuid(String uuid) {
		try {
			return fileRecordRepository.findByUuid(uuid);
		} catch (Exception e) {
			throw new CustomException(environment.getProperty(ExceptionConstant.INTERNAL_SERVER_ERROR), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * File records by userUuid and fileUuids or caregiverUuid and userUuid and
	 * fileUuids
	 * 
	 * @author Mindbowser | ashitosh.mane@mindbowser.com
	 * @param userUuid
	 * @param fileUuids
	 * @return {@link FileRecord}
	 */
	@Override
	public List<FileRecord> fileRecordsByUserAndUuids(String userUuid,
			Set<String> fileUuids) {

		return fileRecordRepository.findByUuidIn(fileUuids);
	}

	/**
	 * 
	 * @author rohan.shrivastava@mindbowser.com
	 *
	 * @param specification
	 * @return {@link List}
	 * @see FileRecord
	 */
	@Override
	public List<FileRecord> filterFileRecord(Specification<FileRecord> specification) {
		try {
			return fileRecordRepository.findAll(specification);
		} catch (Exception e) {
			throw new CustomException(environment.getProperty(ExceptionConstant.INTERNAL_SERVER_ERROR),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * FileRecords by uuids
	 * 
	 * @author Mindbowser | ashitosh.mane@mindbowser.com
	 *
	 * @param uuids
	 * @return {@link List}
	 * @see FileRecord
	 */
	@Override
	public List<FileRecord> fileRecordsByUuids(Set<String> uuids) {
		try {
			return fileRecordRepository.findByUuidIn(uuids);
		} catch (Exception e) {
			throw new CustomException(environment.getProperty(ExceptionConstant.INTERNAL_SERVER_ERROR),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Delete fileRecords
	 * 
	 * @author Mindbowser | ashitosh.mane@mindbowser.com
	 *
	 * @param fileRecords
	 */
	@Override
	public void deleteFileRecords(List<FileRecord> fileRecords) {
		try {
			fileRecordRepository.deleteAll(fileRecords);
		} catch (Exception e) {
			throw new CustomException(environment.getProperty(ExceptionConstant.INTERNAL_SERVER_ERROR),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 
	 * @author rohan.shrivastava@mindbowser.com
	 *
	 * @param userUuid
	 * @param fileName
	 * @return {@link FileRecord}
	 */
	@Override
	public FileRecord fileRecordByUserUuidAndFileName(String userUuid, String fileName) {
		try {
			return fileRecordRepository.findFirstByUserUuidAndFinalName(userUuid, fileName);
		} catch (Exception e) {
			throw new CustomException(environment.getProperty(ExceptionConstant.INTERNAL_SERVER_ERROR),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
     * 
     * @author rohan.shrivastava@mindbowser.com
     *
     * @param userUuid
     * @param searchWord
     * @return {@link List}
     * @see IFileRecord
     */
	@Override
	public List<IFileRecord> filterByFileRecords(String userUuid, String searchWord) {
		try {
            return fileRecordRepository.filterFileRecords(userUuid, searchWord);
        } catch (Exception e) {
            throw new CustomException(environment.getProperty(ExceptionConstant.INTERNAL_SERVER_ERROR),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
	}

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
	@Override
	public Page<IFileRecord> fileRecordByFileCollectionUuid(String userUuid, String searchText, Pageable pageable) {
		try {
            return fileRecordRepository.filterFileRecordsByFileCollection(userUuid, searchText, pageable);
        } catch (Exception e) {
            throw new CustomException(environment.getProperty(ExceptionConstant.INTERNAL_SERVER_ERROR),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
	}

	/**
     * 
     * @author rohan.shrivastava@mindbowser.com
     *
     * @param userUuid
     * @param searchWord
     * @return {@link List}
     * @see IFileRecord
     */
	@Override
	public List<IFileRecord> fileRecordsByFileCollectionUuid(String userUuid) {
		try {
            return fileRecordRepository.fileRecordsByFileCollection(userUuid);
        } catch (Exception e) {
            throw new CustomException(environment.getProperty(ExceptionConstant.INTERNAL_SERVER_ERROR),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
	}

	/**
     * 
     * @author rohan.shrivastava@mindbowser.com
     *
     * @param userUuid
     * @param fileRecordUuid
     * @return {@link IFileRecord}
     */
	@Override
	public IFileRecord fileRecordByUuid(String userUuid, String fileRecordUuid) {
		try {
            return fileRecordRepository.fileRecordByUuid(userUuid, fileRecordUuid);
        } catch (Exception e) {
            throw new CustomException(environment.getProperty(ExceptionConstant.INTERNAL_SERVER_ERROR),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
	}
}
