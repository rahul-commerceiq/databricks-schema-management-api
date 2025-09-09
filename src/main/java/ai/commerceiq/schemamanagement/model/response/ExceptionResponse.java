package ai.commerceiq.schemamanagement.model.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExceptionResponse {

  private String message;
  private List<String> details;
}
