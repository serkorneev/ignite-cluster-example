package com.griddynamics.dev.igniteclusterexample.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource("classpath:ignite-config.xml")
public class ApplicationConfig {
}
