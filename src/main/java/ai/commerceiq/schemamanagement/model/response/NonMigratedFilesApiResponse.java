package ai.commerceiq.schemamanagement.model.response;

import ai.commerceiq.schemamanagement.utils.StringConstants;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class NonMigratedFilesApiResponse {

  private String status;
  private List<String> filePaths;
  private LocalDateTime timestamp;

  public NonMigratedFilesApiResponse(List<String> filesName) {
    this.filePaths = filesName;
    this.status = StringConstants.SUCCESS;
    this.timestamp = LocalDateTime.now();
  }
}
