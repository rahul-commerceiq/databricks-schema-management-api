package ai.commerceiq.schemamanagement.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeleteTablesAndS3Response {

  String tableName;
  String status;
  String error;

  public DeleteTablesAndS3Response(String tableName) {
    this.tableName = tableName;
  }


}
