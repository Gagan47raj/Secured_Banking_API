
package com.algotutor.securebanking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        // Define JWT security scheme
        SecurityScheme jwtSecurityScheme = new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .name("Authorization")
            .description("JWT Authorization header using the Bearer scheme. Example: 'Bearer {token}'");

        // Define security requirement
        SecurityRequirement jwtSecurityRequirement = new SecurityRequirement()
            .addList("bearerAuth");

        // Create server information
        Server localServer = new Server()
            .url("http://localhost:8080")
            .description("Local Development Server");

        // Create contact information
        Contact contact = new Contact()
            .name("Banking API Team")
            .email("support@banking.com")
            .url("https://banking.com");

        // Create API information
        Info apiInfo = new Info()
            .title("Secure Banking API")
            .version("1.0.0")
            .description("A comprehensive banking API with JWT authentication, role-based authorization, and complete banking operations including deposits, withdrawals, and transfers.")
            .contact(contact)
            .license(new License()
                .name("MIT")
                .url("https://opensource.org/licenses/MIT"));

        return new OpenAPI()
            .info(apiInfo)
            .servers(List.of(localServer))
            .addSecurityItem(jwtSecurityRequirement)
            .components(new Components()
                .addSecuritySchemes("bearerAuth", jwtSecurityScheme));
    }
}
