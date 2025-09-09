package ai.commerceiq.schemamanagement.model.ddl;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DropTableModel extends ChangeSetIdModel {

  private String tableName;
  private boolean forceDrop;
}
