package com.CUK.geulDa.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String JWT_SCHEME_NAME = "JWT_Bearer_Auth";

    @Bean
    public OpenAPI GeulDa_API() {
        Info info = new Info().title("글다 API").version("1.0.0").description(
            "GeulDa(글다) REST API 문서입니다.\n\n" + "## 인증 방법\n"
                + "1. `/api/auth/temp-token/exchange` 엔드포인트로 임시 토큰을 JWT로 교환\n"
                + "2. 응답받은 `accessToken` 복사\n" + "3. 우측 상단 [Authorize] 버튼 클릭\n"
                + "4. 토큰 입력 후 [Authorize] 버튼 클릭\n" + "5. 이후 모든 API 요청에 자동으로 토큰이 포함됩니다.");

        SecurityScheme securityScheme = new SecurityScheme().type(SecurityScheme.Type.HTTP)
            .scheme("bearer").bearerFormat("JWT").in(SecurityScheme.In.HEADER).name("Authorization")
            .description("JWT 액세스 토큰을 입력하세요. 'Bearer ' 접두사는 자동으로 추가됩니다.");

        SecurityRequirement securityRequirement = new SecurityRequirement().addList(
            JWT_SCHEME_NAME);

        return new OpenAPI().addServersItem(new Server().url("/").description("현재 서버"))
            .components(new Components().addSecuritySchemes(JWT_SCHEME_NAME, securityScheme))
            .info(info).addSecurityItem(securityRequirement);
    }
}
