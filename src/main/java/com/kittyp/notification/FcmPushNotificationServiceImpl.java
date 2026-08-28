package com.kittyp.notification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class FcmPushNotificationServiceImpl implements FcmPushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(FcmPushNotificationServiceImpl.class);
    private final UserFcmTokenDao fcmTokenDao;

    /**
     * Sends a push notification to a single device using FCM token
     *
     * @param fcmToken The device FCM token
     * @param title    Notification title
     * @param body     Notification body
     */
    @Override
    public void sendPushNotification(String fcmToken, String title, String body) {
        try {

            Map<String, String> data = new HashMap<>();
            data.put("url", "/offers"); // This will be used by service worker for navigation
            data.put("click_action", "FLUTTER_NOTIFICATION_CLICK"); // optional, keeps compatibility
            data.put("icon", AppConstant.KITTYP_PUSH_NOTIFICATION_LOGO);

            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .setImage(AppConstant.KITTYP_PUSH_NOTIFICATION_LOGO)
                            .build())
                    .putAllData(data)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Push notification sent successfully: {}", response);

        } catch (FirebaseMessagingException e) {
            if ("registration-token-not-registered".equals(e.getErrorCode())) {
                log.warn("Invalid FCM token, removing: {}", fcmToken);
                fcmTokenDao.deleteByToken(fcmToken);
            } else {
                log.error("Error sending FCM push notification: {}", e.getMessage(), e);
            }
            // Prevent 500 by not rethrowing
        } catch (Exception e) {
            log.error("Unexpected error while sending FCM push notification: {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendNotificationToUser(List<String> fcmTokens, String title, String body) {
        for (String token : fcmTokens) {
            sendPushNotification(token, title, body);
        }
    }

}
