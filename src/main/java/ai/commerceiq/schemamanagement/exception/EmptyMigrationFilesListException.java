package ai.commerceiq.schemamanagement.exception;

import ai.commerceiq.schemamanagement.model.request.MigrationReqQueryParmsAndBody;

public class EmptyMigrationFilesListException extends MigrationException {

  public EmptyMigrationFilesListException(String message,
      MigrationReqQueryParmsAndBody migrateRequest) {
    super.setMessage(message);
    super.setUserName(migrateRequest.getUserName());
    super.setJobUrl(migrateRequest.getJobUrl());
  }
}
