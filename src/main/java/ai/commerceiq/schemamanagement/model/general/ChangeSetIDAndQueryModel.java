package ai.commerceiq.schemamanagement.model.general;


import lombok.Data;

@Data
public class ChangeSetIDAndQueryModel {

  int id;
  String query;
  String error;

  public ChangeSetIDAndQueryModel(int id, String query, String error) {
    this.id = id;
    this.query = query;
    this.error = error.substring(0, Math.min(error.length(), 100));
  }
}
