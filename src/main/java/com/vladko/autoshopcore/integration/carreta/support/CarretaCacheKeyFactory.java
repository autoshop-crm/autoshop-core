package com.vladko.autoshopcore.integration.carreta.support;

import org.springframework.stereotype.Component;

@Component
public class CarretaCacheKeyFactory {

    public String searchKey(String account, String query) {
        String accountKey = account == null || account.isBlank() ? "default" : account.trim();
        return "carreta:quotes:search:v1:account=%s:query=%s".formatted(accountKey, query);
    }
}
