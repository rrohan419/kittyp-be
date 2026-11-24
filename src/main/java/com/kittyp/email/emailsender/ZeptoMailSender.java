/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.email.emailsender;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.kittyp.common.constants.AppConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.email.dto.EmailAddress;
import com.kittyp.email.dto.ZeptoMailDto;
import com.kittyp.email.dto.Recipient;
import com.kittyp.email.dto.ZohoMailRequest;
import com.kittyp.email.model.ZeptoMailResponseModel;
import com.kittyp.email.service.ClinicEmailUsageService;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com
 */
@Component("zeptoMail")
@RequiredArgsConstructor
public class ZeptoMailSender implements IEmailSender<ZeptoMailDto, ZeptoMailResponseModel> {

	private final RestClient restClient;
	private final Environment env;
	private final ClinicEmailUsageService clinicEmailUsageService;

	/**
	 * @author rrohan419@gmail.com
	 */
	@SuppressWarnings("null")
	@Override
	public ZeptoMailResponseModel sendEmail(ZeptoMailDto zeptoMailDto) {
		ZohoMailRequest request = new ZohoMailRequest();
        request.setTemplateKey(zeptoMailDto.getTemplateKey());
        if (zeptoMailDto.getMessage() != null && !zeptoMailDto.getMessage().isBlank()) {
			request.setTemplateKey(null);
			request.setTextBody(zeptoMailDto.getMessage());
			request.setSubject(zeptoMailDto.getSubject());
        }
//        request.setBounceAddress("bounce@yourdomain.com");
        Map<String, Object> mergeInfo = new HashMap<>();
        if (zeptoMailDto.getMergeInfo() != null) {
            mergeInfo.putAll(zeptoMailDto.getMergeInfo());
        }
        mergeInfo.put("current_year", LocalDate.now().getYear());
        mergeInfo.put("logo_url", AppConstant.KITTYP_EMAIL_TEMPLATE_LOGO);
        request.setMergeInfo(mergeInfo);
        Object supportId = mergeInfo.get("Support_Id");
        if (supportId != null && !supportId.toString().isBlank()) {
            request.setClientReference(supportId.toString().trim());
        }
        if (zeptoMailDto.getReplyToEmail() != null && !zeptoMailDto.getReplyToEmail().isBlank()) {
            String replyName = zeptoMailDto.getReplyToName() == null || zeptoMailDto.getReplyToName().isBlank()
                    ? zeptoMailDto.getReplyToEmail().trim()
                    : zeptoMailDto.getReplyToName().trim();
            request.setReplyTo(List.of(new EmailAddress(zeptoMailDto.getReplyToEmail().trim(), replyName)));
        }
        request.setFrom(new EmailAddress(env.getProperty(AppConstant.KITTYP_MAIL_ID), AppConstant.KITTYP));
        request.setTo(List.of(new Recipient(new EmailAddress(zeptoMailDto.getRecipientEmail(), zeptoMailDto.getRecipientName()))));

        String sendUrl = zeptoMailDto.getMessage() != null && !zeptoMailDto.getMessage().isBlank()
				? env.getProperty("zoho.email.send.url", env.getProperty(AppConstant.ZOHO_EMAIL_SEND_URL))
				: env.getProperty(AppConstant.ZOHO_EMAIL_SEND_URL);
        ClinicEmailUsageService.Reservation reservation = null;
        if (zeptoMailDto.getClinicAccountId() != null) {
            reservation = clinicEmailUsageService.reserve(zeptoMailDto.getClinicAccountId(),
                    zeptoMailDto.getRecipientEmail(), emailType(zeptoMailDto));
            if (!reservation.allowed()) {
                throw new CustomException("This practice has used its "
                        + ClinicEmailUsageService.MONTHLY_ALLOWANCE + " monthly email credits",
                        HttpStatus.TOO_MANY_REQUESTS);
            }
        }

        ResponseEntity<ZeptoMailResponseModel> responseEntity;
        try {
            responseEntity = restClient.post().uri(sendUrl)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, env.getProperty(AppConstant.ZOHO_API_KEY))
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toEntity(ZeptoMailResponseModel.class);
        } catch (RuntimeException sendError) {
            if (reservation != null) {
                try {
                    clinicEmailUsageService.markFailed(reservation.activityId(), "PROVIDER_REJECTED");
                } catch (RuntimeException trackingError) {
                    sendError.addSuppressed(trackingError);
                }
            }
            throw sendError;
        }
        ZeptoMailResponseModel response = responseEntity.getBody();
        if (reservation != null) {
            clinicEmailUsageService.markAccepted(reservation.activityId(),
                    response == null ? null : response.getRequestId());
        }
        return response;
	}

    private static String emailType(ZeptoMailDto mail) {
        if (mail.getType() != null && !mail.getType().isBlank()) {
            return mail.getType();
        }
        if (mail.getSubject() != null && !mail.getSubject().isBlank()) {
            return mail.getSubject();
        }
        return mail.getTemplateKey();
    }

}
