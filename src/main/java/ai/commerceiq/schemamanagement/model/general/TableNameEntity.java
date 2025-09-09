package ai.commerceiq.schemamanagement.model.general;

import ai.commerceiq.schemamanagement.utils.StringConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TableNameEntity {

  String catalog;
  String schema;
  String tableName;
  String fullTableName;

  public TableNameEntity(String entireTableName) {
    if (entireTableName == null || entireTableName.isEmpty()) {
      throw new IllegalArgumentException("Input entireTableName cannot be null or empty");
    }
    String[] components = entireTableName.split("\\.", 3);
    if (components.length != 3) {
      throw new IllegalArgumentException(
          StringConstants.INVALID_TABLE_NAME_ERROR);
    }
    this.catalog = components[0];
    this.schema = components[1];
    this.tableName = components[2];
    this.fullTableName = entireTableName;
  }
}
