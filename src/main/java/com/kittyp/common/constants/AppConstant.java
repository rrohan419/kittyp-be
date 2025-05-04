/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.constants;

/**
 * @author rrohan419@gmail.com 
 */
public class AppConstant {
	private AppConstant() {}
	
	//AWS KEYS
		public static final String AWS_SECRET_KEY = "aws.secret.key";
		public static final String AWS_ACCESS_KEY = "aws.access.key";
		
		public static final String AWS_BUCKET_NAME="aws.bucket.name";
		public static final String S3_REGION="amazon.s3.region";
		
		// AWS S3 constants
	    public static final String S3_BUCKET_NAME = "amazon.s3.bucket.name";
	    public static final String S3_BASE_URL = "amazon.s3.base.url";
	    public static final String S3_PRESIGNED_URL_EXPIRATION_TIME = "amazon.s3.presignedurl.expiration.minutes";
	    public static final String S3_PUBLIC_BUCKET_NAME = "amazon.s3.public.bucket.name";

	    // File
	    public static final String MAX_FILE_SIZE_DOCUMENT = "max.file.size.document";
	    public static final String MAX_FILE_SIZE_PROFILE = "max.file.size.profile";
	    public static final String MAX_FILE_NAME_LENGTH = "max.file.name.length";
	    public static final String ATTACHMENT_HEADER = "attachment;";
	    public static final String FILE_NAME = "filename=";
}
