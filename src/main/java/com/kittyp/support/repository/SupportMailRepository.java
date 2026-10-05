package com.kittyp.support.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.support.entity.SupportMail;

public interface SupportMailRepository extends JpaRepository<SupportMail, Long> {

	Optional<SupportMail> findByZohoMessageId(String zohoMessageId);

	Optional<SupportMail> findFirstBySupportIdAndOpeningTrue(String supportId);

	Optional<SupportMail> findFirstByThreadId(String threadId);

	/**
	 * Highest opening sequence for today's prefix ({@code KIT-YYYYMMDD-%}).
	 * The five-digit number starts at 1-based position 14.
	 */
	@Query(value = """
			SELECT COALESCE(MAX(CAST(SUBSTRING(support_id FROM 14 FOR 5) AS integer)), 0)
			FROM support_mail
			WHERE opening = true AND support_id LIKE :prefix
			""", nativeQuery = true)
	Number maxOpeningSequence(@Param("prefix") String prefix);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE SupportMail m SET m.ackSent = true WHERE m.id = :id AND m.ackSent = false")
	int claimAck(@Param("id") Long id);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE SupportMail m SET m.ackSent = false WHERE m.id = :id")
	int releaseAck(@Param("id") Long id);
}
