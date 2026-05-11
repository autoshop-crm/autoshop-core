package com.vladko.autoshopcore.event.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.events")
public record OrderNotificationEventProperties(
        String source,
        int version,
        boolean orderNotificationsEnabled
) {
}
