package ai.commerceiq.schemamanagement.exception;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class InValidFileNameException extends RuntimeException {

  List<String> inValidFileNamesList;

  public InValidFileNameException(List<String> fileNames) {
    super("Invalid file names");
    this.inValidFileNamesList = fileNames;
  }
}
