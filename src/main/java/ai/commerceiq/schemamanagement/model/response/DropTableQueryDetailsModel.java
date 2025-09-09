package ai.commerceiq.schemamanagement.model.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class DropTableQueryDetailsModel extends QueryDetailsModel {

  boolean forceDrop;

  public DropTableQueryDetailsModel(String dropTableQuery, int id, boolean forceDrop) {
    super(dropTableQuery, id);
    this.forceDrop = forceDrop;
  }
}
