/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.emailsender;

import java.util.List;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.kittyp.common.constants.AppConstant;
import com.kittyp.email.dto.EmailAddress;
import com.kittyp.email.dto.ZeptoMailDto;
import com.kittyp.email.dto.Recipient;
import com.kittyp.email.dto.ZohoMailRequest;
import com.kittyp.email.model.ZeptoMailResponseModel;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com
 */
@Component("zeptoMail")
@RequiredArgsConstructor
public class ZeptoMailSender implements IEmailSender<ZeptoMailDto, ZeptoMailResponseModel> {

	private final RestClient restClient;
	private final Environment env;

	/**
	 * @author rrohan419@gmail.com
	 */
	@SuppressWarnings("null")
	@Override
	public ZeptoMailResponseModel sendEmail(ZeptoMailDto zeptoMailDto) {
		ZohoMailRequest request = new ZohoMailRequest();
        request.setTemplateKey(zeptoMailDto.getTemplateKey());
//        request.setBounceAddress("bounce@yourdomain.com");

        request.setFrom(new EmailAddress(env.getProperty(AppConstant.KITTYP_MAIL_ID), AppConstant.KITTYP));
        request.setTo(List.of(new Recipient(new EmailAddress(zeptoMailDto.getRecipientEmail(), zeptoMailDto.getRecipientName()))));
        request.setMergeInfo(zeptoMailDto.getMergeInfo());

        ResponseEntity<ZeptoMailResponseModel> responseEntity = restClient.post().uri(env.getProperty(AppConstant.ZOHO_EMAIL_SEND_URL))
        		.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, env.getProperty(AppConstant.ZOHO_API_KEY))
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(ZeptoMailResponseModel.class);
        return responseEntity.getBody();
	}

}
