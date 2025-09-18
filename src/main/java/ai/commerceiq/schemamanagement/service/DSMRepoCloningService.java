package ai.commerceiq.schemamanagement.service;


import ai.commerceiq.schemamanagement.utils.GitHelperFunctionUtils;
import ai.commerceiq.schemamanagement.utils.StringConstants;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for cloning the Databricks Schema Management repository.
 * Note: This service performs full repository cloning and is now used as a fallback
 * when the optimized PR diff approach (PRDiffService) is not available or fails.
 * For better performance, the validation pipeline preferentially uses PRDiffService
 * which processes only changed files instead of cloning the entire repository.
 */
@Service
@Slf4j
public class DSMRepoCloningService {

  @Autowired
  GitHelperFunctionUtils gitHelperFunctionUtils;

  public Map<Path, String> cloneRepoAndGenerateFileChecksums(String branchName) {
    log.info("Inside cloneRepoAndGenerateFileChecksums");
    Path repositoryPath = gitHelperFunctionUtils.gitCloneRepoAndReturnRepoPath(branchName)
        .resolve(StringConstants.DSM_FOLDER_PATH);
    Map<Path, String> checksumMap = new HashMap<>();
    try {
      Files.walk(repositoryPath, Integer.MAX_VALUE, FileVisitOption.FOLLOW_LINKS)
          .filter(Files::isRegularFile).forEach(filePath -> {
            String checksum = generateChecksum(filePath);
            checksumMap.put(filePath, checksum);
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    log.info("cloneRepoAndGenerateFileChecksums checksumMap:: {} ", checksumMap);
    return checksumMap;
  }

  private String generateChecksum(Path filePath) {
    log.debug("Inside generateChecksum for : {} ", filePath.toString());
    String checksum = "";
    try {
      checksum = DigestUtils.md5Hex(new FileInputStream(filePath.toFile()));
    } catch (IOException e) {
      log.error("Error:: {}", e.getLocalizedMessage());
    }
    return checksum;
  }

  public Map<String, Boolean> cloneRepoAndGetFiles(String branchName) {
    log.info("Inside cloneRepoAndGetFiles");
    Path repositoryPath = gitHelperFunctionUtils.gitCloneRepoAndReturnRepoPath(branchName)
        .resolve(StringConstants.DSM_FOLDER_PATH);
    Map<String, Boolean> filePathsMap = new HashMap<>();
    try {
      Files.walk(repositoryPath, Integer.MAX_VALUE, FileVisitOption.FOLLOW_LINKS)
          .filter(Files::isRegularFile).forEach(filePath -> {
            filePathsMap.put(filePath.toString(), true);
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    log.info("cloneRepoAndGetFiles FilePath Mao:: {} ", filePathsMap);
    return filePathsMap;
  }
}
