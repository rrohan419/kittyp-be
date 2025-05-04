package com.kittyp.common.enums;

/**
 * @author Mindbowser | ashitosh.mane@mindbowser.com
 */
public enum FileGroup {
    PROFILE("profile"),
    DOCUMENT("document"),
    VIDEO("video"),
    AUDIO("audio"),
    OTHER("other"),
    IMAGE("image");

    private final String folderName;

    private FileGroup(String folderName) {
        this.folderName = folderName;
    }

    public String folderName() {
        return this.folderName;
    }
}