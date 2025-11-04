package com.prolizwebservices.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

/**
 * OpenAPI (Swagger) configuration for ProlizWebServices
 * Provides interactive API documentation and testing interface
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:/ProlizWebServices}")
    private String contextPath;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ProlizWebServices API")
                        .description("SOAP to REST adapter for Gaziantep University Student Information System\n\n" +
                                "This service provides a RESTful interface to access student information, " +
                                "academic data, and administrative functions from the university's SOAP-based backend system.\n\n" +
                                "**Key Features:**\n" +
                                "- Student authentication and information retrieval\n" +
                                "- Academic staff authentication\n" +
                                "- Course and program information\n" +
                                "- Distance education data\n" +
                                "- Academic schedules and transcripts\n\n" +
                                "**Authentication:** Most endpoints require valid credentials through the authentication endpoints.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ProlizWebServices Development Team")
                                .email("proliz@gantep.edu.tr")
                                .url("https://github.com/gaziantep-university/proliz-web-services"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8083" + contextPath)
                                .description("Local Development (Embedded Tomcat)"),
                        new Server()
                                .url("http://localhost:8080" + contextPath)
                                .description("Local Development (External Tomcat)"),
                        new Server()
                                .url("http://193.140.136.26:8084" + contextPath)
                                .description("Production Server (HTTP - Port 8084)"),
                        new Server()
                                .url("https://193.140.136.26:8443" + contextPath)
                                .description("Production Server (HTTPS)"),
                        new Server()
                                .url("https://proliz.gantep.edu.tr" + contextPath)
                                .description("University Server (HTTPS)")
                ));
    }
}