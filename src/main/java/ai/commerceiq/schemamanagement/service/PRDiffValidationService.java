package ai.commerceiq.schemamanagement.service;

import ai.commerceiq.schemamanagement.exception.NoNewFilesFoundException;
import ai.commerceiq.schemamanagement.exception.OldFilesDeletedAndUpdatedException;
import ai.commerceiq.schemamanagement.exception.OldFilesDeletedException;
import ai.commerceiq.schemamanagement.exception.OldFilesUpdatedException;
import ai.commerceiq.schemamanagement.model.general.FilePathAndChecksumEntity;
import ai.commerceiq.schemamanagement.model.response.FileValidationResultModel;
import ai.commerceiq.schemamanagement.model.response.QueryDetailsModel;
import ai.commerceiq.schemamanagement.model.response.ValidateApiResponse;
import ai.commerceiq.schemamanagement.utils.CommonUtils;
import ai.commerceiq.schemamanagement.utils.MysqlConnectionUtils;
import ai.commerceiq.schemamanagement.utils.StringConstants;
import ai.commerceiq.schemamanagement.utils.YamlFileProcessingAndValidationUtil;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PRDiffValidationService {

  @Autowired
  private PRDiffRepoService prDiffRepoService;
  @Autowired
  private MysqlConnectionUtils mysqlConnectionUtils;
  @Autowired
  private YamlFileProcessingAndValidationUtil yamlFileProcessingAndValidationUtil;

  /**
   * Validates migration files using PR diff approach. This optimized process:
   * 1. Fetches only the files changed in the PR diff instead of cloning the full repository
   * 2. Generates and validates checksum for only changed files
   * 3. Validates YAML files for only the changed files
   * 4. Generates and validates SQL queries for only the changed files
   * 5. If validation is successful, stores the generated query and file checksum in the metadata table
   *
   * @param branchName The name of the branch to validate.
   * @param prNumber The PR number for diff-based validation (optional, if null falls back to branch comparison)
   * @return An ApiResponse indicating the result of the validation process.
   */
  public ValidateApiResponse validateFilesWithPRDiff(String branchName, String userName, String prNumber) {
    log.info("Inside validateFilesWithPRDiff - Branch: {}, PR: {}", branchName, prNumber);
    
    Map<Path, String> prDiffFilesChecksum = prDiffRepoService.getPRDiffFilesAndGenerateChecksums(branchName, prNumber);
    List<FilePathAndChecksumEntity> filesToValidate = getFilesToValidate(prDiffFilesChecksum);
    CommonUtils.validateFilesName(filesToValidate);
    List<FileValidationResultModel> validationResult = yamlFileProcessingAndValidationUtil.processAndValidateFiles(
        filesToValidate);
    boolean isOverAllValidationSuccessful = updateMetaData(validationResult, userName);
    ValidateApiResponse response = buildValidationResponse(validationResult, userName,
        isOverAllValidationSuccessful);
    log.info("PR Diff ValidationApi response: {}", response);
    return response;
  }

  /**
   * Builds a ValidationApiResponse based on the provided validation results and the overall
   * validation status.
   *
   * @param validationResults             The list of FileValidationResultModel containing details
   *                                      of each validated file.
   * @param isOverAllValidationSuccessful A boolean indicating whether the final validation was
   *                                      successful or not.
   * @return A ValidateApiResponse with timestamp, details, and status set accordingly.
   */
  ValidateApiResponse buildValidationResponse(
      List<FileValidationResultModel> validationResults, String userName,
      boolean isOverAllValidationSuccessful) {
    ValidateApiResponse response = new ValidateApiResponse();
    response.setTimestamp(LocalDateTime.now());
    response.setValidatedBy(userName);
    response.setDetails(validationResults);
    response.setStatus(
        isOverAllValidationSuccessful ? StringConstants.SUCCESS : StringConstants.FAILED);
    return response;
  }

  /**
   * Retrieves files to validate based on the provided PR diff file checksums.
   *
   * @param prDiffFilesChecksum The map containing file checksums from the PR diff.
   * @return A map of files path and checksum to be validated, filtered based on existing metadata.
   * @throws NoNewFilesFoundException If no new files are found to validate.
   */
  private List<FilePathAndChecksumEntity> getFilesToValidate(
      Map<Path, String> prDiffFilesChecksum) {
    log.info("Inside getFilesToValidate for PR diff files");
    Map<Path, String> allNewFilesMap = getAllNewFilesInBranch(prDiffFilesChecksum);
    List<String> filePaths = allNewFilesMap.keySet().stream().map(Path::toString).toList();
    Map<Path, String> validatedNonMigratedFilesMetaData = mysqlConnectionUtils.filterValidatedNonMigratedFiles(
        filePaths);
    List<FilePathAndChecksumEntity> toBeValidatedFilesList = allNewFilesMap.entrySet().stream()
        .filter(entry -> !validatedNonMigratedFilesMetaData.containsKey(entry.getKey())
            || !entry.getValue().equals(validatedNonMigratedFilesMetaData.get(entry.getKey())))
        .map(entry -> new FilePathAndChecksumEntity(entry.getKey(), entry.getValue())).toList();
    if (toBeValidatedFilesList.isEmpty()) {
      log.info(StringConstants.NO_NEW_FILES_OR_CHANGES_IN_GIT_BRANCH);
      throw new NoNewFilesFoundException(StringConstants.NO_NEW_FILES_OR_CHANGES_IN_GIT_BRANCH);
    }
    log.info("PR Diff Validation files : {}", toBeValidatedFilesList);
    return toBeValidatedFilesList;
  }

  /**
   * Updates metadata based on the validation results of files.
   *
   * @param validationResult The list of FileValidationResultModel containing details of each
   *                         validated file.
   * @return True if any file was successfully validated and metadata was updated, otherwise false.
   */
  private boolean updateMetaData(List<FileValidationResultModel> validationResult,
      String userName) {
    log.info("Inside updateFileAndQueryValidationMetaData for PR diff");

    List<FileValidationResultModel> successfullyValidatedFilesList = new ArrayList<>();
    for (FileValidationResultModel fileValidationResult : validationResult) {
      boolean validationSuccessful = true;
      if (fileValidationResult.getQueryList() == null) {
        validationSuccessful = false;
      } else {
        for (QueryDetailsModel queryValidation : fileValidationResult.getQueryList()) {
          if (queryValidation.getId() <= 0) {
            queryValidation.setError("Change set id is missing/invalid");
            queryValidation.setResult(StringConstants.FAILED);
            validationSuccessful = false;
          } else if (queryValidation.getResult().equals(StringConstants.FAILED)) {
            validationSuccessful = false;
          }
        }
      }
      if (!validationSuccessful) {
        fileValidationResult.setResult(StringConstants.FAILED);
      } else {
        successfullyValidatedFilesList.add(fileValidationResult);
        fileValidationResult.setResult(StringConstants.SUCCESS);
      }
    }
    if (!successfullyValidatedFilesList.isEmpty()) {
      mysqlConnectionUtils.insertOrUpdateFileMetadata(successfullyValidatedFilesList, userName);
    }
    return successfullyValidatedFilesList.size() == validationResult.size();
  }

  /**
   * Retrieves all new files in the branch based on PR diff file checksums and compares them with
   * the existing metadata.
   *
   * @param repoFilesChecksumMap The map containing file checksums from the PR diff.
   * @return A map of new files in the repository.
   * @throws OldFilesDeletedAndUpdatedException If both old modified and deleted files are
   *                                            detected.
   * @throws OldFilesDeletedException           If only old deleted files are detected.
   * @throws OldFilesUpdatedException           If only old modified files are detected.
   * @throws NoNewFilesFoundException           If no new files are found in the branch.
   */
  private Map<Path, String> getAllNewFilesInBranch(Map<Path, String> repoFilesChecksumMap) {
    log.info("Inside getAllNewFilesInBranch for PR diff files");

    Map<Path, String> databaseFilesChecksumMap = mysqlConnectionUtils.getMigratedFilesMetaData();
    int countOfFilesInDB = databaseFilesChecksumMap.size();

    log.info("Number of files registered in metadata tables: {}", countOfFilesInDB);

    List<String> oldModifiedFiles = repoFilesChecksumMap.entrySet().stream().filter(
            entry -> databaseFilesChecksumMap.containsKey(entry.getKey())
                && !databaseFilesChecksumMap.get(entry.getKey()).equals(entry.getValue()))
        .map(entry -> entry.getKey().toString()).collect(Collectors.toList());

    List<String> deletedFilesInRepo = databaseFilesChecksumMap.keySet().stream()
        .filter(key -> !repoFilesChecksumMap.containsKey(key)).map(Path::toString)
        .collect(Collectors.toList());

    Map<Path, String> newFilesInRepo = repoFilesChecksumMap.entrySet().stream()
        .filter(entry -> !databaseFilesChecksumMap.containsKey(entry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    log.info("New files: {}", newFilesInRepo);
    log.info("Old modified files: {}", oldModifiedFiles);
    log.info("Deleted files: {}", deletedFilesInRepo);

    validateFilesStatus(oldModifiedFiles, deletedFilesInRepo);
    if (newFilesInRepo.isEmpty()) {
      throw new NoNewFilesFoundException(StringConstants.NO_NEW_FILES_OR_CHANGES_IN_GIT_BRANCH);
    }
    return newFilesInRepo;
  }

  /**
   * Validates the status of old modified and deleted files and throws exception accordingly.
   *
   * @param oldModifiedFiles   The list of paths of old modified files.
   * @param deletedFilesInRepo The list of paths of deleted files in the repository.
   * @throws OldFilesDeletedAndUpdatedException If both old modified and deleted files are
   *                                            detected.
   * @throws OldFilesDeletedException           If only old deleted files are detected.
   * @throws OldFilesUpdatedException           If only old modified files are detected.
   */
  private void validateFilesStatus(List<String> oldModifiedFiles, List<String> deletedFilesInRepo) {
    if (!deletedFilesInRepo.isEmpty() && !oldModifiedFiles.isEmpty()) {
      throw new OldFilesDeletedAndUpdatedException(deletedFilesInRepo, oldModifiedFiles);
    } else if (!deletedFilesInRepo.isEmpty()) {
      throw new OldFilesDeletedException(deletedFilesInRepo);
    } else if (!oldModifiedFiles.isEmpty()) {
      throw new OldFilesUpdatedException(oldModifiedFiles);
    }
  }
}