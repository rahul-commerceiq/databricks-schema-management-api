package ai.commerceiq.schemamanagement.utils;

import ai.commerceiq.schemamanagement.exception.InValidFileNameException;
import ai.commerceiq.schemamanagement.model.general.FilePathAndChecksumEntity;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CommonUtils {


  @Value("${databricks.s3.location}")
  private String location;

  /**
   * Extracts the table name from a DROP TABLE SQL query.
   *
   * @param query The DROP TABLE SQL query from which the table name is to be extracted.
   * @return The extracted table name from the DROP TABLE SQL query.
   */
  public static String getDropTableName(String query) {
    String[] queryParts = query.split("\\s+");
    return queryParts[2].replaceAll(StringConstants.SEMICOLON, StringConstants.EMPTY_STRING);
  }

  /**
   * Extracts specific components (catalog, schema, table name, or schema and table name combined)
   * from a given full table name.
   *
   * @param fullTableName The full table name from which components are to be extracted.
   * @param component     The specific component to extract ("catalog," "schema," "table_name," or
   *                      "schemaAndTableName").
   * @return The extracted component value as a String.
   */
  public static String getTableComponents(String fullTableName, String component) {
    log.info("Inside getSchemaAndTableName");
    String[] components = fullTableName.split("\\.");
    String value = switch (component) {
      case StringConstants.CATALOG -> components[0];
      case StringConstants.SCHEMA -> components[1];
      case StringConstants.TABLE_NAME -> components[2];
      case "schemaAndTableName" -> components[1] + "." + components[2];
      default -> null;
    };
    log.info("Table component {} value is:: {}", component, value);
    return value;

  }

  /**
   * Checks if a dynamic view needs to be created based on the given full table name.
   *
   * @param fullTableName The full table name including the catalog, schema, and table name.
   * @return The full view table name if a dynamic view needs to be created, or null otherwise.
   */
  public static String getDynamicViewName(String fullTableName) {
    String catalog = getTableComponents(fullTableName, StringConstants.CATALOG);
    String viewCatalog = catalog.equalsIgnoreCase(StringConstants.CLIENT_CATALOG)
        ? StringConstants.CLIENT_VIEW_CATALOG : StringConstants.COMMON_VIEW_CATALOG;
    String fullViewName =
        viewCatalog + "." + getTableComponents(fullTableName, "schemaAndTableName");
    log.info("View name:: {}", fullViewName);
    return fullViewName;
  }

  /**
   * Extracts the full table name from a CREATE TABLE SQL query or its EXPLAIN variant.
   *
   * @param query The SQL query from which the full table name is to be extracted.
   * @return The full table name extracted from the SQL query.
   */
  public static String getTableNameFromCreateTableQuery(String query) {
    String[] splitWords = query.split("\\s+");
    String fullTableName = splitWords[2];
    if (splitWords[0].equals("EXPLAIN")) {
      fullTableName = splitWords[3];
    }
    log.info("Full table name:: {}", fullTableName);
    return fullTableName;
  }

  public static String getTableNameFromAlterTableQuery(String query) {
    String[] splitWords = query.split("\\s+");
    String fullTableName = splitWords[2];
    if (splitWords[0].equals("EXPLAIN")) {
      fullTableName = splitWords[3];
    }
    log.info("Full table name:: {}", fullTableName);
    return fullTableName;
  }

  public static String getTableNameFromQuery(String query) {
    log.info("Inside getTableNameFromQuery");

    String queryType = getQueryType(query);
    return switch (queryType) {
      case StringConstants.DROP_TABLE -> getDropTableName(query);
      case StringConstants.CREATE_TABLE -> getTableNameFromCreateTableQuery(query);
      case StringConstants.ALTER_TABLE -> getTableNameFromAlterTableQuery(query);
      default -> {
        log.info(StringConstants.TABLE_NAME_RETRIVAL_ERROR);
        throw new RuntimeException(StringConstants.TABLE_NAME_RETRIVAL_ERROR);
      }
    };
  }

  /**
   * Validates file names based on a specified pattern and date criteria.
   *
   * @param filePathsMap A map containing file paths as keys and associated information as values.
   * @throws InValidFileNameException If any file name in the provided map does not meet the
   *                                  specified criteria.
   */
  public static void validateFilesName(List<FilePathAndChecksumEntity> filePathsMap) {
    log.info("Inside validateFilesName");
    List<String> fileNames = getFileNamesFromFilePathsMap(filePathsMap);
    String pattern = StringConstants.FILE_NAME_PATTERN;
    Pattern regex = Pattern.compile(pattern);
    List<String> inValidFileNamesList = new ArrayList<>();
    for (String fileName : fileNames) {
      Matcher matcher = regex.matcher(fileName);
      // Check if the file name matches the pattern
      if (!matcher.matches()) {
        inValidFileNamesList.add(fileName);
        continue;
      }
      // Extract the date part from the file name
      String datePart = fileName.substring(0, 10);
      // Parse the date and compare with the present day
      try {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd");
        Date fileDate = dateFormat.parse(datePart);
        Date currentDate = new Date();
        if (!(fileDate.before(currentDate) || fileDate.equals(currentDate))) {
          inValidFileNamesList.add(fileName);
        }
      } catch (ParseException e) {
        log.info("Invalid file name::{}", fileName);
        inValidFileNamesList.add(fileName);
      }
    }
    if (!inValidFileNamesList.isEmpty()) {
      log.info("Invalid files name:: {}", inValidFileNamesList);
      throw new InValidFileNameException(inValidFileNamesList);
    }
  }

  /**
   * Extracts file names from a map of file paths, returning a list of file names.
   *
   * @param filePathsList A list containing  FilePathAndChecksum .
   * @return A list of file names extracted from the provided file paths.
   */
  public static List<String> getFileNamesFromFilePathsMap(
      List<FilePathAndChecksumEntity> filePathsList) {
    log.info("Inside getFileNamesFromFilePathsMap");
    return filePathsList.stream().map(FilePathAndChecksumEntity::getFilePath).map(Path::getFileName)
        .map(Path::toString).toList();
  }

  public static String getQueryType(String query) {
    log.info("Inside getQueryType");
    String queryType = "";
    if (query.contains(StringConstants.DROP_TABLE)) {
      queryType = StringConstants.DROP_TABLE;
    } else if (query.contains(StringConstants.CREATE_TABLE)) {
      queryType = StringConstants.CREATE_TABLE;
    } else if (query.contains((StringConstants.ALTER_TABLE))) {
      queryType = StringConstants.ALTER_TABLE;
    } else if (query.contains((StringConstants.CREATE_SCHEMA))) {
      queryType = StringConstants.CREATE_SCHEMA;
    } else if (containsAnySubstringInList(query, StringConstants.CREATE_VIEW_POSSIBLE_SUBSTRINGS)) {
      queryType = StringConstants.CREATE_VIEW;
    } else if (query.contains(StringConstants.DROP_VIEW)) {
      queryType = StringConstants.DROP_VIEW;
    } else if (containsAnySubstringInList(query,
        StringConstants.CREATE_FUNCTION_POSSIBLE_SUBSTRINGS)) {
      return StringConstants.CREATE_FUNCTION;
    } else if (query.contains(StringConstants.DROP_FUNCTION)) {
      queryType = StringConstants.DROP_FUNCTION;
    }
    log.info("Query type:: {}", queryType);
    return queryType;
  }

  public static boolean containsAnySubstringInList(String mainString, List<String> substrings) {
    for (String substring : substrings) {
      if (mainString.contains(substring)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Generates a SELECT query for retrieving table information from the Information Schema based on
   * the input CREATE TABLE query.
   *
   * @param query The CREATE TABLE query for which the SELECT query is being generated.
   * @return The SELECT query for retrieving table information from the Information Schema.
   */
  public String addLocationBasedCreateTableQuery(String query) {
    log.info("Inside addLocationBasedCreateTableQuery");
    String entireTableName = getTableNameFromCreateTableQuery(query);
    String[] splitTableName = entireTableName.split("\\.");

    String s3location = this.location.replace(StringConstants.CATALOG_IDENTIFIER, splitTableName[0])
        .replace(StringConstants.SCHEMA_IDENTIFIER, splitTableName[1])
        .replace(StringConstants.TABLE_NAME_IDENTIFIER, splitTableName[2]);
    query = query.replace(StringConstants.LOCATION_IDENTIFIER, s3location);
    log.info("addLocationBasedCreateTableQuery:: {}", query);
    return query;
  }

}
