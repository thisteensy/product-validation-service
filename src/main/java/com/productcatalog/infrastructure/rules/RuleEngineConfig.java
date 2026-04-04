package com.productcatalog.infrastructure.rules;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class RuleEngineConfig {

    @Bean
    public List<ValidationRules> universalRules() {
        return List.of(new UniversalRules());
    }

    @Bean
    public Map<String, ValidationRules> dspRules() {
        return Map.of("spotify", new SpotifyRules());
    }
}
