/**
 * @description: 银行投产材料管理系统 - 核心业务逻辑实现
 * @author: LLQ (Researcher @ SCU-CEIE)
 * @date: 2026-03
 * @version: 1.0
 * @note: 本项目仅用于个人技术方案复现与脱敏展示，严禁用于其他任何用途。
 */


package com.example.prd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
/*
执行 redisTemplate.opsForValue().set(CACHE_KEY_TREE, tree) 时，
能在 Redis 可视化工具里看到整齐的 JSON 树结构，而不是一串看不懂的乱码。
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 使用 String 序列化 Key
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // 使用 JSON 序列化 Value
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}