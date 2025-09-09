package ai.commerceiq.schemamanagement.config;


import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;


//@Configuration
//@EnableSwagger2
public class SwaggerConfig {

  private static final String applicationVersion = "1.0.0";

  private final String title = "databricks-schema-management-api";

  private final String description = "";

  @Bean
  public Docket api() {
    return new Docket(DocumentationType.SWAGGER_2).apiInfo(getApiInfo()).select()
        .apis(RequestHandlerSelectors.withClassAnnotation(RestController.class))
        .paths(PathSelectors.any())
        .build();
  }

  /**
   * @return ApiInfo with application & team details
   */
  private ApiInfo getApiInfo() {
    return new ApiInfoBuilder().title(title)
        .description(description).version(applicationVersion)
        .build();
  }
}
