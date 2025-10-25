package com.kittyp.notification;

import java.util.List;

public interface FcmPushNotificationService {
    
    void sendPushNotification(String fcmToken, String title, String body);

    void sendNotificationToUser(List<String> fcmTokens, String title, String body);
}
