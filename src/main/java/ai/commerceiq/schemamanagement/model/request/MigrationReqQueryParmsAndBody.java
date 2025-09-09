package ai.commerceiq.schemamanagement.model.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MigrationReqQueryParmsAndBody extends MigrateRequestModel {

  String userName;
  String jobUrl;

  public MigrationReqQueryParmsAndBody(MigrateRequestModel migrateReqBody, String userName,
      String jobUrl) {
    this.jobUrl = jobUrl;
    this.userName = userName;
    super.files = migrateReqBody.getFiles();
  }
}
