/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.kittyp.common.exception.CustomException;
import com.kittyp.payment.entity.ChargeConfig;
import com.kittyp.payment.enums.ChargeType;
import com.kittyp.payment.repository.ChargeConfigRepository;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Service
@RequiredArgsConstructor
public class ChargeServiceImpl implements ChargeService{

	private final ChargeConfigRepository chargeRepo;

	@Override
    public BigDecimal calculateCharge(ChargeType type, BigDecimal baseAmount) {
        ChargeConfig config = chargeRepo.findByType(type)
                .orElseThrow(() -> new CustomException("Charge config missing for " + type));

        if (config.isPercentage()) {
            return baseAmount.multiply(config.getRate());
        } else {
            return config.getRate();
        }
    }

	@Override
    public BigDecimal calculateTotalCharges(BigDecimal baseAmount, List<ChargeType> types) {
        return types.stream()
                .map(type -> calculateCharge(type, baseAmount))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

	@Override
    public Map<ChargeType, BigDecimal> chargeBreakdown(BigDecimal baseAmount, List<ChargeType> types) {
        return types.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        type -> calculateCharge(type, baseAmount)
                ));
    }
}
