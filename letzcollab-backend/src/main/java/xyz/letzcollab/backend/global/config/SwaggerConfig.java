package xyz.letzcollab.backend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI openAPI() {
		String jwtSchemeName = "jwtAuth";
		String cookieSchemeName = "cookieAuth";

		SecurityRequirement jwtRequirement = new SecurityRequirement().addList(jwtSchemeName);
		SecurityRequirement cookieRequirement = new SecurityRequirement().addList(cookieSchemeName);

		Components components = new Components()
				// 1. Bearer Token Auth (모바일 앱 또는 외부 서버용)
				.addSecuritySchemes(jwtSchemeName, new SecurityScheme()
						.name(jwtSchemeName)
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT"))

				// 2. Cookie Auth (웹 브라우저 - React 프론트엔드용)
				.addSecuritySchemes(cookieSchemeName, new SecurityScheme()
						.name("accessToken") // 실제 필터에서 찾는 쿠키 이름
						.type(SecurityScheme.Type.APIKEY)
						.in(SecurityScheme.In.COOKIE));

		return new OpenAPI()
				.info(apiInfo())
				.addSecurityItem(jwtRequirement)
				.addSecurityItem(cookieRequirement)
				.components(components);
	}

	private Info apiInfo() {
		return new Info()
				.title("LetzCollab API 명세서")
				.description("프로젝트 협업 관리 플랫폼 LetzCollab 백엔드 API 문서입니다.<br>" +
						"<b>웹 프론트엔드(React)</b>는 로그인 시 `X-Client-Type: web` 헤더를 전송하여 HttpOnly 쿠키로 인증하고, " +
						"<b>모바일 앱</b>은 발급받은 JWT를 Authorization 헤더(Bearer)에 담아 요청해주세요.")
				.version("1.0.0");
	}
}
