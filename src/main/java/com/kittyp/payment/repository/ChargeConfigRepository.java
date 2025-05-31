/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.payment.entity.ChargeConfig;
import com.kittyp.payment.enums.ChargeType;

/**
 * @author rrohan419@gmail.com 
 */
public interface ChargeConfigRepository extends JpaRepository<ChargeConfig, Long>{

	Optional<ChargeConfig> findByType(ChargeType chargeType);
}
