package ai.commerceiq.schemamanagement.utils;

import ai.commerceiq.databricks.connection.library.exception.DatabricksConnectionFailureException;
import ai.commerceiq.databricks.connection.library.service.DatabricksConnectionManagerImpl;
import ai.commerceiq.schemamanagement.exception.MigrationException;
import ai.commerceiq.schemamanagement.exception.MigrationFailedException;
import ai.commerceiq.schemamanagement.model.entity.MigrationStatus;
import ai.commerceiq.schemamanagement.model.entity.ValidationMetaData;
import ai.commerceiq.schemamanagement.model.general.ChangeSetIDAndQueryModel;
import ai.commerceiq.schemamanagement.model.request.MigrationReqQueryParmsAndBody;
import ai.commerceiq.schemamanagement.model.response.FailedMigrationVersionDetails;
import ai.commerceiq.schemamanagement.model.response.QueryDetailsModel;
import ai.commerceiq.schemamanagement.repository.DropTableStatusRepository;
import ai.commerceiq.schemamanagement.repository.MigrationStatusRepository;
import ai.commerceiq.schemamanagement.service.GetExternalTableLocationService;
import ai.commerceiq.schemamanagement.utils.StringConstants.Queries;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DatabricksQueryExecutionUtils {

  @Autowired
  private MigrationStatusRepository migrationStatusRepository;
  @Autowired
  private DropTableStatusRepository dropTableStatusRepository;
  @Autowired
  private GetExternalTableLocationService getExternalTableLocationService;
  @Autowired
  private CommonUtils commonUtils;

  @Autowired
  private MigrationExecutionHelperUtils migrationExecutionHelperUtils;

  @Value("${databricks.aws.secret}")
  private String databricksCred;

  /**
   * Processes the given Explain query on a Databricks connection.
   *
   * @param explainQueriesList The list of SQL queries to be executed.
   */
  public void processExplainQueries(List<QueryDetailsModel> explainQueriesList) {
    log.info("Inside processExplainQueries");

    try (Connection connection = new DatabricksConnectionManagerImpl().getConnection(
        databricksCred); Statement statement = connection.createStatement()) {
      for (QueryDetailsModel explainQueryDetails : explainQueriesList) {
        if (explainQueryDetails.getResult() != null && explainQueryDetails.getResult()
            .equals(StringConstants.FAILED)) {
          continue;
        }
        String result = executeExplainQueries(statement, explainQueryDetails);
        explainQueryDetails.setResult(StringConstants.SUCCESS);
        if (result != null) {
          explainQueryDetails.setResult(StringConstants.FAILED);
          explainQueryDetails.setError(result);
        }
      }
    } catch (DatabricksConnectionFailureException | InterruptedException | JsonProcessingException |
             SQLException e) {
      log.info("Error while establishing connection to Databricks:: {}", e.getLocalizedMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Executes the given Explain query on a Databricks connection.
   *
   * @param explainQueryDetails The list of SQL queries to be executed.
   * @param statement           The Statement to execute query.
   */
  private String executeExplainQueries(Statement statement, QueryDetailsModel explainQueryDetails) {
    log.info("Inside executeExplainQueries");
    String result;
    try {
      String explainQuery = StringConstants.EXPLAIN + explainQueryDetails.getQuery();

      if (explainQuery.contains(StringConstants.CREATE_TABLE)) {
        explainQuery = commonUtils.addLocationBasedCreateTableQuery(explainQuery);
        String queryToCheckSchemaExist = getSelectSchemaFromInformationSchemaQuery(explainQuery);
        ResultSet selectTableQueryResult = statement.executeQuery(queryToCheckSchemaExist);
        result = checkSchemaExistInResultSet(selectTableQueryResult);
        if (result != null) {
          return result;
        }
      }

      log.info("Executing explain query::{}", explainQuery);
      result = getExplainQueryErrorResult(statement.executeQuery(explainQuery));
      if (result == null && (explainQuery.contains(StringConstants.CREATE_TABLE)
          || explainQuery.contains(StringConstants.DROP_TABLE))) {
        String selectTableFromInformationSchemaQuery = getSelectTablesFromInformationSchemaQuery(
            explainQuery);
        ResultSet selectTableQueryResult = statement.executeQuery(
            selectTableFromInformationSchemaQuery);
        result = checkForDuplicateTableName(selectTableQueryResult, explainQuery);
      }
    } catch (SQLException e) {
      log.info("Error while executing the query:: {}", e.getLocalizedMessage());
      result = e.getMessage();
    }
    return result;
  }

  public void executeMigrationQueries(List<ValidationMetaData> validationMetaDataRecords,
      MigrationReqQueryParmsAndBody migrateRequestBody) {
    log.info("Inside executeMigrationQueries");
    List<FailedMigrationVersionDetails> failedMigrationVersionList = new ArrayList<>();
    try (Connection connection = new DatabricksConnectionManagerImpl().getConnection(
        databricksCred); Statement statement = connection.createStatement()) {
      for (ValidationMetaData validationMetaData : validationMetaDataRecords) {
        log.info("Executing migration for version:: {}", validationMetaData.getVersion());
        Map<Integer, String> sortedChangeSetIDQueries = new TreeMap<>(
            validationMetaData.getQueries());
        log.info("Sorted queries based on change set id::{}", sortedChangeSetIDQueries);
        ArrayList<ChangeSetIDAndQueryModel> failedMigrationChangesetIdList = new ArrayList<>();
        int version = validationMetaData.getVersion();
        for (Map.Entry<Integer, String> entry : sortedChangeSetIDQueries.entrySet()) {
          int changeSetId = entry.getKey();
          String query = entry.getValue();
          log.info("Change set id:: {}  and query:: {}", changeSetId, query);
          try {
            String queryType = CommonUtils.getQueryType(query);
            switch (queryType) {
              case StringConstants.DROP_TABLE:
                processDropTableQuery(entry, statement, version);
                break;
              case StringConstants.CREATE_TABLE:
                executeCreateTableQuery(query, statement);
                break;
              case StringConstants.CREATE_SCHEMA:
                executeCreateSchemaQuery(query, statement);
                break;
              case StringConstants.ALTER_TABLE:
                executeAlterTableQuery(query, statement);
                break;
              default:
                log.info("Executing query: {}", query);
                statement.execute(query);
                break;
            }
          } catch (SQLException | RuntimeException e) {
            log.error("Error while executing query at databricks:: {}", e.getMessage());
            failedMigrationChangesetIdList.add(
                new ChangeSetIDAndQueryModel(changeSetId, query, e.getMessage()));
          }
        }
        if (!failedMigrationChangesetIdList.isEmpty()) {
          FailedMigrationVersionDetails failedMigrationVersionDetails = new FailedMigrationVersionDetails(
              version, failedMigrationChangesetIdList);
          failedMigrationVersionList.add(failedMigrationVersionDetails);
          migrationStatusRepository.save(
              new MigrationStatus(version, migrateRequestBody.getUserName(), false));
        } else {
          migrationStatusRepository.save(
              new MigrationStatus(version, migrateRequestBody.getUserName(), true));
        }
      }
      if (!failedMigrationVersionList.isEmpty()) {
        log.info("Migration failed for following version:: {}", failedMigrationVersionList);
        throw new MigrationFailedException(failedMigrationVersionList, migrateRequestBody);
      }

    } catch (DatabricksConnectionFailureException | InterruptedException | JsonProcessingException |
             SQLException e) {
      log.error("Error while establishing connection to databricks:: {}", e.getMessage());
      throw new MigrationException(migrateRequestBody, e.getMessage());
    }
  }

  private void executeAlterTableQuery(String query, Statement statement) throws SQLException {
    log.info("Inside executeAlterTableQuery");
    statement.execute(query);
    boolean hasClientId = checkForClientIdInTbl(query, statement);
    String viewCreationQuery;
    if (hasClientId) {
      viewCreationQuery = getCreateRLSViewQuery(query);
    } else {
      viewCreationQuery = getCreateNormalViewQuery(query);
    }
    statement.execute(viewCreationQuery);
  }

  private boolean checkForClientIdInTbl(String query, Statement statement) throws SQLException {
    log.info("Inside checkForClientIdInTbl");
    String fullTableName = CommonUtils.getTableNameFromQuery(query);
    String getTableColsQuery = Queries.GET_TABLE_COLUMNS.replace("{catalog.schema.table}",
        fullTableName);
    ResultSet resultSet = statement.executeQuery(getTableColsQuery);
    while (resultSet.next()) {
      if (StringConstants.CLIENT_ID.equalsIgnoreCase(
          resultSet.getString(StringConstants.COL_NAME))) {
        return true;
      }
    }
    return false;
  }


  /**
   * Generates a dynamic view drop query based on the input DROP TABLE query.
   *
   * @param fullTableName The DROP TABLE name for which the dynamic view drop query is being
   *                      generated.
   * @return The dynamic view drop query based on the input DROP TABLE query.
   */
  public String getDropDynamicViewQuery(String fullTableName) {
    log.info("Inside getDropDynamicViewQuery");
    String catalogSchemaTableName = CommonUtils.getDynamicViewName(fullTableName);

    String dropViewDynamicQuery = StringConstants.Queries.DROP_DYNAMIC_VIEW_TEMPLATE.replace(
        "{catalog.schema.table_name}", catalogSchemaTableName);
    log.info("Drop dynamic view query:: {}", dropViewDynamicQuery);
    return dropViewDynamicQuery;
  }

  /**
   * Generates a dynamic view creation query based on the input CREATE TABLE query.
   *
   * @param query The CREATE TABLE query for which the dynamic view creation query is being
   *              generated.
   * @return The dynamic view creation query based on the input CREATE TABLE query.
   */
  private String getCreateRLSViewQuery(String query) {
    log.info("Inside getCreateDynamicViewQuery");
    String fullTableName = CommonUtils.getTableNameFromQuery(query);
    String catalogSchemaTableName = CommonUtils.getDynamicViewName(fullTableName);

    String createDynamicViewQuery = StringConstants.Queries.RLS_VIEW_CREATION_TEMPLATE;
    createDynamicViewQuery = createDynamicViewQuery.replace(
            StringConstants.VIEW_CATALOG_TABLE_NAME_IDENTIFIER, catalogSchemaTableName)
        .replace(StringConstants.TABLE_NAME_IDENTIFIER, fullTableName);
    log.info("Create dynamic view query:: {}", createDynamicViewQuery);
    return createDynamicViewQuery;
  }

  /**
   * Generates a SELECT query for retrieving table information from the Information Schema based on
   * the input CREATE TABLE query.
   *
   * @param query The CREATE TABLE query for which the SELECT query is being generated.
   * @return The SELECT query for retrieving table information from the Information Schema.
   */
  private String getSelectTablesFromInformationSchemaQuery(String query) {
    log.info("Inside getSelectTablesFromInformationSchemaQuery");
    String fullTableName = CommonUtils.getTableNameFromCreateTableQuery(query);
    String catalog = CommonUtils.getTableComponents(fullTableName, "catalog");
    String schema = CommonUtils.getTableComponents(fullTableName, "schema");
    String tableName = CommonUtils.getTableComponents(fullTableName, "table_name");
    String selectTableQuery = StringConstants.Queries.SELECT_TABLE_NAME_TEMPLATE.replace(
        "{catalog}",
        catalog).replace("{schema_name}", schema).replace("{table_name}", tableName);
    log.info("Created select table name query:: {}", selectTableQuery);
    return selectTableQuery;
  }

  /**
   * Parses the ResultSet to extract and build an explanation of any error encountered during query
   * planning. This method reads the ResultSet, specifically the first character stream, and
   * constructs an explanation by appending each line. It detects an error in query planning using a
   * predefined identifier string.
   *
   * @param result The ResultSet containing information about the query execution.
   * @return A string representation of the error explanation in query planning, or null if no error
   * is detected.
   * @throws RuntimeException if a SQLException or IOException occurs during the process.
   */
  private String getExplainQueryErrorResult(ResultSet result) {
    boolean isFirstLine = true;
    boolean errorInQueryPlaning = false;
    StringBuilder sb = new StringBuilder();
    try {
      while (result.next()) {
        Reader reader = result.getCharacterStream(1);
        BufferedReader br = new BufferedReader(reader);
        String line;
        while ((line = br.readLine()) != null) {
          sb.append(line).append(" ");
          if (isFirstLine) {
            if (line.equals(StringConstants.QUERY_PLAL_FAILED_IDENTIFIER_STRING)) {
              isFirstLine = false;
              errorInQueryPlaning = true;
            } else {
              break;
            }
          }
        }
      }
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }
    if (errorInQueryPlaning) {
      log.info("Error in query planning:: {}", sb);
      return sb.toString();
    }
    return null;
  }

  /**
   * Checks for the existence of a duplicate table name in the context of a given SQL query and
   * result set.
   *
   * @param resultSet The ResultSet containing information about the query execution.
   * @param query     The SQL query for which the duplicate table name is being checked.
   * @return An error string if a duplicate table name or not-found scenario is detected, otherwise
   * null.
   * @throws RuntimeException if a SQLException occurs during the process.
   */
  private String checkForDuplicateTableName(ResultSet resultSet, String query) {
    log.info("Inside checkForDuplicateTableName");
    try {
      if (resultSet.next()) {
        if (query.contains(StringConstants.CREATE_TABLE)) {
          log.info(StringConstants.DUPLICATE_TABLE_ERROR);
          return StringConstants.DUPLICATE_TABLE_ERROR;
        }
      } else if (query.contains(StringConstants.DROP_TABLE)) {
        log.info(StringConstants.DROP_TABLE_NOT_FOUND);
        return StringConstants.DROP_TABLE_NOT_FOUND;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  private void executeCreateTableQuery(String query, Statement statement) throws SQLException {
    log.info("Inside executeCreateTableQuery");
    query = commonUtils.addLocationBasedCreateTableQuery(query);
    log.info("Executing query::{}", query);
    statement.execute(query);
    String viewCreationQuery;
    if (query.contains(StringConstants.PARTITION_BY_LITERAL)) {
      log.info("Create dynamic view ");
      viewCreationQuery = getCreateRLSViewQuery(query);
    } else {
      log.info("Creating normal view");
      viewCreationQuery = getCreateNormalViewQuery(query);
    }

    statement.execute(viewCreationQuery);

  }

  private String getCreateNormalViewQuery(String query) {
    log.info("Inside getCreateNormalViewQuery");
    String fullTableName = CommonUtils.getTableNameFromQuery(query);
    String catalogSchemaTableName = CommonUtils.getDynamicViewName(fullTableName);

    String createNormalViewQuery = StringConstants.Queries.NORMAL_VIEW_CREATION_TEMPLATE;
    createNormalViewQuery = createNormalViewQuery.replace(
            StringConstants.VIEW_CATALOG_TABLE_NAME_IDENTIFIER, catalogSchemaTableName)
        .replace(StringConstants.TABLE_NAME_IDENTIFIER, fullTableName);
    log.info("Create normal view query:: {}", createNormalViewQuery);
    return createNormalViewQuery;
  }

  private void executeCreateSchemaQuery(String query, Statement statement) throws SQLException {
    log.info("Inside executeCreateSchemaQuery");
    statement.execute(query);
    String completeSchemaName = query.split(" ")[2];
    String catalogName = completeSchemaName.split("\\.")[0];
    String schemaName = completeSchemaName.split("\\.")[1];
    if (catalogName.equals(StringConstants.CLIENT_CATALOG) || catalogName.equals(
        StringConstants.COMMON_CATALOG)) {
      String viewCatalog =
          catalogName.equals(StringConstants.COMMON_CATALOG) ? StringConstants.COMMON_VIEW_CATALOG
              : StringConstants.CLIENT_VIEW_CATALOG;
      log.info("Creating schema in {}", viewCatalog);
      String viewCatalogSchemaName = StringConstants.SCHEMA_NAME_TEMPLATE.replace(
              StringConstants.CATALOG_IDENTIFIER, viewCatalog)
          .replace(StringConstants.SCHEMA_IDENTIFIER, schemaName);
      query = StringConstants.Queries.CREATE_SCHEMA_TEMPLATE.replace(
          StringConstants.SCHEMA_IDENTIFIER,
          viewCatalogSchemaName);
      log.info("Executing query - {}", query);
      statement.execute(query);
    }
  }

  private void processDropTableQuery(Map.Entry<Integer, String> entry, Statement statement,
      int version) throws SQLException, RuntimeException {
    log.info("Inside executeDropTableQuery");
    int changeSetId = entry.getKey();
    String query = entry.getValue();
    String fullTableName = CommonUtils.getDropTableName(query);
    executeDropDynamicViewQuery(fullTableName, statement);
    String tableLocation = getExternalTableLocationService.getTableLocation(fullTableName);
    dbxQueryExecutor(query, statement);
    migrationExecutionHelperUtils.handleS3LocationForDroppedTable(fullTableName, tableLocation,
        version, changeSetId);
  }

  public void executeDropDynamicViewQuery(String fullTableName, Statement statement)
      throws SQLException {
    log.info("Inside executeDropDynamicQuery");
    String dropDynamicViewQuery = getDropDynamicViewQuery(fullTableName);
    log.info("Drop dynamic query::{}", dropDynamicViewQuery);
    if (dropDynamicViewQuery != null) {
      statement.execute(dropDynamicViewQuery);
    }
  }

  private String checkSchemaExistInResultSet(ResultSet resultSet) {
    log.info("Inside checkSchemaExistInResultSet");
    try {
      if (resultSet.next()) {
        return null;
      }
    } catch (SQLException e) {
      log.info("Error while checking for schema existence at databricks:: {}",
          e.getLocalizedMessage());
      return e.getLocalizedMessage();
    }
    log.info(StringConstants.SCHEMA_NOT_PRESENT_ERROR);
    return StringConstants.SCHEMA_NOT_PRESENT_ERROR;
  }

  private String getSelectSchemaFromInformationSchemaQuery(String query) {
    log.info("Inside getSelectSchemaFromInformationSchemaQuery");
    String fullTableName = CommonUtils.getTableNameFromCreateTableQuery(query);
    String catalog = CommonUtils.getTableComponents(fullTableName, "catalog");
    String schema = CommonUtils.getTableComponents(fullTableName, "schema");
    String selectSchemaQuery = StringConstants.Queries.SELECT_SCHEMA_FROM_INFORMATION_SCHEMA.replace(
            StringConstants.CATALOG_IDENTIFIER, catalog)
        .replace(StringConstants.SCHEMA_IDENTIFIER, schema.toLowerCase());
    log.info("Select schema query:: {}", selectSchemaQuery);
    return selectSchemaQuery;
  }

  public Connection createDbxConnection()
      throws DatabricksConnectionFailureException, InterruptedException, JsonProcessingException {
    log.info("Inside createDbxConnection");
    return new DatabricksConnectionManagerImpl().getConnection(databricksCred);
  }

  public void dbxQueryExecutor(String query, Statement statement) throws SQLException {
    log.info("Executing query::{}", query);
    statement.execute(query);
  }
}
