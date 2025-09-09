package ai.commerceiq.schemamanagement.utils;


import java.nio.file.Path;
import java.util.List;

public class StringConstants {


  //YAML file name pattern
  public static final String FILE_NAME_PATTERN = "\\d{4}_\\d{2}_\\d{2}__[a-zA-Z0-9_]+\\.yaml";
  //DDL types
  public static final String CREATE_TABLE_DDL = "create_table";
  public static final String DROP_TABLE_DDL = "drop_table";
  public static final String CREATE_VIEW_DDL = "create_view";
  public static final String UPDATE_VIEW_DDL = "update_view";
  public static final String DROP_VIEW_DDL = "drop_view";
  public static final String ALTER_TABLE_DDL = "alter_table";
  public static final String CREATE_SCHEMA_DDL = "create_schema";
  public static final String CREATE_FUNCTION_DDL = "create_function";
  public static final String UPDATE_FUNCTION_DDL = "update_function";
  public static final String DROP_FUNCTION_DDL = "drop_function";

  //Alternations types for ALTER TABLE query
  public static final String ADD_COLUMN = "add_column";
  public static final String DROP_COLUMN = "drop_column";
  public static final String RENAME_COLUMN = "rename_column";
  public static final String CORRELATION_ID_KEY = "CorrelationId";
  public static final String QUERY_PLAL_FAILED_IDENTIFIER_STRING = "Error occurred during query planning: ";

  //Error messages
  public static final String DUPLICATE_TABLE_ERROR = "Error: Duplicate table, table with given name already exist";
  public static final String DROP_TABLE_NOT_FOUND = "Error: Table does not exist";
  public static final String NO_NEW_FILES_OR_CHANGES_IN_GIT_BRANCH = "Error: Specified branch does not contains any new files/changes";
  public static final String NO_FILES_VALIDATED_FOR_MIGRATION = "Error: No new files are validated to migrate";
  public static final String MIGRATION_FILES_NOT_FOUND_IN_DB = "Error: Files could not be located in the database, please ensure that the files exist before attempting the operation again.";
  public static final String FILES_PATH_LIST_CANNOT_BE_EMPTY = "Error: Migration file list cannot be empty.";
  public static final String INVALID_TABLE_NAME_ERROR = "Error: Invalid table name. Table name should follow the convention of catalog.schema.table_name";
  public static final String INVALID_SCHEMA_NAME_ERROR = "Error: Invalid schema name. schema name should follow the convention of catalog.schema_name";
  public static final String SCHEMA_NOT_PRESENT_ERROR = "Error: Schema not present at databricks";
  public static final String INVALID_DDL_OPERATION = "Error: Invalid DDL operation.";
  public static final String MIGRATED_FILES_FOR_MIGRATION = "Error: Migrated files in the file list";
  public static final String ABSENT_FIELD_ERROR_PREFIX = "Following fields are not present: ";
  public static final String AUTOINCREMENT_VALIDATION_FAILED_ERROR = "AUTOINCREMENT should be present only in one column with type either LONG or BIGINT";
  public static final String MIGRATION_FAILED_MESSAGE = "Migration Failed";
  public static final String TABLE_NAME_RETRIVAL_ERROR = "Error: Could not get tables name out of query";
  public static final String HAS_PARTITION_WITH_OUT_CLIENT_ID_ERROR = "Client Id partition is must to create table with other partition columns";

  //Environments
  public static final String BETA_ENV = "beta-dbx";
  public static final String PROD_ENV = "prod-dbx";
  public static final String QA_ENV = "qa-dbx";
  //Status
  public static final String SUCCESS = "Success";
  public static final String FAILED = "Failed";
  //Git credentials
  public static final String VERSION_MIGRATION_REPOSITORY_NAME = "databricks-schema-management";
  public static final String VERSION_MIGRATION_GIT_URL = "https://bitbucket.org/commerceiq/databricks-schema-management.git";
  public static final String GIT = "git";
  public static final String CIQ = "ciq";
  public static final String READUSER = "readuser";
  public static final String GIT_CIQ_READUSER = "bitbucketcommerceiqread";
  public static final String MASTER_BRANCH = "master";
  public static final long DAYS_IN_MILLISECONDS = 24 * 60 * 60 * 1000;
  //PagerDuty
  public static final String DATABRICKS_MIGRATION_FAILURE = "{env}: Databricks migration failed ";
  public static final String DATABRICKS_SCHEMA_MANAGEMENT_API = "databricks-schema-management-api";
  public static final String ALERT = "Alert";
  public static final String TRIGGERS = "Trigger";
  public static final String PAGERDUTY_INTEGRATION_KEY = "9b9a6271921a490fd022d3dddd413174";
  public static final String DATABRICKS = "databricks";

  //Databricks credentials
  public static final String AUTH_TOKEN = "authToken";
  //Commonly used strings

  public static final String HAS_PARTITION_WITH_OUT_CLIENT_ID = "hasPartitionWithOutClientId";
  public static final String COLUMNS_STRING = "columnsString";
  public static final String HAS_CLIENT_ID_COL = "hasClientIdColumn";
  public static final String PARTITION_COLS_STRING = "partitionColString";

  public static final String PARTITION_BY_LITERAL = "PARTITIONED BY (";


  public static final String CATALOG_IDENTIFIER = "{catalog}";
  public static final String SCHEMA_IDENTIFIER = "{schema}";
  public static final String TABLE_NAME_IDENTIFIER = "{table_name}";
  public static final String LOCATION_IDENTIFIER = "{location}";
  public static final String COLUMN_NAME_IDENTIFIER = "{column_name}";
  public static final String PARTITION_IDENTIFIER = "{partition}";
  public static final String VIEW_CATALOG_TABLE_NAME_IDENTIFIER = "{view_catalog.schema.table_name}";
  public static final String VIEW_NAME_IDENTIFIER = "{view_name}";
  public static final String FUNCTION_NAME_IDENTIFIER = "{function_name}";
  public static final String COL_NAME = "col_name";
  public static final String EXPLAIN = "EXPLAIN ";
  public static final String SCHEMA_NAME_TEMPLATE = "{catalog}.{schema}";
  public static final String CREATE_TABLE = "CREATE TABLE";
  public static final String AUTOINCREMENT_VALIDATION = "autoIncrementValidation";
  public static final String CUSTOM_AUTO_INCREMENT = " GENERATED BY DEFAULT AS IDENTITY (START WITH {start_with} INCREMENT BY {increment_by}) ";
  public static final String CREATE_SCHEMA = "CREATE SCHEMA";
  public static final String DROP_TABLE = "DROP TABLE";
  public static final String ALTER_TABLE = "ALTER TABLE";
  public static final String DROP_VIEW = "DROP VIEW";
  public static final String DROP_FUNCTION = "DROP FUNCTION";
  public static final String CREATE_FUNCTION = "CREATE FUNCTION";
  public static final String CREATE_VIEW = "CREATE VIEW";
  public static final List<String> CREATE_FUNCTION_POSSIBLE_SUBSTRINGS = List.of(CREATE_FUNCTION,
      "CREATE OR REPLACE FUNCTION", "create function", "create or replace function");
  public static final List<String> CREATE_VIEW_POSSIBLE_SUBSTRINGS = List.of(CREATE_VIEW,
      "CREATE OR REPLACE VIEW", "create view", "create or replace view");


  public static final String DROP_TABLE_STATUS_QUEUED = "queued";
  public static final String DROP_TABLE_STATUS_OBSOLETE = "obsolete";
  public static final Path DSM_FOLDER_PATH = Path.of("resources/db_migration");
  public static final String EMPTY_STRING = "";
  public static final String SEMICOLON = ";";
  public static final String COMMA_DELIMITER = ", ";
  public static final String CATALOG = "catalog";
  public static final String SCHEMA = "schema";
  public static final String TABLE_NAME = "table_name";
  public static final String CLIENT_ID = "CLIENT_ID";
  public static final String CLIENT_CATALOG = "client_catalog";
  public static final String COMMON_CATALOG = "common_catalog";
  public static final String CLIENT_VIEW_CATALOG = "client_view_catalog";
  public static final String COMMON_VIEW_CATALOG = "common_view_catalog";
  public static final String DELETED = "deleted";
  public static final String NAME = "name";
  public static final String TYPE = "type";
  public static final String COMMENT = "comment";
  public static final List<String> REQUIRED_FIELDS_IN_COLUMN_INFO = List.of(NAME, TYPE, COMMENT);
  // Databricks supported datatypes -- Move all these to a ENUM
  public static final String BIGINT = "bigint";
  public static final String LONG = "long";
  public static final String BOOLEAN = "boolean";
  public static final String TIMESTAMP = "timestamp";
  public static final String DATE = "date";
  public static String EMPTY_STRING_LITERAl = "";

  public static class Queries {

    //Create table and dynamic view template
    public static final String CREATE_EXTERNAL_TABLE_TEMPLATE = """
        CREATE TABLE {table_name} {columns}
        USING delta
        {partition}
        LOCATION '{location}'
        TBLPROPERTIES ( 'team'='{table_owner}', 'autoOptimize.autoCompact' = 'true', 'autoOptimize.maxFileSize' = '128m', 'autoOptimize.optimizeWrite' = 'true', 'delta.feature.allowColumnDefaults' = 'supported', 'delta.feature.identityColumns' = 'supported', 'delta.feature.invariants' = 'supported', 'delta.minReaderVersion' = '1', 'delta.minWriterVersion' = '7', 'delta.targetFileSize' = '134217728','delta.columnMapping.mode' = 'name','delta.deletedFileRetentionDuration' = '{deletedFileRetentionDuration}', 'delta.logRetentionDuration' = '{logRetentionDuration}');
        """;
    public static final String RLS_VIEW_CREATION_TEMPLATE = """
        create or replace view {view_catalog.schema.table_name} AS
        SELECT
          *
        FROM
          {table_name}
        WHERE
          CLIENT_ID IN (
            SELECT
              DISTINCT b.child_client_id
            FROM
              admin_catalog.ciq.access_control_table a
              INNER JOIN admin_catalog.ciq.parent_child_mapping b ON a.client_id = b.parent_client_id
              and USER_ID = current_user()
        )
        """;

    public static final String NORMAL_VIEW_CREATION_TEMPLATE = """
        create or replace view {view_catalog.schema.table_name} AS
        SELECT
          *
        FROM
          {table_name}
              
        """;
    public static final String GET_TABLE_COLUMNS = """
        show columns in {catalog.schema.table}
        """;

    //Drop table and view template
    public static final String DROP_TABLE_TEMPLATE = "DROP TABLE   {table_name}";
    public static final String DROP_DYNAMIC_VIEW_TEMPLATE = "DROP VIEW IF EXISTS {catalog.schema.table_name}";
    public static final String DROP_VIEW_TEMPLATE = "DROP VIEW {view_name}";
    public static final String DROP_FUNCTION_TEMPLATE = "DROP FUNCTION {function_name}";

    //Alter table template
    public static final String ALTER_TABLE_ADD_COLUMN_TEMPLATE = """
        ALTER TABLE {table_name}
        ADD COLUMN {column_name} {data_type};
        """;
    public static final String ALTER_TABLE_DROP_COLUMN_TEMPLATE = """
        ALTER TABLE {table_name}
        DROP COLUMN {column_name};
        """;
    public static final String ALTER_TABLE_RENAME_COLUMN_TEMPLATE = """
        ALTER TABLE {table_name}
        RENAME COLUMN {old_column_name} TO {new_column_name};
        """;

    //Create schema template
    public static final String CREATE_SCHEMA_TEMPLATE = "CREATE SCHEMA {schema}";

    //Selection queries
    public static final String SELECT_TABLE_NAME_TEMPLATE = """
        SELECT *
        FROM {catalog}.INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA='{schema_name}' AND TABLE_NAME='{table_name}';
        """;

    public static final String SELECT_FILE_METADATA_RECORD = """
        SELECT *
        FROM file_metadata fm
        WHERE fm.absolute_file_path = :absoluteFilePath
        """;
    public static final String GET_MIGRATED_FILES_CHECKSUM_TEMPLATE = """
        SELECT fm.checksum, fm.absolute_file_path
        FROM file_metadata fm
        JOIN migration_status ms ON fm.version = ms.version
        """;
    public static final String GET_VALIDATED_NON_MIGRATED_FILES_CHECKSUM_TEMPLATE = """
        SELECT fm.absolute_file_path, fm.checksum
        FROM file_metadata fm
        JOIN databricks_metadata_beta.validation_metadata vm ON fm.version = vm.version
        WHERE fm.absolute_file_path IN :filePaths
        """;

    public static final String SELECT_MIGRATED_FILES_FROM_LIST = """
        SELECT  fm.absolute_file_path
        FROM file_metadata fm
        JOIN migration_status ms ON fm.version = ms.version
         WHERE fm.absolute_file_path IN :filePaths
        """;
    public static final String GET_NON_MIGRATED_FILES_PATH_BETA_TEMPLATE = """
        SELECT fm.absolute_file_path
        FROM file_metadata fm
        JOIN databricks_metadata_beta.validation_metadata vm ON fm.version = vm.version
        LEFT JOIN migration_status ms ON fm.version = ms.version
        WHERE vm.version IS NOT NULL AND ms.version IS NULL
        """;
    public static final String GET_NON_MIGRATED_FILES_PATH_PROD_TEMPLATE = """
        SELECT fm.absolute_file_path
        FROM file_metadata fm
        JOIN databricks_metadata_beta.migration_status msb ON fm.version = msb.version
        LEFT JOIN databricks_metadata_prod.migration_status msp ON fm.version = msp.version
        LEFT JOIN databricks_metadata_qa.migration_status msq ON fm.version = msq.version
        WHERE msb.version IS NOT NULL
        AND msp.version IS NULL
        AND msq.version IS NOT NULL
        AND msb.is_migrated = true
        AND msq.is_migrated = true
        """;

    public static final String GET_NON_MIGRATED_FILES_PATH_QA_TEMPLATE = """
        SELECT fm.absolute_file_path
        FROM file_metadata fm
        JOIN databricks_metadata_beta.migration_status msb ON fm.version = msb.version
        LEFT JOIN databricks_metadata_qa.migration_status msq ON fm.version = msq.version
        WHERE msb.version IS NOT NULL AND msq.version IS NULL AND msb.is_migrated = true
        """;
    public static final String SELECT_QUERIES_AND_VERSION_BY_PATHS = """
        SELECT vm.queries,vm.version ,vm.is_validated
        FROM databricks_metadata_beta.validation_metadata vm
        JOIN file_metadata fm ON vm.version = fm.version
        WHERE fm.absolute_file_path IN :absoluteFilePaths
        """;
    public static final String SELECT_FILES_NOT_MIGRATED_IN_BETA_FROM_GIVEN_FILE_LIST = """
        SELECT fm.absolute_file_path
        FROM file_metadata fm
        JOIN databricks_metadata_beta.migration_status msb ON fm.version = msb.version
        WHERE msb.version IS NULL AND fm.absolute_file_path IN :filePaths
        """;
    public static final String SELECT_FILES_NOT_MIGRATED_IN_BETA_AND_QA_FROM_GIVEN_FILE_LIST = """
        SELECT fm.absolute_file_path
        FROM file_metadata fm
        JOIN databricks_metadata_beta.migration_status msb ON fm.version = msb.version
        LEFT JOIN databricks_metadata_qa.migration_status msq ON fm.version = msq.version
        WHERE fm.absolute_file_path IN :filePaths AND (msb.version IS NULL OR msq.version IS NULL);
        """;
    public static final String SELECT_DROP_TABLES_RECORD = """
        SELECT *
        FROM databricks_drop_table_status d
        WHERE d.updated_at <= :thresholdDays AND d.status = 'queued' AND d.force_delete=false
        """;

    public static final String CHECK_TO_FORCE_DROP_FORCE_DROP_TABLES_RECORD = """
        SELECT *
        FROM databricks_drop_table_status d
        WHERE d.version = :version AND d.status = 'queued' AND d.force_delete=true And d.change_set_id = :changeSetId
        """;

    public static final String BETA_SELECT_FORCE_DROP_TABLES_RECORD = """
        SELECT *
        FROM databricks_metadata_beta.databricks_drop_table_status d
        WHERE d.version = :version  AND d.force_delete = true And d.change_set_id = :changeSetId
        """;
    public static final String QA_SELECT_FORCE_DROP_TABLES_RECORD = """
        SELECT *
        FROM databricks_metadata_qa.databricks_drop_table_status d
        WHERE d.version = :version AND d.force_delete=true And d.change_set_id = :changeSetId
        """;
    public static final String PROD_SELECT_FORCE_DROP_TABLES_RECORD = """
        SELECT *
        FROM databricks_metadata_prod.databricks_drop_table_status d
        WHERE d.version = :version  AND d.force_delete=true And d.change_set_id = :changeSetId
        """;

    public static final String INSERT_FORCE_DROP_TABLES_RECORD_TO_QA_DROP_STATUS_TBL = """
        INSERT INTO
        databricks_metadata_qa.databricks_drop_table_status
        (table_name, status, version, force_delete, change_set_id)
        VALUES
        (:tableName, "queued",:version,true,:changeSetId)
        """;

    public static final String UPDATE_FORCE_DROP_TABLES_RECORD_TO_QA_DROP_STATUS_TBL = """
        UPDATE databricks_metadata_qa.databricks_drop_table_status
        SET table_name = :tableName, status="queued"
        WHERE version = :version AND change_set_id = :changeSetId
        """;
    public static final String OBSOLETE_FORCE_DROP_TABLES_RECORD_TO_QA_DROP_STATUS_TBL = """
        UPDATE databricks_metadata_qa.databricks_drop_table_status
        SET status = "obsolete"
        WHERE version = :version AND change_set_id = :changeSetId
        """;

    public static final String INSERT_FORCE_DROP_TABLES_RECORD_TO_PROD_DROP_STATUS_TBL = """
        INSERT INTO
        databricks_metadata_prod.databricks_drop_table_status
        (table_name, status, version, force_delete, change_set_id)
        VALUES
        (:tableName, "queued",:version,true,:changeSetId)
        """;

    public static final String UPDATE_FORCE_DROP_TABLES_RECORD_TO_PROD_DROP_STATUS_TBL = """
        UPDATE databricks_metadata_prod.databricks_drop_table_status
        SET table_name = :tableName, status="queued"
        WHERE version = :version AND change_set_id = :changeSetId
        """;

    public static final String OBSOLETE_FORCE_DROP_TABLES_RECORD_TO_PROD_DROP_STATUS_TBL = """
        UPDATE databricks_metadata_prod.databricks_drop_table_status
        SET status = "obsolete"
        WHERE version = :version AND change_set_id = :changeSetId
        """;


    public static final String SELECT_SCHEMA_FROM_INFORMATION_SCHEMA = """
        SELECT *
        FROM {catalog}.INFORMATION_SCHEMA.schemata
        where schema_name='{schema}';
        """;
  }

}
