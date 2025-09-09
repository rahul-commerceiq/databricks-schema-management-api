package ai.commerceiq.schemamanagement.exception;

import ai.commerceiq.schemamanagement.model.request.MigrationReqQueryParmsAndBody;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class FilesNotMigratedToLowerEnvException extends MigrationException {

  List<String> fileList;

  public FilesNotMigratedToLowerEnvException(List<String> filesList,
      MigrationReqQueryParmsAndBody migrateRequest) {
    super.setMessage(
        "Following files cannot be migrated to QA/PROD before migrating to lower environments");
    super.setJobUrl(migrateRequest.getJobUrl());
    super.setUserName(migrateRequest.getUserName());
    this.fileList = filesList;
  }
}
