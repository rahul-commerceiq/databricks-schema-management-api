package ai.commerceiq.schemamanagement.model.general;

import java.nio.file.Path;
import lombok.Data;

@Data
public class FilePathAndChecksumEntity {

  private Path filePath;
  private String checksum;
  private String fileName;

  public FilePathAndChecksumEntity(Path filePath, String checksum) {
    this.filePath = filePath;
    this.checksum = checksum;
    this.fileName = filePath.getFileName().toString();
  }
}
