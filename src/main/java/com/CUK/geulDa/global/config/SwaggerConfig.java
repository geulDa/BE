package com.CUK.geulDa.global.config;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI GeulDa_API() {
        Info info = new Info()
                .title("글다_API")
                .version("1.0")
                .description("GeulDa API입니다");

//        String jwtSchemeName = "JWT_t0ken";
//        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);

//  jwt 스킴용
//        Components components = new Components()
//                .addSecuritySchemes(jwtSchemeName,new SecurityScheme()
//                        .name(jwtSchemeName)
//                        .type(SecurityScheme.Type.HTTP)
//                        .scheme("bearer")
//                        .bearerFormat("JWT"));

        //Swagger UI 설정 및 보안 추가
        return new OpenAPI()
                .addServersItem(new Server().url("/"))
                .components(new Components())
                .info(info);
//              .addSecurityItem(securityRequirement);
    }

}