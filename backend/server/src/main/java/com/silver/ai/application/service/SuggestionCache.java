package com.silver.ai.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 推荐问题进程内缓存。
 * key = conversationId，value 携带版本号（= 当前会话消息条数），
 * 消息增长即自动失效。
 */
@Component
@Slf4j
public class SuggestionCache {

    private static final String KEY_PREFIX = "chat:suggestions:";
    private static final Duration TTL = Duration.ofHours(24);

    private final ConcurrentHashMap<Long, Entry> store = new ConcurrentHashMap<>();
    private RedissonClient redissonClient;
    private ObjectMapper objectMapper = new ObjectMapper();

    public SuggestionCache() {
    }

    @Autowired
    public SuggestionCache(RedissonClient redissonClient, ObjectMapper objectMapper) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }

    public Optional<List<String>> get(Long conversationId, int version) {
        Optional<List<String>> redisValue = getFromRedis(conversationId, version);
        if (redisValue.isPresent()) {
            store.put(conversationId, new Entry(version, redisValue.get()));
            return redisValue;
        }

        Entry e = store.get(conversationId);
        if (e == null || e.version != version) {
            return Optional.empty();
        }
        return Optional.of(e.suggestions);
    }

    public void put(Long conversationId, int version, List<String> suggestions) {
        List<String> copied = List.copyOf(suggestions);
        store.put(conversationId, new Entry(version, copied));
        putToRedis(conversationId, version, copied);
    }

    public void evict(Long conversationId) {
        store.remove(conversationId);
        if (redissonClient != null) {
            try {
                redissonClient.getKeys().deleteByPattern(redisPattern(conversationId));
            } catch (Exception e) {
                log.debug("Evict redis suggestion cache failed for conversation {}: {}", conversationId, e.getMessage());
            }
        }
    }

    private Optional<List<String>> getFromRedis(Long conversationId, int version) {
        if (redissonClient == null) {
            return Optional.empty();
        }

        try {
            Object raw = redissonClient.getBucket(redisKey(conversationId, version)).get();
            if (!(raw instanceof String json) || json.isBlank()) {
                return Optional.empty();
            }
            List<String> list = objectMapper.readValue(json, new TypeReference<>() {});
            if (list == null || list.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(List.copyOf(list));
        } catch (Exception e) {
            log.debug("Read redis suggestion cache failed for conversation {}: {}", conversationId, e.getMessage());
            return Optional.empty();
        }
    }

    private void putToRedis(Long conversationId, int version, List<String> suggestions) {
        if (redissonClient == null || suggestions.isEmpty()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(suggestions);
            redissonClient.getBucket(redisKey(conversationId, version)).set(json, TTL);
        } catch (Exception e) {
            log.debug("Write redis suggestion cache failed for conversation {}: {}", conversationId, e.getMessage());
        }
    }

    private String redisKey(Long conversationId, int version) {
        return KEY_PREFIX + conversationId + ":" + version;
    }

    private String redisPattern(Long conversationId) {
        return KEY_PREFIX + conversationId + ":*";
    }

    private record Entry(int version, List<String> suggestions) {}
}
