package ai.commerceiq.schemamanagement.model.ddl;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CreateTableModel extends ChangeSetIdModel {

  private String tableName;
  private TableMetadataModel tableMetaData;
  private List<ColumnDefinitionModel> columns;

}



