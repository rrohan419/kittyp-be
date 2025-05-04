package com.kittyp.common.model;

import java.util.Date;

/**
 * @author rrohan419@gmail.com
 */
public interface IFileRecord {

	
	public String getUuid();
	
	public String getFileLocation();

	public String getOriginalName();

	public String getFinalName();

	public Long getSize();

	public String getContentType();
    
	public Date getCreatedAt();
    
	public Date getUpdatedAt();
    
	public Boolean getIsPasswordProtected();
}
