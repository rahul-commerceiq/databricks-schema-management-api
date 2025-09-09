package ai.commerceiq.schemamanagement.model.ddl;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CreateSchemaChangeSet extends DdlStatementModel {

  List<CreateSchemaModel> changeSets;
}
