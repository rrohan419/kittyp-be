/**
 * 
 */
package com.kittyp.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class FileUpdateDto {

	@NotBlank
	private String fileUuid;
	
	@NotBlank
	private String name;
}
