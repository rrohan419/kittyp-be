/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.emailsender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		String templateKey = zeptoMailDto.getTemplateKey();
		String templateAlias = zeptoMailDto.getTemplateAlias();
		if (templateKey != null && !templateKey.isBlank()) {
			request.setTemplateKey(templateKey);
		} else if (templateAlias != null && !templateAlias.isBlank()) {
			request.setTemplateAlias(templateAlias);
		}
		if (zeptoMailDto.getSubject() != null && !zeptoMailDto.getSubject().isBlank()) {
			request.setSubject(zeptoMailDto.getSubject());
		}
        Map<String, Object> mergeInfo = new HashMap<>();
        if (zeptoMailDto.getMergeInfo() != null) {
            mergeInfo.putAll(zeptoMailDto.getMergeInfo());
        }
        request.setMergeInfo(mergeInfo);
        request.setFrom(new EmailAddress(env.getProperty(AppConstant.KITTYP_MAIL_ID), AppConstant.KITTYP));
        request.setTo(List.of(new Recipient(
                new EmailAddress(zeptoMailDto.getRecipientEmail(), zeptoMailDto.getRecipientName()))));
        if (zeptoMailDto.getAttachments() != null && !zeptoMailDto.getAttachments().isEmpty()) {
            request.setAttachments(zeptoMailDto.getAttachments());
        }

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
