package com.vladko.autoshopcore.integration.umapi.support;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UmapiCatalogCacheKeyFactory {

    public String manufacturersKey(String languageCode, String regionCode, String type, boolean popular) {
        return "umapi:catalog:manufacturers:%s:%s:%s:%s"
                .formatted(languageCode, regionCode, type, popular);
    }

    public String modelSeriesKey(String languageCode, String regionCode, String type, Integer manufacturerId) {
        return "umapi:catalog:model-series:%s:%s:%s:%s"
                .formatted(languageCode, regionCode, type, manufacturerId);
    }

    public String modificationsKey(String languageCode, String regionCode, String type, Integer modelSeriesId) {
        return "umapi:catalog:modifications:%s:%s:%s:%s"
                .formatted(languageCode, regionCode, type, modelSeriesId);
    }

    public String fuseKey(String languageCode, String regionCode, String type, Integer modificationId) {
        return "umapi:catalog:fuse:%s:%s:%s:%s"
                .formatted(languageCode, regionCode, type, modificationId);
    }

    public String productGroupSearchKey(String languageCode,
                                        String regionCode,
                                        String type,
                                        Integer modificationId,
                                        String normalizedQuery) {
        return "umapi:catalog:product-groups-search:%s:%s:%s:%s:%s"
                .formatted(languageCode, regionCode, type, modificationId, sha256(normalizedQuery));
    }

    public String articlesKey(String languageCode,
                              String regionCode,
                              String type,
                              Integer modificationId,
                              List<Integer> productGroupIds,
                              Integer supplierId,
                              int limit,
                              int offset) {
        String groupIds = productGroupIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String supplierKey = supplierId == null ? "ANY" : String.valueOf(supplierId);
        return "umapi:catalog:articles:%s:%s:%s:%s:%s:%s:%s:%s"
                .formatted(languageCode, regionCode, type, modificationId, groupIds, supplierKey, limit, offset);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : bytes) {
                builder.append("%02x".formatted(item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }
}
