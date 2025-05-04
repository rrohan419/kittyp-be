/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.entity;

import org.hibernate.annotations.DynamicUpdate;

import com.kittyp.common.enums.FileGroup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Entity
@Table(name = "file_records")
@DynamicUpdate
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class FileRecord extends BaseEntity{

	private static final long serialVersionUID = 1L;

	@Column(nullable = false, unique = true)
    private String uuid;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String fileLocation;

    @Column
    private String originalName;

    @Column(nullable = false)
    private String finalName;

    @Column
    private String contentType;

    @Column
    private Long size;

    @Column
    private String eTag;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FileGroup fileGroup;

    @Column(nullable = false)
    private String userUuid;
}
