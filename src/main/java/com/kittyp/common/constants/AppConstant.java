/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.common.constants;

/**
 * @author rrohan419@gmail.com 
 */
public class AppConstant {
	private AppConstant() {}
	
	public static final String KITTYP_EMAIL_TEMPLATE_LOGO= "https://www.kittyp.in/android-chrome-512x512.png";

		//AWS KEYS
		public static final String AWS_INVOICE_BUCKET_NAME="aws.invoice.bucket.name";
		public static final String S3_REGION="amazon.s3.region";
		public static final String AWS_ACCESS_KEY_ID="aws.access.key.id";
		public static final String AWS_SECRET_KEY = "aws.secret.key";
		
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
	    
	    // Zoho
	    public static final String KITTYP = "KittyP";
		public static final String ZOHO_BEARER_KEY = "Zoho-enczapikey ";
	    public static final String ZOHO_API_KEY="zoho.api.key";
	    public static final String KITTYP_MAIL_ID ="kittyp.mail.id";
	    public static final String ZOHO_EMAIL_SEND_URL="zoho.email.send.url.with.template";
	    public static final String ZOHO_WELCOME_EMAIL_TEMPLATE_ID="zoho.welcome.template.id";
}
