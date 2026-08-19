package com.kittyp.common.entity;

import com.kittyp.common.util.AlphanumericIdService;

import jakarta.persistence.PrePersist;

/**
 * Assigns a 6-character public id on insert when {@code uuid} is blank.
 * Does not overwrite an id that is already present.
 */
public class PublicIdEntityListener {

    @PrePersist
    public void assignPublicId(Object entity) {
        if (!(entity instanceof HasPublicId target)) {
            return;
        }
        if (target.getUuid() != null && !target.getUuid().isBlank()) {
            return;
        }
        // Generate only — do not query the DB here. Hibernate AUTO flush during
        // @PrePersist would INSERT this row while uuid is still null.
        target.setUuid(AlphanumericIdService.generate());
    }
}
