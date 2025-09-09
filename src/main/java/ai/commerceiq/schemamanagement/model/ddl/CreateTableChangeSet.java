package ai.commerceiq.schemamanagement.model.ddl;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CreateTableChangeSet extends DdlStatementModel {

  List<CreateTableModel> changeSets;
}
