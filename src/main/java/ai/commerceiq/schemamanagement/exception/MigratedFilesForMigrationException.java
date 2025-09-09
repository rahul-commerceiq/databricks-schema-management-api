package ai.commerceiq.schemamanagement.exception;

import ai.commerceiq.schemamanagement.model.request.MigrationReqQueryParmsAndBody;
import ai.commerceiq.schemamanagement.utils.StringConstants;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MigratedFilesForMigrationException extends MigrationException {

  List<String> fileList;

  public MigratedFilesForMigrationException(List<String> filesList,
      MigrationReqQueryParmsAndBody migrateRequest) {
    super.setMessage(StringConstants.MIGRATED_FILES_FOR_MIGRATION);
    super.setJobUrl(migrateRequest.getJobUrl());
    super.setUserName(migrateRequest.getUserName());
    this.fileList = filesList;

  }
}
