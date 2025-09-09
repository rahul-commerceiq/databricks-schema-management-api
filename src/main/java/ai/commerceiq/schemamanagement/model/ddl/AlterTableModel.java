package ai.commerceiq.schemamanagement.model.ddl;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AlterTableModel extends ChangeSetIdModel {

  String alterationType;
  ColumnDefinitionModel column;
  String tableName;
}
