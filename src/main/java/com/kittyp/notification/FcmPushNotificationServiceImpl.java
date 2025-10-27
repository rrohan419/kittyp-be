package com.kittyp.notification;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.kittyp.common.constants.AppConstant;
import com.kittyp.user.dao.UserFcmTokenDao;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FcmPushNotificationServiceImpl implements FcmPushNotificationService{

    private static final Logger log = LoggerFactory.getLogger(FcmPushNotificationServiceImpl.class);
    private final UserFcmTokenDao fcmTokenDao;
    /**
     * Sends a push notification to a single device using FCM token
     *
     * @param fcmToken The device FCM token
     * @param title Notification title
     * @param body Notification body
     */
    @Override
    public void sendPushNotification(String fcmToken, String title, String body) {
        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .setImage(AppConstant.KITTYP_PUSH_NOTIFICATION_LOGO)
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Push notification sent successfully: {}", response);

        } catch (FirebaseMessagingException e) {
            log.error("Error sending FCM push notification: {}", e.getMessage(), e);
            fcmTokenDao.deleteByToken(fcmToken);
        }
    }

    @Override
    public void sendNotificationToUser(List<String> fcmTokens, String title, String body) {
        for (String token : fcmTokens) {
        sendPushNotification(token, title, body);
    }
    }
    
}
