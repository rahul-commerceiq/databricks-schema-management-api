package ai.commerceiq.schemamanagement.model.ddl;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CreateFunctionModel extends ChangeSetIdModel {

  String functionDefinition;
}
