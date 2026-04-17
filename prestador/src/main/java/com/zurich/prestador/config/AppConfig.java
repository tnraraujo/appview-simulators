package com.zurich.prestador.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PrestadorSimProperties.class)
public class AppConfig {
}

