package com.kittyp.common.entity;

/**
 * Entities whose public {@code uuid} is allocated at persist time.
 */
public interface HasPublicId {

    String getUuid();

    void setUuid(String uuid);
}
