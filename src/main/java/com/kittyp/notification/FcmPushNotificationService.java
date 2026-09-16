package com.kittyp.notification;

import java.util.List;
import java.util.Map;

public interface FcmPushNotificationService {

    void sendPushNotification(String fcmToken, String title, String body);

    void sendPushNotification(String fcmToken, String title, String body, Map<String, String> extraData);

    void sendNotificationToUser(List<String> fcmTokens, String title, String body);

    void sendNotificationToUser(List<String> fcmTokens, String title, String body, Map<String, String> extraData);
}
