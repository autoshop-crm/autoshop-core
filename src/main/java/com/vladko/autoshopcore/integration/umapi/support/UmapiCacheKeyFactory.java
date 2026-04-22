package com.vladko.autoshopcore.integration.umapi.support;

import org.springframework.stereotype.Component;

@Component
public class UmapiCacheKeyFactory {

    public String searchKey(String languageCode, String regionCode, String articleNumber, String brand, int limit, int offset) {
        String brandKey = brand == null ? "ANY" : brand;
        return "umapi:parts:search:v1:lang=%s:region=%s:mode=analogs:article=%s:brand=%s:limit=%s:offset=%s"
                .formatted(languageCode, regionCode, articleNumber, brandKey, limit, offset);
    }
}
