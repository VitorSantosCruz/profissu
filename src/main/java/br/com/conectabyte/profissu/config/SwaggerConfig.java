package br.com.conectabyte.profissu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {
  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("PROFISU API")
            .description("API para conectar profissionais e clientes.")
            .version("0.0.1")
            .license(new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT")))
        .externalDocs(new ExternalDocumentation()
            .description("Repositório no GitHub")
            .url("https://github.com/VitorSantosCruz/profissu"))
        .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
        .components(new io.swagger.v3.oas.models.Components()
            .addSecuritySchemes("BearerAuth",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
  }
}
