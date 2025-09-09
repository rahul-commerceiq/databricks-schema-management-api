package ai.commerceiq.schemamanagement.service;


import ai.commerceiq.databricks.connection.library.exception.DatabricksConnectionFailureException;
import ai.commerceiq.schemamanagement.model.general.TableNameEntity;
import ai.commerceiq.schemamanagement.model.response.DeleteTablesAndS3Response;
import ai.commerceiq.schemamanagement.scheduler.DropExternalTableLocationScheduler;
import ai.commerceiq.schemamanagement.utils.DatabricksQueryExecutionUtils;
import ai.commerceiq.schemamanagement.utils.QueryGeneratorUtils;
import ai.commerceiq.schemamanagement.utils.StringConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DropTableService {

  @Autowired
  DatabricksQueryExecutionUtils databricksQueryExecutionUtils;
  @Autowired
  QueryGeneratorUtils queryGeneratorUtils;
  @Autowired
  GetExternalTableLocationService getExternalTableLocationService;
  @Autowired
  DropExternalTableLocationScheduler dropExternalTableLocationScheduler;

  /**
   * #step1. drop dynamic view #step2. drop table #step3. clean up s3 path
   *
   * @param dropTablesList contains list of table names to be deleted
   */
  public List<DeleteTablesAndS3Response> dropTables(List<String> dropTablesList) {
    log.info("Inside dropTables");
    List<DeleteTablesAndS3Response> response = new ArrayList<>();
    Connection connection = null;
    Statement statement = null;
    try {
      connection = databricksQueryExecutionUtils.createDbxConnection();
      statement = connection.createStatement();
    } catch (DatabricksConnectionFailureException | InterruptedException | JsonProcessingException |
             SQLException e) {
      throw new RuntimeException(e);
    }

    for (String tableName : dropTablesList) {
      log.info("deleting table:: {}", tableName);
      DeleteTablesAndS3Response deleteTablesAndS3Response = new DeleteTablesAndS3Response();
      try {
        TableNameEntity tableNameObject = new TableNameEntity(tableName);
        deleteTablesAndS3Response.setTableName(tableNameObject.getFullTableName());

        String tableLocation = getExternalTableLocationService.getTableLocation(
            tableNameObject.getFullTableName());

        databricksQueryExecutionUtils.executeDropDynamicViewQuery(
            tableNameObject.getFullTableName(), statement);

        String dropTableQuery = queryGeneratorUtils.generateDropTableQuery(
            tableNameObject.getFullTableName());
        databricksQueryExecutionUtils.dbxQueryExecutor(dropTableQuery, statement);

        dropExternalTableLocationScheduler.deleteS3Path(tableLocation);
        deleteTablesAndS3Response.setError(null);
        deleteTablesAndS3Response.setStatus(StringConstants.SUCCESS);

      } catch (SQLException | RuntimeException e) {
        log.info("Error while deleting table: {}, Error: {}", tableName, e.getMessage());
        deleteTablesAndS3Response.setTableName(tableName);
        deleteTablesAndS3Response.setStatus(StringConstants.FAILED);
        String errorMessage = e.getMessage();
        deleteTablesAndS3Response.setError(
            errorMessage.length() > 500 ? errorMessage.substring(0, 500) : errorMessage);
      }
      response.add(deleteTablesAndS3Response);
    }
    return response;
  }
}
