package com.iread.backend.student.app.service;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GrowthStageProperties.class)
public class GrowthStageConfig {
}
