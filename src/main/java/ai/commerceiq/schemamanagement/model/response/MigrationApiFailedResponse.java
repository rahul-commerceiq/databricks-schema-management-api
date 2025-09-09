package ai.commerceiq.schemamanagement.model.response;

import ai.commerceiq.schemamanagement.exception.MigrationFailedException;
import ai.commerceiq.schemamanagement.utils.StringConstants;
import java.util.List;
import lombok.Data;

@Data
public class MigrationApiFailedResponse {

  private List<FailedMigrationVersionDetails> migratedVersionList;
  private String status = StringConstants.FAILED;

  public MigrationApiFailedResponse(MigrationFailedException migrationFailedException) {
    this.migratedVersionList = migrationFailedException.getFailedMigrationDetails();
  }
}
