package com.platform.framework.config;

import com.platform.framework.service.CaffeineOpenService;
import com.platform.framework.service.RedisOpenService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfiguration {

    @Bean
    @ConditionalOnMissingBean(RedisOpenService.class)
    public RedisOpenService redisOpenService() {
        return new RedisOpenService();
    }


    @Bean
    @ConditionalOnMissingBean(CaffeineOpenService.class)
    public CaffeineOpenService caffeineOpenService() {
        return new CaffeineOpenService();
    }
}
