package com.nequi.ticketing.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI ticketingOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Ticketing Platform API")
                .version("v1")
                .description("API for managing events, ticket availability, and asynchronous ticket orders."));
    }
}
