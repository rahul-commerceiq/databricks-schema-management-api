package ai.commerceiq.schemamanagement.service;

import ai.commerceiq.schemamanagement.model.entity.FilePathAndChecksumEntity;
import ai.commerceiq.schemamanagement.utils.GitDiffHelperUtils;
import ai.commerceiq.schemamanagement.utils.StringConstants;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for handling Pull Request diff-based file operations.
 * This service optimizes the validation pipeline by processing only files that changed in the PR
 * instead of cloning the entire repository.
 */
@Service
@Slf4j
public class PRDiffService {

  @Autowired
  private GitDiffHelperUtils gitDiffHelperUtils;

  /**
   * Retrieves changed files from a Pull Request and generates their checksums.
   * This method replaces the full repository cloning approach with a diff-based approach.
   *
   * @param pullRequestId The ID of the pull request
   * @param sourceBranch  The source branch of the pull request
   * @return A map of file paths and their checksums for only the changed files
   */
  public Map<Path, String> getChangedFilesWithChecksums(String pullRequestId, String sourceBranch) {
    log.info("Inside getChangedFilesWithChecksums for PR: {} from branch: {}", pullRequestId, sourceBranch);
    
    try {
      // Get list of changed files in the PR
      List<String> changedFiles = gitDiffHelperUtils.getChangedFilesInPR(pullRequestId);
      
      // Filter for only schema management files (YAML files in resources/db_migration)
      List<String> schemaFiles = filterSchemaManagementFiles(changedFiles);
      
      if (schemaFiles.isEmpty()) {
        log.info("No schema management files changed in PR: {}", pullRequestId);
        return new HashMap<>();
      }
      
      log.info("Found {} schema management files changed in PR: {}", schemaFiles.size(), pullRequestId);
      
      // Download files and generate checksums
      Map<Path, String> fileChecksumMap = new HashMap<>();
      for (String filePath : schemaFiles) {
        try {
          // Download file content from the source branch
          byte[] fileContent = gitDiffHelperUtils.downloadFileContent(filePath, sourceBranch);
          
          // Generate checksum from file content
          String checksum = DigestUtils.md5Hex(fileContent);
          
          // Create local path for the file (maintain directory structure)
          Path localPath = createLocalPath(filePath);
          
          // Save file locally for processing
          saveFileLocally(localPath, fileContent);
          
          fileChecksumMap.put(localPath, checksum);
          log.debug("Processed file: {} with checksum: {}", filePath, checksum);
          
        } catch (Exception e) {
          log.error("Error processing file {}: {}", filePath, e.getMessage());
          // Continue with other files even if one fails
        }
      }
      
      log.info("Successfully processed {} files from PR diff", fileChecksumMap.size());
      return fileChecksumMap;
      
    } catch (Exception e) {
      log.error("Error retrieving changed files from PR {}: {}", pullRequestId, e.getMessage());
      throw new RuntimeException("Failed to retrieve changed files from PR", e);
    }
  }

  /**
   * Filters the list of changed files to include only schema management YAML files.
   *
   * @param changedFiles List of all changed files in the PR
   * @return List of schema management files (in resources/db_migration folder)
   */
  private List<String> filterSchemaManagementFiles(List<String> changedFiles) {
    return changedFiles.stream()
        .filter(file -> file.startsWith("resources/db_migration/"))
        .filter(file -> file.endsWith(".yaml") || file.endsWith(".yml"))
        .toList();
  }

  /**
   * Creates a local path for the file, maintaining the directory structure.
   *
   * @param remotePath The remote file path
   * @return Local path for the file
   */
  private Path createLocalPath(String remotePath) {
    // Create local path: databricks-schema-management/resources/db_migration/...
    Path basePath = Paths.get(StringConstants.VERSION_MIGRATION_REPOSITORY_NAME);
    return basePath.resolve(remotePath);
  }

  /**
   * Saves file content locally to maintain compatibility with existing processing logic.
   *
   * @param localPath   The local path where the file should be saved
   * @param fileContent The content of the file
   * @throws IOException If there's an error saving the file
   */
  private void saveFileLocally(Path localPath, byte[] fileContent) throws IOException {
    // Create parent directories if they don't exist
    Files.createDirectories(localPath.getParent());
    
    // Write file content to local path
    Files.write(localPath, fileContent);
    
    log.debug("Saved file locally at: {}", localPath.toAbsolutePath());
  }

  /**
   * Alternative method for backward compatibility - works with branch names instead of PR IDs.
   * This method can be used when PR context is not available but we still want to optimize
   * by comparing against a base branch.
   *
   * @param sourceBranch The source branch name
   * @param baseBranch   The base branch to compare against (typically 'master' or 'main')
   * @return A map of file paths and their checksums for changed files
   */
  public Map<Path, String> getChangedFilesWithChecksumsFromBranch(String sourceBranch, String baseBranch) {
    log.info("Inside getChangedFilesWithChecksumsFromBranch - source: {}, base: {}", sourceBranch, baseBranch);
    
    try {
      // Get list of changed files between branches
      List<String> changedFiles = gitDiffHelperUtils.getChangedFilesBetweenBranches(sourceBranch, baseBranch);
      
      // Filter for only schema management files
      List<String> schemaFiles = filterSchemaManagementFiles(changedFiles);
      
      if (schemaFiles.isEmpty()) {
        log.info("No schema management files changed between {} and {}", sourceBranch, baseBranch);
        return new HashMap<>();
      }
      
      log.info("Found {} schema management files changed between branches", schemaFiles.size());
      
      // Download files and generate checksums
      Map<Path, String> fileChecksumMap = new HashMap<>();
      for (String filePath : schemaFiles) {
        try {
          // Download file content from the source branch
          byte[] fileContent = gitDiffHelperUtils.downloadFileContent(filePath, sourceBranch);
          
          // Generate checksum from file content
          String checksum = DigestUtils.md5Hex(fileContent);
          
          // Create local path for the file
          Path localPath = createLocalPath(filePath);
          
          // Save file locally for processing
          saveFileLocally(localPath, fileContent);
          
          fileChecksumMap.put(localPath, checksum);
          log.debug("Processed file: {} with checksum: {}", filePath, checksum);
          
        } catch (Exception e) {
          log.error("Error processing file {}: {}", filePath, e.getMessage());
          // Continue with other files even if one fails
        }
      }
      
      log.info("Successfully processed {} files from branch diff", fileChecksumMap.size());
      return fileChecksumMap;
      
    } catch (Exception e) {
      log.error("Error retrieving changed files between branches {} and {}: {}", sourceBranch, baseBranch, e.getMessage());
      throw new RuntimeException("Failed to retrieve changed files from branch comparison", e);
    }
  }
}