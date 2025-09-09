package ai.commerceiq.schemamanagement.model.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;


@Data
public class MigrationApiSuccessfulResponse {

  private String status;
  private List<Object> details;
  private LocalDateTime timestamp;
  private String userName;
  private String jobUrl;
}
