package ai.commerceiq.schemamanagement.model.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class ValidateApiResponse {

  private String status;
  private List<FileValidationResultModel> details;
  private LocalDateTime timestamp;
  private String validatedBy;
}
