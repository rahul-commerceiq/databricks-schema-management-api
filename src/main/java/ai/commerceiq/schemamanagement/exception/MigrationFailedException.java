package ai.commerceiq.schemamanagement.exception;


import ai.commerceiq.schemamanagement.model.request.MigrationReqQueryParmsAndBody;
import ai.commerceiq.schemamanagement.model.response.FailedMigrationVersionDetails;
import ai.commerceiq.schemamanagement.utils.StringConstants;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MigrationFailedException extends MigrationException {

  List<FailedMigrationVersionDetails> failedMigrationDetails;

  public MigrationFailedException(List<FailedMigrationVersionDetails> failedMigrationDetails
      , MigrationReqQueryParmsAndBody migrateRequest) {
    super.setMessage(StringConstants.MIGRATION_FAILED_MESSAGE);
    super.setJobUrl(migrateRequest.getJobUrl());
    super.setUserName(migrateRequest.getUserName());
    this.failedMigrationDetails = failedMigrationDetails;
  }
}
