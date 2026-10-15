package com.connectlink.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 문서 설정. 프론트 타입(schema.d.ts)의 원천이므로 "응답 필드는 기본 필수" 규칙을 여기서 건다. 비어 있을 수 있는 필드만
 * {@code @Schema(nullable = true)} 로 표시한다.
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI connectLinkOpenApi() {
    return new OpenAPI()
        .info(new Info().title("연결고리 API").version("v1").description("결제원·기관 담당자 업무 포털"));
  }

  @Bean
  public OpenApiCustomiser requiredByDefaultCustomiser() {
    return openApi -> {
      if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
        return;
      }
      for (Schema<?> schema : openApi.getComponents().getSchemas().values()) {
        Map<String, Schema> properties = schema.getProperties();
        if (properties == null) {
          continue;
        }
        List<String> required = new ArrayList<>();
        properties.forEach(
            (name, property) -> {
              if (!Boolean.TRUE.equals(property.getNullable())) {
                required.add(name);
              }
            });
        schema.setRequired(required);
      }
    };
  }
}
