package ai.commerceiq.schemamanagement.model.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class OldFilesDeletedAndUpdatedResponse {

  private String message;
  private List<String> deletedFiles;
  private List<String> updatedFiles;
}
