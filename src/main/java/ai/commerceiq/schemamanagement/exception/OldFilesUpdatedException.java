package ai.commerceiq.schemamanagement.exception;


import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class OldFilesUpdatedException extends RuntimeException {

  private final List<String> updatedFiles;

  public OldFilesUpdatedException(List<String> updatedFiles) {
    super("Some old files are updated.");
    this.updatedFiles = updatedFiles;
  }

}
