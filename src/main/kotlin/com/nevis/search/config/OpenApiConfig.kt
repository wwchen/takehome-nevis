package com.nevis.search.config

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import io.swagger.v3.core.jackson.ModelResolver
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig(private val props: AppProperties) {

    /**
     * swagger-core introspects models with its own (Jackson 2) mapper, so it must be told about the
     * snake_case naming the API itself uses (`spring.jackson.property-naming-strategy`).
     */
    @Bean
    fun modelResolver(): ModelResolver =
        ModelResolver(Json.mapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE))

    @Bean
    fun openApi(): OpenAPI {
        val api = OpenAPI().info(
            Info()
                .title("Nevis Search API")
                .version("1.0.0")
                .description(
                    "Clients, their documents, and a unified search endpoint. " +
                        "Client search is lexical (name, email, description); document search is semantic " +
                        "(embeddings + cosine similarity), so 'address proof' also finds 'utility bill'.",
                ),
        )
        if (props.security.apiKey.isNotBlank()) {
            api.components(
                Components().addSecuritySchemes(
                    "ApiKey",
                    SecurityScheme().type(SecurityScheme.Type.APIKEY).`in`(SecurityScheme.In.HEADER).name(ApiKeyFilter.HEADER),
                ),
            ).addSecurityItem(SecurityRequirement().addList("ApiKey"))
        }
        return api
    }
}
