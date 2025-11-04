package com.github.popularityscore.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiSecurityConfig {

    @Bean
    public OpenAPI api() {
        String authUrl  = "http://host.docker.internal:8180/realms/github-popularity/protocol/openid-connect/auth";
        String tokenUrl = "http://host.docker.internal:8180/realms/github-popularity/protocol/openid-connect/token";

        OAuthFlow flow = new OAuthFlow()
                .authorizationUrl(authUrl)
                .tokenUrl(tokenUrl)
                .scopes(new Scopes()
                        .addString("openid", "OpenID scope")
                        .addString("profile", "Profile scope"));

        SecurityScheme scheme = new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .name("keycloak")
                .flows(new OAuthFlows().authorizationCode(flow));

        return new OpenAPI()
                .info(new Info().title("GitHub Popularity API").version("v1"))
                .schemaRequirement("keycloak", scheme)
                .addSecurityItem(new SecurityRequirement().addList("keycloak", java.util.List.of("openid","profile")));
    }
}