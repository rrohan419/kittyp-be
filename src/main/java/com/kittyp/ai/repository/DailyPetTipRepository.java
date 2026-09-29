package com.kittyp.ai.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.kittyp.ai.entity.DailyPetTip;

public interface DailyPetTipRepository extends JpaRepository<DailyPetTip, Long> {

	@Query("select t.tip from DailyPetTip t order by t.id asc")
	List<String> findAllTipsOrdered();
}
