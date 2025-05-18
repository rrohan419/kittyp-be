/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * @author rrohan419@gmail.com 
 */
@Data
public class ZeptoMailResponseModel {

	private List<ZeptoMailData> data;
	private String message;
	@JsonProperty("request_id")
	private String requestId;
	private String object;
	
	
	@Data
	public static class ZeptoMailData{
		private String code;
		private String message;
		@JsonProperty("additional_info")
		private List<String> additionalInfo;
	}
}
