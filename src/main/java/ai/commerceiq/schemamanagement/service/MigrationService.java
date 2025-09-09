package ai.commerceiq.schemamanagement.service;

import ai.commerceiq.schemamanagement.exception.EmptyMigrationFilesListException;
import ai.commerceiq.schemamanagement.exception.FilesNotMigratedToLowerEnvException;
import ai.commerceiq.schemamanagement.exception.MigratedFilesForMigrationException;
import ai.commerceiq.schemamanagement.model.entity.ValidationMetaData;
import ai.commerceiq.schemamanagement.model.request.MigrationReqQueryParmsAndBody;
import ai.commerceiq.schemamanagement.model.response.MigrationApiSuccessfulResponse;
import ai.commerceiq.schemamanagement.repository.FileMetaDataRepository;
import ai.commerceiq.schemamanagement.repository.ValidationMetaDataRepository;
import ai.commerceiq.schemamanagement.utils.DatabricksQueryExecutionUtils;
import ai.commerceiq.schemamanagement.utils.MysqlConnectionUtils;
import ai.commerceiq.schemamanagement.utils.StringConstants;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MigrationService {

  @Autowired
  private MysqlConnectionUtils mysqlConnectionUtils;
  @Autowired
  private DatabricksQueryExecutionUtils databricksQueryExecutionUtil;
  @Autowired
  private ValidationMetaDataRepository validationMetaDataRepository;
  @Autowired
  private FileMetaDataRepository fileMetaDataRepository;
  @Value("${spring.profiles.active}")
  private String env;

  public MigrationApiSuccessfulResponse migrateFiles(
      MigrationReqQueryParmsAndBody migrateRequestBody) {
    log.info("Inside MigrateFiles");
    List<ValidationMetaData> validationMetaDataRecords = ValidateMigrateRequestBodyAndGetMigrationQueries(
        migrateRequestBody);
    databricksQueryExecutionUtil.executeMigrationQueries(validationMetaDataRecords,
        migrateRequestBody);
    return constructApiResponse(migrateRequestBody);
  }

  private List<ValidationMetaData> ValidateMigrateRequestBodyAndGetMigrationQueries(
      MigrationReqQueryParmsAndBody migrateRequestBody) {

    log.info("Inside ValidateMigrateRequestBody");

    List<String> filesPathList = migrateRequestBody.getFiles();
    if (filesPathList == null || filesPathList.isEmpty()) {
      throw new EmptyMigrationFilesListException(StringConstants.FILES_PATH_LIST_CANNOT_BE_EMPTY,
          migrateRequestBody);
    }

    List<String> alreadyMigratedFiles = fileMetaDataRepository.findMigratedFilesInList(
        filesPathList);
    if (!alreadyMigratedFiles.isEmpty()) {
      throw new MigratedFilesForMigrationException(alreadyMigratedFiles, migrateRequestBody);
    }

    if (env.equals(StringConstants.QA_ENV)) {
      List<String> filesNotMigratedToBeta = fileMetaDataRepository.findNonBetaMigratedFilesInList(
          filesPathList);
      if (!filesNotMigratedToBeta.isEmpty()) {
        throw new FilesNotMigratedToLowerEnvException(filesNotMigratedToBeta, migrateRequestBody);
      }
    }

    if (env.equals(StringConstants.PROD_ENV)) {
      List<String> filesNotMigratedToBetaOrQa = fileMetaDataRepository.findNonBetaAndQaMigratedFilesInList(
          filesPathList);
      if (!filesNotMigratedToBetaOrQa.isEmpty()) {
        throw new FilesNotMigratedToLowerEnvException(filesNotMigratedToBetaOrQa,
            migrateRequestBody);
      }
    }

    List<ValidationMetaData> validationMetaDataRecords = validationMetaDataRepository.findQueriesAndVersionByAbsoluteFilePaths(
        filesPathList);
    if (validationMetaDataRecords.isEmpty()
        || validationMetaDataRecords.size() != filesPathList.size()) {
      throw new EmptyMigrationFilesListException(StringConstants.MIGRATION_FILES_NOT_FOUND_IN_DB,
          migrateRequestBody);
    }

    log.info("Queries being migrated:: {}", validationMetaDataRecords);
    return validationMetaDataRecords;
  }

  private MigrationApiSuccessfulResponse constructApiResponse(
      MigrationReqQueryParmsAndBody migrateRequest) {
    log.info("Inside constructMigrateApiResponse");
    MigrationApiSuccessfulResponse response = new MigrationApiSuccessfulResponse();
    response.setStatus(StringConstants.SUCCESS);
    response.setTimestamp(LocalDateTime.now());
    response.setJobUrl(migrateRequest.getJobUrl());
    response.setUserName(migrateRequest.getUserName());
    response.setDetails(Collections.singletonList(migrateRequest.getFiles()));
    log.info("MigrationApiSuccessful response:: {}", response);
    return response;
  }

}
