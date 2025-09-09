package ai.commerceiq.schemamanagement.exception;

import ai.commerceiq.schemamanagement.model.request.MigrationReqQueryParmsAndBody;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MigrationException extends RuntimeException {

  String userName;
  String jobUrl;
  String message;

  public MigrationException() {
  }

  public MigrationException(MigrationReqQueryParmsAndBody migrateRequest, String message) {

    super(message);
    this.message = message;
    this.userName = migrateRequest.getUserName();
    this.jobUrl = migrateRequest.getJobUrl();
  }

}
