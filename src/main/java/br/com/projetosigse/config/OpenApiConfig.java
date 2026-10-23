package br.com.projetosigse.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
//@OpenAPIDefinition(
//        info = @Info(
//                title = "SICS - Sistema de Identificacao e Chamada Silenciosa",
//                version = "1.0.0",
//                description = "API REST para cadastro escolar, leituras RFID e chamadas no patio."
//        )
//)
public class OpenApiConfig {

        @Bean
        public OpenAPI customOpenAPI() {
            return new OpenAPI()
                    .info(new io.swagger.v3.oas.models.info.Info()
                            .title("SICS - Sistema de Identificacao e Chamada Silenciosa")
                            .version("1.0.0")
                            .description("API REST para cadastro escolar, leituras RFID e chamadas no patio."));
        }
}
