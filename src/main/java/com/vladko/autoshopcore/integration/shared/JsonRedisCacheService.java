package com.vladko.autoshopcore.integration.shared;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JsonRedisCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> Optional<T> get(String key, Class<T> type) {
        String cachedValue = redisTemplate.opsForValue().get(key);
        if (cachedValue == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(cachedValue, type));
        } catch (JsonProcessingException exception) {
            return Optional.empty();
        }
    }

    public <T> Optional<List<T>> getList(String key, Class<T> elementType) {
        String cachedValue = redisTemplate.opsForValue().get(key);
        if (cachedValue == null) {
            return Optional.empty();
        }

        try {
            JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
            return Optional.of(objectMapper.readValue(cachedValue, type));
        } catch (JsonProcessingException exception) {
            return Optional.empty();
        }
    }

    public void put(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException exception) {
            throw new ExternalApiContractException("Could not serialize external API cache value", exception);
        }
    }
}
