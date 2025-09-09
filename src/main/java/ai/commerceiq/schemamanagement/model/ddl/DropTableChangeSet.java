package ai.commerceiq.schemamanagement.model.ddl;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DropTableChangeSet extends DdlStatementModel {

  List<DropTableModel> changeSets;
}
