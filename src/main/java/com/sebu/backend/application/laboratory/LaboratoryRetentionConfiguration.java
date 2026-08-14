package com.sebu.backend.application.laboratory;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(LaboratoryRetentionProperties.class)
public class LaboratoryRetentionConfiguration {
}
