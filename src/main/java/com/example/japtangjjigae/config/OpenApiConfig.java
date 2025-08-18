package com.example.japtangjjigae.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("잡탕찌개 API 목록")
                .description("잡탕찌개 서비스의 API 목록들입니다.")
                .version("v.1.0.0"))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("개발용 서버")
            ));
    }

    @Bean
    public GroupedOpenApi groupedOpenApiV1() {
        return GroupedOpenApi.builder()
            .group("v1")
            .pathsToMatch("/api/v1/**")
            .build();
    }

//    @Bean
//    public GroupedOpenApi groupedOpenApiV2() {
//        return GroupedOpenApi.builder()
//            .group("v2")
//            .pathsToMatch("/api/v2/**")
//            .build();
//    }

}
