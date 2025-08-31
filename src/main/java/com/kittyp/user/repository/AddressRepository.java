package com.kittyp.user.repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.user.entity.Address;


public interface AddressRepository extends JpaRepository<Address, Long> {
    Address findByUuid(String uuid);
    void deleteByUuid(String uuid);

    List<Address> findByUserUuid(String userUuid, Sort sort);
}
