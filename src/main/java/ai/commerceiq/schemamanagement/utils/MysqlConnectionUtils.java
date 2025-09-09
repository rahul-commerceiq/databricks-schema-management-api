package ai.commerceiq.schemamanagement.utils;

import ai.commerceiq.schemamanagement.model.entity.DropTableStatus;
import ai.commerceiq.schemamanagement.model.entity.FileMetaData;
import ai.commerceiq.schemamanagement.model.entity.ValidationMetaData;
import ai.commerceiq.schemamanagement.model.response.DropTableQueryDetailsModel;
import ai.commerceiq.schemamanagement.model.response.FileValidationResultModel;
import ai.commerceiq.schemamanagement.model.response.QueryDetailsModel;
import ai.commerceiq.schemamanagement.repository.DropTableStatusRepository;
import ai.commerceiq.schemamanagement.repository.FileMetaDataRepository;
import ai.commerceiq.schemamanagement.repository.MigrationStatusRepository;
import ai.commerceiq.schemamanagement.repository.ValidationMetaDataRepository;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MysqlConnectionUtils {

  @Autowired
  private DataSource dataSource;
  @Autowired
  private FileMetaDataRepository fileMetadataRepository;
  @Autowired
  private MigrationStatusRepository migrationStatusBetaRepository;
  @Autowired
  private DropTableStatusRepository dropTableStatusRepository;
  @Autowired
  private ValidationMetaDataRepository validationMetaDataRepository;

  /**
   * returns the FileMetaData object based on the two arguments, if any.
   *
   * @param fileValidationDetails The FileValidationResultModel containing details of the file.
   * @param existingRecord        Optional<FileMetaData> representing the existing record, if
   *                              present.
   * @return The FileMetaData object for the file.
   */
  private static FileMetaData constructFileMetaDataObject(
      FileValidationResultModel fileValidationDetails, Optional<FileMetaData> existingRecord,
      String userName) {
    log.info("Inside getFileMetaData");
    FileMetaData newFileMetaData = new FileMetaData();
    //Updates the checksum of the file metadata if existingRecord is not null
    if (existingRecord.isPresent()) {
      newFileMetaData = existingRecord.get();
      newFileMetaData.setUpdated_by(userName);
      newFileMetaData.setChecksum(fileValidationDetails.getChecksum());

    } else {
      newFileMetaData.setAbsoluteFilePath(fileValidationDetails.getFilePath());
      newFileMetaData.setFileName(fileValidationDetails.getFileName());
      newFileMetaData.setChecksum(fileValidationDetails.getChecksum());
      newFileMetaData.setCreated_by(userName);
      newFileMetaData.setUpdated_by(userName);
    }
    log.info("file metadata:: {}", newFileMetaData);
    return newFileMetaData;
  }


  /**
   * Retrieves metadata for migrated files from the database. 1st step of validation
   *
   * @return A Map<Path, String> where Path is the file path and String is the checksum.
   * @throws RuntimeException if an error occurs while getting the checksum of migrated files.
   */
  public Map<Path, String> getMigratedFilesMetaData() {
    log.info("Inside getMigratedFilesMetaData");
    Map<Path, String> databaseFileChecksumMap = new HashMap<>();
    try {
      List<Object[]> result = fileMetadataRepository.getMigratedFilesChecksum();

      for (Object[] row : result) {
        String checksum = (String) row[0];
        String absoluteFilePath = (String) row[1];

        Path filePath = Paths.get(absoluteFilePath);
        databaseFileChecksumMap.put(filePath, checksum);
        log.debug("File name:: {}", filePath);
      }

      log.debug("Checksum from database:: {}", databaseFileChecksumMap);
      return databaseFileChecksumMap;
    } catch (Exception e) {
      log.error("Error while getting checksum of migrated files:: {}", e.getMessage(), e);
      throw new RuntimeException("Error while getting checksum of migrated files", e);
    }
  }

  /**
   * Retrieves metadata for validated but not migrated files from the database, this is to check if
   * there are any modifications to the file that is already validated but not migrated. 2nd step of
   * validation
   *
   * @return A Map<Path, String> where Path is the file path and String is the checksum.
   */
  public Map<Path, String> filterValidatedNonMigratedFiles(List<String> filePaths) {
    log.info("Inside filterValidatedNonMigratedFiles");

    Map<Path, String> databaseFileChecksumMap = new HashMap<>();
    List<Object[]> result = fileMetadataRepository.getValidatedNonMigratedFilesChecksum(filePaths);

    for (Object[] row : result) {
      String absoluteFilePath = (String) row[0];
      String checksum = (String) row[1];

      Path filePath = Paths.get(absoluteFilePath);
      databaseFileChecksumMap.put(filePath, checksum);

      log.info("File name:: {}", filePath);
    }

    log.info("Checksum from database:: {}", databaseFileChecksumMap);
    return databaseFileChecksumMap;
  }

  /**
   * Updates MetaData tables(File metadata and Validation metadata table) for the successfully
   * validated files only.
   */
  public void insertOrUpdateFileMetadata(List<FileValidationResultModel> filesValidationDetails,
      String userName) {
    log.info("Inside insertOrUpdateFileMetadata");

    for (FileValidationResultModel fileValidationDetails : filesValidationDetails) {
      String filePath = fileValidationDetails.getFilePath();

      Optional<FileMetaData> existingRecord = fileMetadataRepository.findByAbsoluteFilePath(
          filePath);
      log.info("Existing Record:: {}", existingRecord);

      FileMetaData updatedFileMetaData = updateOrCreateFileMetaData(fileValidationDetails,
          existingRecord, userName);
      log.info("Updated File Metadata:: {}", updatedFileMetaData);

      ValidationMetaData fileQueriesValidationMetaData = createValidationMetaData(
          fileValidationDetails, updatedFileMetaData);
      log.info("Validation Metadata Record:: {}", fileQueriesValidationMetaData);

      saveValidationMetaData(fileQueriesValidationMetaData);

      if (fileValidationDetails.getFileDdlType().equalsIgnoreCase(StringConstants.DROP_TABLE_DDL)) {
        checkAndUpdateForceDeleteMetaData(fileValidationDetails.getQueryList(),
            fileQueriesValidationMetaData.getVersion());
      }
    }

  }

  private FileMetaData updateOrCreateFileMetaData(FileValidationResultModel validationDetails,
      Optional<FileMetaData> existingRecord, String userName) {
    FileMetaData fileMetaData = constructFileMetaDataObject(validationDetails, existingRecord,
        userName);
    return fileMetadataRepository.save(fileMetaData);
  }

  private ValidationMetaData createValidationMetaData(FileValidationResultModel validationDetails,
      FileMetaData updatedFileMetaData) {
    return new ValidationMetaData(validationDetails.getQueryList(),
        updatedFileMetaData.getVersion());
  }

  private void saveValidationMetaData(ValidationMetaData validationMetaData) {
    validationMetaDataRepository.save(validationMetaData);
  }

  private void checkAndUpdateForceDeleteMetaData(
      List<QueryDetailsModel> dropTableQueryDetailsModels, int version) {
    log.info("Inside checkAndUpdateForceDeleteMetaData");

    for (QueryDetailsModel queryDetailsModel : dropTableQueryDetailsModels) {
      DropTableQueryDetailsModel dropTableQueryDetailsModel = (DropTableQueryDetailsModel) queryDetailsModel;
      if (dropTableQueryDetailsModel.isForceDrop()) {
        String tableName = CommonUtils.getDropTableName(dropTableQueryDetailsModel.getQuery());
        updateBetaForceDropTableStatus(tableName, version, dropTableQueryDetailsModel);
        updateQaForceDropTableStatus(tableName, version, dropTableQueryDetailsModel);
        updateProdForceDropTableStatus(tableName, version, dropTableQueryDetailsModel);

      } else {
        //Delete if drop table status was added in previous validation
        obsoleteRegisteredForceDropTableRecord(version, dropTableQueryDetailsModel.getId());

      }
    }
  }


  private void updateBetaForceDropTableStatus(String tableName, int version,
      DropTableQueryDetailsModel dropTableQueryDetailsModel) {
    log.info("Inside updateBetaForceDropTableStatus");
    DropTableStatus dropTableStatusModel;
    dropTableStatusModel = dropTableStatusRepository.checkForceDeleteForDroppedTableEntryBeta(
        version, dropTableQueryDetailsModel.getId());
    if (dropTableStatusModel != null) {
      dropTableStatusModel.setTableName(tableName);
      dropTableStatusModel.setStatus(StringConstants.DROP_TABLE_STATUS_QUEUED);
    } else {
      dropTableStatusModel = new DropTableStatus(tableName, version,
          dropTableQueryDetailsModel.getId(), dropTableQueryDetailsModel.isForceDrop());
    }
    dropTableStatusRepository.save(dropTableStatusModel);
  }

  private void updateQaForceDropTableStatus(String tableName, int version,
      DropTableQueryDetailsModel dropTableQueryDetailsModel) {
    log.info("Inside updateQaForceDropTableStatus");
    DropTableStatus dropTableStatusModel;
    dropTableStatusModel = dropTableStatusRepository.checkForceDeleteForDroppedTableEntryQa(version,
        dropTableQueryDetailsModel.getId());
    if (dropTableStatusModel != null) {
      dropTableStatusRepository.updateQAForceDropStatusRecord(tableName, version,
          dropTableQueryDetailsModel.getId());
    } else {
      dropTableStatusRepository.insertQAForceDropStatusRecord(tableName, version,
          dropTableQueryDetailsModel.getId());
    }

  }

  private void updateProdForceDropTableStatus(String tableName, int version,
      DropTableQueryDetailsModel dropTableQueryDetailsModel) {
    log.info("Inside updateProdForceDropTableStatus");
    DropTableStatus dropTableStatusModel;
    dropTableStatusModel = dropTableStatusRepository.checkForceDeleteForDroppedTableEntryProd(
        version, dropTableQueryDetailsModel.getId());
    if (dropTableStatusModel != null) {
      dropTableStatusRepository.updateProdForceDropStatusRecord(tableName, version,
          dropTableQueryDetailsModel.getId());
    } else {
      dropTableStatusRepository.insertProdForceDropStatusRecord(tableName, version,
          dropTableQueryDetailsModel.getId());
    }

  }

  private void obsoleteRegisteredForceDropTableRecord(int version, int changeSetId) {
    log.info("Inside deleteRegisteredForceDropTableRecord");

    //Beta
    DropTableStatus betaDropTableStatusModel = dropTableStatusRepository.checkForceDeleteForDroppedTableEntryBeta(
        version, changeSetId);
    if (betaDropTableStatusModel != null) {
      betaDropTableStatusModel.setStatus(StringConstants.DROP_TABLE_STATUS_OBSOLETE);
      dropTableStatusRepository.save(betaDropTableStatusModel);
    }

    //Qa
    DropTableStatus qaDropTableStatusModel = dropTableStatusRepository.checkForceDeleteForDroppedTableEntryQa(
        version,
        changeSetId);
    if (qaDropTableStatusModel != null) {
      dropTableStatusRepository.obsoleteQAForceDropStatusRecord(version, changeSetId);
    }

    //Prod
    DropTableStatus prodDropTableStatusModel = dropTableStatusRepository.checkForceDeleteForDroppedTableEntryProd(
        version,
        changeSetId);
    if (prodDropTableStatusModel != null) {
      dropTableStatusRepository.obsoleteProdForceDropStatusRecord(version, changeSetId);
    }

  }


}
