package com.stemcloud.liye.dc.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Belongs to data-camera-web
 * Description:
 *  base operation of redis
 * @author liye on 2017/12/11
 */
@Component
public class RedisUtils {
    @SuppressWarnings("rawtypes")
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 批量删除对应的value
     *
     * @param keys
     */
    public void remove(final String... keys) {
        for (String key : keys) {
            remove(key);
        }
    }

    /**
     * 批量删除key
     *
     * @param pattern
     */
    @SuppressWarnings("unchecked")
    public void removePattern(final String pattern) {
        Set<Serializable> keys = redisTemplate.keys(pattern);
        if (keys.size() > 0) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * 删除对应的value
     *
     * @param key
     */
    @SuppressWarnings("unchecked")
    public void remove(final String key) {
        if (exists(key)) {
            redisTemplate.delete(key);
        }
    }

    /**
     * 判断缓存中是否有对应的value
     *
     * @param key
     * @return
     */
    @SuppressWarnings("unchecked")
    public boolean exists(final String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 读取缓存
     *
     * @param key
     * @return
     */
    @SuppressWarnings("unchecked")
    public Object get(final String key) {
        Object result = null;
        ValueOperations<Serializable, Object> operations = redisTemplate.opsForValue();
        result = operations.get(key);
        return result;
    }

    /**
     * 写入缓存
     *
     * @param key
     * @param value
     * @return
     */
    @SuppressWarnings("unchecked")
    public boolean set(final String key, Object value) {
        boolean result = false;
        try {
            ValueOperations<Serializable, Object> operations = redisTemplate.opsForValue();
            operations.set(key, value);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 写入缓存
     *
     * @param key
     * @param value
     * @return
     */
    @SuppressWarnings("unchecked")
    public boolean set(final String key, Object value, Long expireTime) {
        boolean result = false;
        try {
            ValueOperations<Serializable, Object> operations = redisTemplate.opsForValue();
            operations.set(key, value);
            redisTemplate.expire(key, expireTime, TimeUnit.SECONDS);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 列表添加
     *
     * @param k
     * @param v
     */
    @SuppressWarnings("unchecked")
    public boolean lPush(String k, Object v){
        boolean result = false;
        try {
            ListOperations<String, Object> list = redisTemplate.opsForList();
            list.rightPush(k,v);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 列表获取
     *
     * @param k
     * @return
     */
    @SuppressWarnings("unchecked")
    public Object lPop(String k){
        boolean result = false;
        try {
            ListOperations<String, Object> list = redisTemplate.opsForList();
            list.leftPop(k);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public boolean hashSet(String key, String hashKey, Object hashValue){
        boolean result = false;
        try {
            HashOperations<String, String, Object> hash = redisTemplate.opsForHash();
            hash.put(key, hashKey, hashValue);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public Object hashGet(String key, String hashKey){
        try {
            HashOperations<String, String, Object> hash = redisTemplate.opsForHash();
            return hash.get(key, hashKey);
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public boolean hashRemove(String key, String hashKey) {
        boolean result = false;
        try {
            HashOperations<String, String, Object> hash = redisTemplate.opsForHash();
            hash.delete(key, hashKey);
            result = true;
        } catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }
}
