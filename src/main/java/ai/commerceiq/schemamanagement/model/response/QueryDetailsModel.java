package ai.commerceiq.schemamanagement.model.response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class QueryDetailsModel {

  String query;
  int id;
  String result;
  String error;

  public QueryDetailsModel(String query, int id) {
    this.query = query;
    this.id = id;
  }

  public QueryDetailsModel(int id) {
    this.id = id;
  }

}
