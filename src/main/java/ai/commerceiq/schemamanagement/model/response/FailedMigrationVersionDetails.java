package ai.commerceiq.schemamanagement.model.response;

import ai.commerceiq.schemamanagement.model.general.ChangeSetIDAndQueryModel;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;


@AllArgsConstructor
@Data
public class FailedMigrationVersionDetails {

  int version;
  List<ChangeSetIDAndQueryModel> details;


}
