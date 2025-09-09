package ai.commerceiq.schemamanagement.exception;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class OldFilesDeletedAndUpdatedException extends RuntimeException {

  private final List<String> deletedFiles;
  private final List<String> updatedFiles;

  public OldFilesDeletedAndUpdatedException(List<String> deletedFiles, List<String> updatedFiles) {
    super("Some files are deleted and some are updated.");
    this.deletedFiles = deletedFiles;
    this.updatedFiles = updatedFiles;
  }
}
