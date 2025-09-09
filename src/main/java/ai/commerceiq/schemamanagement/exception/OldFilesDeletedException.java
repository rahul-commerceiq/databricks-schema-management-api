package ai.commerceiq.schemamanagement.exception;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class OldFilesDeletedException extends RuntimeException {

  private final List<String> deletedFiles;

  public OldFilesDeletedException(List<String> deletedFiles) {
    super("Some old files are deleted.");
    this.deletedFiles = deletedFiles;
  }
}
