package ai.commerceiq.schemamanagement.service;

import ai.commerceiq.schemamanagement.exception.NoNewFilesFoundException;
import ai.commerceiq.schemamanagement.model.response.NonMigratedFilesApiResponse;
import ai.commerceiq.schemamanagement.repository.FileMetaDataRepository;
import ai.commerceiq.schemamanagement.utils.StringConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GetMigrationFilesService {

  @Value("${spring.profiles.active}")
  private String env;
  @Autowired
  private FileMetaDataRepository fileMetaDataRepository;
  @Autowired
  private DSMRepoCloningService dsmRepoCloningService;

  public NonMigratedFilesApiResponse getFilesToMigrate() {
    log.info("Inside MigrationFilesService");
    List<String> filesList = switch (env) {
      case StringConstants.BETA_ENV ->
          fileMetaDataRepository.findFileNamesNotInMigrationStatusBeta();
      case StringConstants.QA_ENV -> fileMetaDataRepository.findFileNamesNotInMigrationStatusQa();
      case StringConstants.PROD_ENV ->
          fileMetaDataRepository.findFileNamesNotInMigrationStatusProd();
      default -> null;
    };
    if (filesList == null || filesList.isEmpty()) {
      throw new NoNewFilesFoundException(StringConstants.NO_FILES_VALIDATED_FOR_MIGRATION);
    }
    List<String> filesPresentInReleaseBranch = getFilesPresentInReleaseBranch(filesList);
    if (filesPresentInReleaseBranch == null || filesPresentInReleaseBranch.isEmpty()) {
      throw new NoNewFilesFoundException(StringConstants.NO_FILES_VALIDATED_FOR_MIGRATION);
    }
    log.info("FilesPresentInReleaseBranch list ::{}", filesPresentInReleaseBranch);
    NonMigratedFilesApiResponse nonMigratedFilesApiResponse = new NonMigratedFilesApiResponse(
        filesPresentInReleaseBranch);
    log.info("NonMigratedApi response:: {}", nonMigratedFilesApiResponse);
    return nonMigratedFilesApiResponse;
  }

  public List<String> getFilesPresentInReleaseBranch(List<String> filesList) {
    log.info("Inside getFilesPresentInReleaseBranch");
    Map<String, Boolean> AllFilesFromReleaseBranch = dsmRepoCloningService.cloneRepoAndGetFiles(
        StringConstants.MASTER_BRANCH);
    List<String> filesPresentInReleaseBranch = new ArrayList<>();
    for (String filePath : filesList) {
      if (AllFilesFromReleaseBranch.containsKey(filePath)) {
        filesPresentInReleaseBranch.add(filePath);
      }
    }
    return filesPresentInReleaseBranch;
  }

}
