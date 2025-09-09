package ai.commerceiq.schemamanagement.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

@Getter
@Service
@NoArgsConstructor
@AllArgsConstructor
public class ChangeSetValidationResultModel {

  private String filePath;
  private String fileName;
  private String Error;
}
