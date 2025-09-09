package ai.commerceiq.schemamanagement.utils;

import ai.commerceiq.schemamanagement.model.ddl.AlterTableChangeSet;
import ai.commerceiq.schemamanagement.model.ddl.AlterTableModel;
import ai.commerceiq.schemamanagement.model.ddl.ColumnDefinitionModel;
import ai.commerceiq.schemamanagement.model.ddl.CreateFunctionChangeSet;
import ai.commerceiq.schemamanagement.model.ddl.CreateFunctionModel;
import ai.commerceiq.schemamanagement.model.ddl.CreateSchemaChangeSet;
import ai.commerceiq.schemamanagement.model.ddl.CreateSchemaModel;
import ai.commerceiq.schemamanagement.model.ddl.CreateTableChangeSet;
import ai.commerceiq.schemamanagement.model.ddl.CreateTableModel;
import ai.commerceiq.schemamanagement.model.ddl.CreateViewChangeSet;
import ai.commerceiq.schemamanagement.model.ddl.CreateViewModel;
import ai.commerceiq.schemamanagement.model.ddl.DdlStatementModel;
import ai.commerceiq.schemamanagement.model.ddl.DropFunctionChangeSet;
import ai.commerceiq.schemamanagement.model.ddl.DropFunctionModel;
import ai.commerceiq.schemamanagement.model.ddl.DropTableChangeSet;
import ai.commerceiq.schemamanagement.model.ddl.DropTableModel;
import ai.commerceiq.schemamanagement.model.ddl.DropViewChangeSet;
import ai.commerceiq.schemamanagement.model.ddl.DropViewModel;
import ai.commerceiq.schemamanagement.model.response.DropTableQueryDetailsModel;
import ai.commerceiq.schemamanagement.model.response.QueryDetailsModel;
import ai.commerceiq.schemamanagement.utils.StringConstants.Queries;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QueryGeneratorUtils {

  @Autowired
  private DatabricksQueryExecutionUtils databricksQueryExecutionUtil;

  @Value("${databricks.delta.deletedFileRetentionDuration}")
  private String deletedFileRetentionDuration;
  @Value("${databricks.delta.logRetentionDuration}")
  private String logRetentionDuration;

  public List<QueryDetailsModel> generateAndValidateQuery(DdlStatementModel ddlObject) {
    log.info("Inside generateAndValidateQuery");

    List<QueryDetailsModel> queryDetailsModelList;
    String ddlType = ddlObject.getDDLType();

    queryDetailsModelList = switch (ddlType) {
      case StringConstants.CREATE_TABLE_DDL ->
          generateCreateTableQuery((CreateTableChangeSet) ddlObject);
      case StringConstants.DROP_TABLE_DDL -> generateDropTableQuery((DropTableChangeSet) ddlObject);
      case StringConstants.CREATE_VIEW_DDL, StringConstants.UPDATE_VIEW_DDL ->
          generateCreateViewQuery((CreateViewChangeSet) ddlObject);
      case StringConstants.DROP_VIEW_DDL -> generateDropViewQuery((DropViewChangeSet) ddlObject);
      case StringConstants.ALTER_TABLE_DDL ->
          generateAlterTableQuery((AlterTableChangeSet) ddlObject);
      case StringConstants.CREATE_SCHEMA_DDL ->
          generateCreateSchemaQuery((CreateSchemaChangeSet) ddlObject);
      case StringConstants.CREATE_FUNCTION_DDL, StringConstants.UPDATE_FUNCTION_DDL ->
          generateCreateFunctionQuery((CreateFunctionChangeSet) ddlObject);
      case StringConstants.DROP_FUNCTION_DDL ->
          generateDropFunctionQuery((DropFunctionChangeSet) ddlObject);
      default -> null;
    };
    log.info("Generated   queryDetailsModelList:: {}", queryDetailsModelList);
    if (queryDetailsModelList != null) {
      databricksQueryExecutionUtil.processExplainQueries(queryDetailsModelList);
    }
    log.info("Query details modelList:: {}", queryDetailsModelList);
    return queryDetailsModelList;

  }

  /**
   * Generates CREATE TABLE queries based on the provided CreateTableChangeSet.
   *
   * @param createTableChangeSet The CreateTableChangeSet containing information about tables to be
   *                             created.
   * @return List of QueryDetailsModel objects containing the CREATE TABLE queries.
   */
  private List<QueryDetailsModel> generateCreateTableQuery(
      CreateTableChangeSet createTableChangeSet) {
    log.info("Inside generateCreateTableQuery");
    List<QueryDetailsModel> explainQueryList = new ArrayList<>();
    for (CreateTableModel createTableObject : createTableChangeSet.getChangeSets()) {
      List<String> absentRequiredFields = new ArrayList<>();
      String[] tableNamePartitions = createTableObject.getTableName().split("\\.");
      String catalog = tableNamePartitions.length == 3 ? tableNamePartitions[0] : "";
      Map<String, Object> columnsDetailsMap = generateTableColumns(createTableObject.getColumns(),
          absentRequiredFields, catalog);

      //handle failed cases here
      if (!absentRequiredFields.isEmpty()) {
        String errorString = absentRequiredFields.stream()
            .collect(Collectors.joining(StringConstants.COMMA_DELIMITER,
                StringConstants.ABSENT_FIELD_ERROR_PREFIX, StringConstants.EMPTY_STRING));
        explainQueryList.add(failedQueryDetailsModel(errorString, createTableObject.getId()));
        continue;
      }

      if (!(Boolean) columnsDetailsMap.get(StringConstants.AUTOINCREMENT_VALIDATION)) {
        explainQueryList.add(
            failedQueryDetailsModel(StringConstants.AUTOINCREMENT_VALIDATION_FAILED_ERROR,
                createTableObject.getId()));
        continue;
      }
      
      // Extract catalog from table name to check if it's common_catalog
      boolean isCommonCatalog = StringConstants.COMMON_CATALOG.equalsIgnoreCase(catalog);
      
      // Only enforce client_id partition requirement for non-common_catalog tables
      if ((Boolean) columnsDetailsMap.get(StringConstants.HAS_PARTITION_WITH_OUT_CLIENT_ID) && !isCommonCatalog) {
        explainQueryList.add(
            failedQueryDetailsModel(StringConstants.HAS_PARTITION_WITH_OUT_CLIENT_ID_ERROR,
                createTableObject.getId()));
        continue;
      }

      String createTableQuery = Queries.CREATE_EXTERNAL_TABLE_TEMPLATE;
      createTableQuery = createTableQuery.replace(StringConstants.TABLE_NAME_IDENTIFIER,
              createTableObject.getTableName())
          .replace("{columns}", (String) columnsDetailsMap.get(StringConstants.COLUMNS_STRING))
          .replace("{table_owner}", createTableObject.getTableMetaData().getTeam())
          .replace("{deletedFileRetentionDuration}", deletedFileRetentionDuration)
          .replace("{logRetentionDuration}", logRetentionDuration);

      // Handle partition columns based on catalog type and client_id presence
      if ((Boolean) columnsDetailsMap.get(StringConstants.HAS_CLIENT_ID_COL) || 
          (isCommonCatalog && columnsDetailsMap.containsKey(StringConstants.PARTITION_COLS_STRING))) {
        createTableQuery = createTableQuery.replace(StringConstants.PARTITION_IDENTIFIER,
            (String) columnsDetailsMap.get(StringConstants.PARTITION_COLS_STRING));
      } else {
        createTableQuery = createTableQuery.replace(StringConstants.PARTITION_IDENTIFIER,
            StringConstants.EMPTY_STRING_LITERAl);
      }
      QueryDetailsModel queryDetails = new QueryDetailsModel(createTableQuery,
          createTableObject.getId());
      explainQueryList.add(queryDetails);

    }
    return explainQueryList;
  }

  private QueryDetailsModel failedQueryDetailsModel(String errorString, int id) {
    QueryDetailsModel failedQueryDetails = new QueryDetailsModel(null, id);
    failedQueryDetails.setResult(StringConstants.FAILED);
    failedQueryDetails.setError(errorString);
    return failedQueryDetails;
  }

  /**
   * Generates column details for CREATE TABLE queries based on the provided list of
   * ColumnDefinitionModel objects.
   *
   * @param columnDefinitionObjects List of ColumnDefinitionModel objects representing table
   *                                columns.
   * @return Map containing the generated columns string and a boolean indicating the presence of a
   * client ID column.
   */
  private Map<String, Object> generateTableColumns(
      List<ColumnDefinitionModel> columnDefinitionObjects, List<String> absentRequiredFields, String catalog) {
    log.info("Inside generateTableColumns ");

    boolean firstColumn = true;
    boolean hasClientIdColumn = false;
    int autoIncrementColumnCount = 0;

    Map<String, Object> columnsDetailsMap = new HashMap<>();
    columnsDetailsMap.put(StringConstants.AUTOINCREMENT_VALIDATION, true);
    columnsDetailsMap.put(StringConstants.HAS_PARTITION_WITH_OUT_CLIENT_ID, false);
    StringBuilder columnsQueryBuilder = new StringBuilder("( ");
    List<String> partitionColumns = new ArrayList<>();

    for (ColumnDefinitionModel column : columnDefinitionObjects) {

      List<String> absentFields = validateColumnObject(column);
      if (!absentFields.isEmpty()) {
        absentRequiredFields.addAll(absentFields);
        continue;
      }

      if (!firstColumn) {
        columnsQueryBuilder.append(", ");
      } else {
        firstColumn = false;
      }
      columnsQueryBuilder.append(column.getName()).append(" ").append(column.getType());

      if (column.getName().equalsIgnoreCase(StringConstants.CLIENT_ID)) {
        hasClientIdColumn = true;
      }
      if (column.isAddNotNullConstraint()) {
        columnsQueryBuilder.append(" NOT NULL");
      }
      if (column.isPartitionColumn() && !column.getName()
          .equalsIgnoreCase(StringConstants.CLIENT_ID)) {
        partitionColumns.add(column.getName());
      }
      if (!Objects.isNull(column.getDefaultValue())) {
        // If columns is boolean , timestamp or date then no need to add single quotes
        if (column.getType().equalsIgnoreCase(StringConstants.BOOLEAN) || column.getType()
            .equalsIgnoreCase(StringConstants.TIMESTAMP) || column.getType()
            .equalsIgnoreCase(StringConstants.DATE)) {
          columnsQueryBuilder.append(" DEFAULT ").append(column.getDefaultValue());
        } else {
          columnsQueryBuilder.append(" DEFAULT '").append(column.getDefaultValue()).append("'");
        }
      }

      if (column.isAutoIncrement()) {
        autoIncrementColumnCount++;

        if ((!column.getType().equalsIgnoreCase(StringConstants.LONG) && !column.getType()
            .equalsIgnoreCase(StringConstants.BIGINT))
            || autoIncrementColumnCount > 1) {
          columnsDetailsMap.put(StringConstants.AUTOINCREMENT_VALIDATION, false);
        } else {

          if (column.isEnableCustomAutoIncrement()) {
            columnsQueryBuilder.append(StringConstants.CUSTOM_AUTO_INCREMENT
                .replace("{start_with}", String.valueOf(column.getAutoIncrementStartWith()))
                .replace("{increment_by}", String.valueOf(column.getAutoIncrementBy())));
          } else {
            columnsQueryBuilder.append(" GENERATED ALWAYS AS IDENTITY");
          }
        }
      }
      if (column.getComment() != null && !column.getComment().isEmpty()) {
        columnsQueryBuilder.append(" COMMENT '").append(column.getComment()).append("'");
      }
    }

    boolean isCommonCatalog = StringConstants.COMMON_CATALOG.equalsIgnoreCase(catalog);
    
    if (hasClientIdColumn) {//build partition col string
      partitionColumns.add(0, StringConstants.CLIENT_ID);
      String partitonString = partitionColumns.stream()
          .collect(Collectors.joining(StringConstants.COMMA_DELIMITER,
              StringConstants.PARTITION_BY_LITERAL, ")"));
      columnsDetailsMap.put(StringConstants.PARTITION_COLS_STRING, partitonString);
    } else if (!partitionColumns.isEmpty() && !isCommonCatalog) {
      // Only flag as error if it's not a common_catalog table
      columnsDetailsMap.put(StringConstants.HAS_PARTITION_WITH_OUT_CLIENT_ID, true);
    } else if (!partitionColumns.isEmpty() && isCommonCatalog) {
      // For common_catalog tables, allow partition columns without client_id
      String partitonString = partitionColumns.stream()
          .collect(Collectors.joining(StringConstants.COMMA_DELIMITER,
              StringConstants.PARTITION_BY_LITERAL, ")"));
      columnsDetailsMap.put(StringConstants.PARTITION_COLS_STRING, partitonString);
    }

    columnsQueryBuilder.append(" )");
    columnsDetailsMap.put(StringConstants.COLUMNS_STRING, columnsQueryBuilder.toString());
    columnsDetailsMap.put(StringConstants.HAS_CLIENT_ID_COL, hasClientIdColumn);
    return columnsDetailsMap;

  }

  private List<String> validateColumnObject(ColumnDefinitionModel column) {
    List<String> required = StringConstants.REQUIRED_FIELDS_IN_COLUMN_INFO;
    List<String> nonPresentFields = new ArrayList<>();

    if (required.contains(StringConstants.NAME) && (column.getName() == null || column.getName()
        .isEmpty())) {
      nonPresentFields.add(StringConstants.NAME);
    }
    if (required.contains(StringConstants.TYPE) && (column.getType() == null || column.getType()
        .isEmpty())) {
      nonPresentFields.add(StringConstants.TYPE);
    }
    if (required.contains(StringConstants.COMMENT) && (column.getComment() == null
        || column.getComment().isEmpty())) {
      nonPresentFields.add(StringConstants.COMMENT);
    }
    return nonPresentFields;
  }


  /**
   * Generates DROP TABLE queries based on the provided DropTableChangeSet.
   *
   * @param dropTableChangeSet The DropTableChangeSet containing information about tables to be
   *                           dropped.
   * @return List of QueryDetailsModel objects containing the DROP TABLE queries.
   */
  private List<QueryDetailsModel> generateDropTableQuery(DropTableChangeSet dropTableChangeSet) {
    log.info("Inside generateDropTableQuery");
    List<QueryDetailsModel> explainQueryList = new ArrayList<>();
    for (DropTableModel dropTableObject : dropTableChangeSet.getChangeSets()) {
      String dropTableQuery = generateDropTableQuery(dropTableObject.getTableName());
      DropTableQueryDetailsModel queryDetails = new DropTableQueryDetailsModel(dropTableQuery,
          dropTableObject.getId(), dropTableObject.isForceDrop());
      explainQueryList.add(queryDetails);
    }
    return explainQueryList;

  }

  public String generateDropTableQuery(String tableName) {
    String dropTableQuery = Queries.DROP_TABLE_TEMPLATE;
    dropTableQuery = dropTableQuery.replace(StringConstants.TABLE_NAME_IDENTIFIER,
        tableName.toLowerCase());
    return dropTableQuery;
  }

  /**
   * Generates CREATE VIEW queries based on the provided CreateViewChangeSet.
   *
   * @param createViewChangeSet The CreateViewChangeSet containing information about views to be
   *                            created.
   * @return List of QueryDetailsModel objects containing the CREATE VIEW queries.
   */
  private List<QueryDetailsModel> generateCreateViewQuery(CreateViewChangeSet createViewChangeSet) {
    log.info("Inside generateCreateViewQuery");
    List<QueryDetailsModel> explainQueryList = new ArrayList<>();
    for (CreateViewModel createViewModel : createViewChangeSet.getChangeSets()) {
      QueryDetailsModel queryDetails = new QueryDetailsModel(createViewModel.getViewStatement(),
          createViewModel.getId());
      explainQueryList.add(queryDetails);
    }
    return explainQueryList;

  }

  /**
   * Generates DROP VIEW queries based on the provided DropViewChangeSet.
   *
   * @param dropViewChangeSet The DropViewChangeSet containing information about views to be
   *                          dropped.
   * @return List of QueryDetailsModel objects containing the DROP VIEW queries.
   */
  private List<QueryDetailsModel> generateDropViewQuery(DropViewChangeSet dropViewChangeSet) {
    log.info("Inside generateDropViewQuery");
    List<QueryDetailsModel> explainQueryList = new ArrayList<>();
    for (DropViewModel dropViewObject : dropViewChangeSet.getChangeSets()) {
      String dropViewQuery = Queries.DROP_VIEW_TEMPLATE;
      dropViewQuery = dropViewQuery.replace(StringConstants.VIEW_NAME_IDENTIFIER,
          dropViewObject.getViewName());
      QueryDetailsModel queryDetails = new QueryDetailsModel(dropViewQuery, dropViewObject.getId());
      explainQueryList.add(queryDetails);
    }
    return explainQueryList;

  }

  /**
   * Generates ALTER TABLE queries based on the provided AlterTableChangeSet.
   *
   * @param alterTableChangeSet The AlterTableChangeSet containing information about table
   *                            alterations.
   * @return List of QueryDetailsModel objects containing the ALTER TABLE queries.
   */
  private List<QueryDetailsModel> generateAlterTableQuery(AlterTableChangeSet alterTableChangeSet) {
    log.info("Inside generateAlterTableQuery");
    List<QueryDetailsModel> queryDetailsModelList = new ArrayList<>();
    for (AlterTableModel alterTableObject : alterTableChangeSet.getChangeSets()) {
      String alterationType = alterTableObject.getAlterationType();
      ColumnDefinitionModel column = alterTableObject.getColumn();
      String tableName = alterTableObject.getTableName();
      log.info("Alteration type:: {}", alterationType);
      String alterTableQuery = switch (alterationType) {
        case StringConstants.ADD_COLUMN -> Queries.ALTER_TABLE_ADD_COLUMN_TEMPLATE.replace(
                StringConstants.TABLE_NAME_IDENTIFIER, tableName)
            .replace(StringConstants.COLUMN_NAME_IDENTIFIER, column.getName())
            .replace("{data_type}", column.getType());
        case StringConstants.DROP_COLUMN -> Queries.ALTER_TABLE_DROP_COLUMN_TEMPLATE.replace(
                StringConstants.TABLE_NAME_IDENTIFIER, tableName)
            .replace(StringConstants.COLUMN_NAME_IDENTIFIER, column.getName());
        case StringConstants.RENAME_COLUMN -> Queries.ALTER_TABLE_RENAME_COLUMN_TEMPLATE.replace(
                StringConstants.TABLE_NAME_IDENTIFIER, tableName)
            .replace("{old_column_name}", column.getName())
            .replace("{new_column_name}", column.getNewName());
        default -> null;
      };
      QueryDetailsModel queryDetails = new QueryDetailsModel(alterTableObject.getId());
      if (alterTableQuery == null) {
        log.info("Invalid alteration type:: {}", alterationType);
        queryDetails.setResult(StringConstants.FAILED);
        queryDetails.setError("Invalid alteration type " + alterationType);
      } else {
        queryDetails.setQuery(alterTableQuery);
      }
      queryDetailsModelList.add(queryDetails);
    }
    return queryDetailsModelList;
  }

  /**
   * Generates CREATE SCHEMA queries based on the provided CreateSchemaChangeSet.
   *
   * @param createSchemaChangeSet The CreateSchemaChangeSet containing information about schema to
   *                              be created.
   * @return List of QueryDetailsModel objects containing the CREATE schema queries.
   */
  private List<QueryDetailsModel> generateCreateSchemaQuery(
      CreateSchemaChangeSet createSchemaChangeSet) {
    log.info("Inside generateCreateSchemaQuery");
    List<QueryDetailsModel> explainQueryList = new ArrayList<>();
    for (CreateSchemaModel createSchemaModel : createSchemaChangeSet.getChangeSets()) {
      String[] schemaNamePartition = createSchemaModel.getSchemaName().split("\\.");
      if (schemaNamePartition.length != 2) {
        QueryDetailsModel failedQueryDetails = new QueryDetailsModel(createSchemaModel.getId());
        failedQueryDetails.setResult(StringConstants.FAILED);
        failedQueryDetails.setError(StringConstants.INVALID_SCHEMA_NAME_ERROR);
        explainQueryList.add(failedQueryDetails);
        continue;
      }
      String createSchemaQuery = Queries.CREATE_SCHEMA_TEMPLATE;
      createSchemaQuery = createSchemaQuery.replace(StringConstants.SCHEMA_IDENTIFIER,
          createSchemaModel.getSchemaName());
      QueryDetailsModel queryDetails = new QueryDetailsModel(createSchemaQuery,
          createSchemaModel.getId());
      explainQueryList.add(queryDetails);
    }
    return explainQueryList;

  }

  /**
   * Generates CREATE FUNCTION queries based on the provided CreateFunctionChangeSet.
   *
   * @param createFunctionChangeSet The CreateFunctionChangeSet containing information about
   *                                functions to be created.
   * @return List of QueryDetailsModel objects containing the CREATE function queries.
   */
  private List<QueryDetailsModel> generateCreateFunctionQuery(
      CreateFunctionChangeSet createFunctionChangeSet) {
    log.info("Inside generateCreateFunctionQuery");
    List<QueryDetailsModel> explainQueryList = new ArrayList<>();
    for (CreateFunctionModel createFunctionModel : createFunctionChangeSet.getChangeSets()) {
      QueryDetailsModel queryDetails = new QueryDetailsModel(
          createFunctionModel.getFunctionDefinition(), createFunctionModel.getId());
      explainQueryList.add(queryDetails);
    }
    return explainQueryList;

  }

  private List<QueryDetailsModel> generateDropFunctionQuery(
      DropFunctionChangeSet dropFunctionChangeSet) {
    log.info("Inside generateDropFunctionQuery");
    List<QueryDetailsModel> explainQueryList = new ArrayList<>();
    for (DropFunctionModel dropFunctionObject : dropFunctionChangeSet.getChangeSets()) {
      String dropFunctionQuery = Queries.DROP_FUNCTION_TEMPLATE;
      dropFunctionQuery = dropFunctionQuery.replace(StringConstants.FUNCTION_NAME_IDENTIFIER,
          dropFunctionObject.getFunctionName());
      QueryDetailsModel queryDetails = new QueryDetailsModel(dropFunctionQuery,
          dropFunctionObject.getId());
      explainQueryList.add(queryDetails);
    }
    return explainQueryList;
  }

}