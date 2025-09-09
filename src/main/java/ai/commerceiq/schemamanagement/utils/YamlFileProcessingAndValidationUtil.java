package ai.commerceiq.schemamanagement.utils;

import ai.commerceiq.schemamanagement.model.ddl.AlterTableChangeSet;
import ai.commerceiq.schemamanagement.model.ddl.CreateFunctionChangeSet;
import ai.commerceiq.schemamanagement.model.ddl.CreateSchemaChangeSet;
import ai.commerceiq.schemamanagement.model.ddl.CreateTableChangeSet;
import ai.commerceiq.schemamanagement.model.ddl.CreateViewChangeSet;
import ai.commerceiq.schemamanagement.model.ddl.DdlStatementModel;
import ai.commerceiq.schemamanagement.model.ddl.DropFunctionChangeSet;
import ai.commerceiq.schemamanagement.model.ddl.DropTableChangeSet;
import ai.commerceiq.schemamanagement.model.ddl.DropViewChangeSet;
import ai.commerceiq.schemamanagement.model.general.FilePathAndChecksumEntity;
import ai.commerceiq.schemamanagement.model.response.FileValidationResultModel;
import ai.commerceiq.schemamanagement.model.response.QueryDetailsModel;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

@Slf4j
@Service
public class YamlFileProcessingAndValidationUtil {

  private final Yaml yaml = new Yaml();
  @Autowired
  private QueryGeneratorUtils queryGeneratorUtils;

  public List<FileValidationResultModel> processAndValidateFiles(
      List<FilePathAndChecksumEntity> filesList) {
    log.info("Inside processAndValidateFiles");
    List<FileValidationResultModel> filesValidationResultList = new ArrayList<>();
    filesList.forEach(filePathAndChecksumEntity -> {
      try {
        FileValidationResultModel fileValidationResult = processYamlFile(filePathAndChecksumEntity);
        filesValidationResultList.add(fileValidationResult);
      } catch (FileNotFoundException e) {
        log.error("Exception while reading file:: {}", e.getMessage());
        throw new RuntimeException(e);
      }
    });
    return filesValidationResultList;
  }

  private FileValidationResultModel processYamlFile(
      FilePathAndChecksumEntity filePathAndChecksumEntity)
      throws FileNotFoundException {
    log.info("Inside processYamlFile");
    FileValidationResultModel fileValidationResult = new FileValidationResultModel(
        filePathAndChecksumEntity);

    List<QueryDetailsModel> queryList = null;
    Object document = null;

    InputStream inputStream = new FileInputStream(
        filePathAndChecksumEntity.getFilePath().toString());
    Iterator<Object> iterator = yaml.loadAll(inputStream).iterator();

    if (iterator.hasNext()) {
      document = iterator.next();
      if (document instanceof Map) {
        queryList = processYamlDocument((Map<?, ?>) document);
      }
    }
    if (queryList == null) {
      fileValidationResult.setResult(StringConstants.FAILED);
      fileValidationResult.setError(StringConstants.INVALID_DDL_OPERATION);
    } else {
      fileValidationResult.setQueryList(queryList);
      String ddlType = (String) ((Map<?, ?>) document).get("DDLType");
      fileValidationResult.setFileDdlType(ddlType);
    }
    return fileValidationResult;
  }

  private List<QueryDetailsModel> processYamlDocument(Map<?, ?> document) {
    log.info("Inside processDocument");
    boolean isInvalidDdl = false;
    String ddlType = (String) document.get("DDLType");
    log.info("DDL type:: {}", ddlType);
    DdlStatementModel ddlObject = null;
    switch (ddlType) {
      case StringConstants.CREATE_TABLE_DDL ->
          ddlObject = yaml.loadAs(yaml.dump(document), CreateTableChangeSet.class);
      case StringConstants.DROP_TABLE_DDL ->
          ddlObject = yaml.loadAs(yaml.dump(document), DropTableChangeSet.class);
      case StringConstants.CREATE_VIEW_DDL, StringConstants.UPDATE_VIEW_DDL ->
          ddlObject = yaml.loadAs(yaml.dump(document), CreateViewChangeSet.class);
      case StringConstants.DROP_VIEW_DDL ->
          ddlObject = yaml.loadAs(yaml.dump(document), DropViewChangeSet.class);
      case StringConstants.ALTER_TABLE_DDL ->
          ddlObject = yaml.loadAs(yaml.dump(document), AlterTableChangeSet.class);
      case StringConstants.CREATE_SCHEMA_DDL ->
          ddlObject = yaml.loadAs(yaml.dump(document), CreateSchemaChangeSet.class);
      case StringConstants.CREATE_FUNCTION_DDL, StringConstants.UPDATE_FUNCTION_DDL ->
          ddlObject = yaml.loadAs(yaml.dump(document), CreateFunctionChangeSet.class);
      case StringConstants.DROP_FUNCTION_DDL ->
          ddlObject = yaml.loadAs(yaml.dump(document), DropFunctionChangeSet.class);
      default -> isInvalidDdl = true;
    }
    if (isInvalidDdl) {
      log.info("Invalid DDL type:: {}", ddlType);
      return null;
    }
    log.info("DDL object:: {}", ddlObject);
    return queryGeneratorUtils.generateAndValidateQuery(ddlObject);
  }
}
