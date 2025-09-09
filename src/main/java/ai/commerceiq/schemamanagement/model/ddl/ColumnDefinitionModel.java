package ai.commerceiq.schemamanagement.model.ddl;

import lombok.Data;

@Data
public class ColumnDefinitionModel {

  private String name;
  private String type;
  private boolean addNotNullConstraint;
  private String comment;
  private String newName;
  private boolean autoIncrement;
  private boolean enableCustomAutoIncrement;
  private boolean partitionColumn;
  private int autoIncrementStartWith;
  private int autoIncrementBy;
  private String defaultValue;
}