package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@Configuration
// @EnableCaching // Temporarily disabled to fix LinkedHashMap casting issue
public class RedisConfig {

        private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

        @Value("${spring.data.redis.host:localhost}")
        private String redisHost;

        @Value("${spring.data.redis.port:6379}")
        private int redisPort;

        @Bean
        public RedisConnectionFactory redisConnectionFactory() {
                log.info("Creating Redis connection factory for {}:{}", redisHost, redisPort);
                return new LettuceConnectionFactory(redisHost, redisPort);
        }

        @Bean
        public ObjectMapper redisObjectMapper() {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                return mapper;
        }

        @Bean
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
                log.info("Creating Redis template with JSON serialization");

                RedisTemplate<String, Object> template = new RedisTemplate<>();
                template.setConnectionFactory(connectionFactory);

                // Use String serialization for keys
                template.setKeySerializer(new StringRedisSerializer());
                template.setHashKeySerializer(new StringRedisSerializer());

                // Use JSON serialization for values with custom ObjectMapper
                GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(
                                redisObjectMapper());
                template.setValueSerializer(jsonSerializer);
                template.setHashValueSerializer(jsonSerializer);

                template.setEnableTransactionSupport(true);
                template.afterPropertiesSet();

                log.debug("Redis template configured successfully");
                return template;
        }

        @Bean
        public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
                log.info("Creating Redis cache manager with TTL configurations");

                GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(
                                redisObjectMapper());

                RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofHours(1))
                                .serializeKeysWith(
                                                org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(
                                                org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                                                                .fromSerializer(jsonSerializer));

                RedisCacheConfiguration usersCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(30))
                                .serializeKeysWith(
                                                org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(
                                                org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                                                                .fromSerializer(jsonSerializer));

                RedisCacheConfiguration tokenValidationCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(15))
                                .serializeKeysWith(
                                                org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(
                                                org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                                                                .fromSerializer(jsonSerializer));

                log.debug("Cache configurations: users=30min, tokenValidation=15min, default=1hour");

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(defaultCacheConfig)
                                .withCacheConfiguration("users", usersCacheConfig)
                                .withCacheConfiguration("userDetails", usersCacheConfig)
                                .withCacheConfiguration("tokenValidation", tokenValidationCacheConfig)
                                .build();
        }
}