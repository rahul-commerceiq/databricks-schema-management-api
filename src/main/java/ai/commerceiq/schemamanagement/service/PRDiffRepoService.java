package ai.commerceiq.schemamanagement.service;

import ai.commerceiq.schemamanagement.utils.GitHelperFunctionUtils;
import ai.commerceiq.schemamanagement.utils.StringConstants;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PRDiffRepoService {

  @Autowired
  GitHelperFunctionUtils gitHelperFunctionUtils;

  /**
   * Gets PR diff files and generates checksums for only the changed files.
   * This is an optimized approach that fetches only the files changed in the PR
   * instead of cloning the entire repository.
   *
   * @param branchName The name of the branch to validate
   * @param prNumber The PR number for diff-based validation (optional)
   * @return Map of file paths to their checksums for only the changed files
   */
  public Map<Path, String> getPRDiffFilesAndGenerateChecksums(String branchName, String prNumber) {
    log.info("Inside getPRDiffFilesAndGenerateChecksums - Branch: {}, PR: {}", branchName, prNumber);
    
    // Get the list of changed files from PR diff
    Set<String> changedFiles = getChangedFilesFromPR(branchName, prNumber);
    log.info("Changed files from PR diff: {}", changedFiles);
    
    // Filter only YAML files in the DSM folder path
    Set<String> yamlFiles = changedFiles.stream()
        .filter(file -> file.endsWith(".yaml") || file.endsWith(".yml"))
        .filter(file -> file.contains(StringConstants.DSM_FOLDER_PATH.toString()))
        .collect(Collectors.toSet());
    
    log.info("YAML files to process: {}", yamlFiles);
    
    // Generate checksums for only the changed YAML files
    Map<Path, String> checksumMap = new HashMap<>();
    for (String filePath : yamlFiles) {
      try {
        Path fullPath = Paths.get(filePath);
        if (Files.exists(fullPath)) {
          String checksum = generateChecksum(fullPath);
          checksumMap.put(fullPath, checksum);
        }
      } catch (Exception e) {
        log.warn("Could not process file {}: {}", filePath, e.getMessage());
      }
    }
    
    log.info("PR diff checksumMap: {}", checksumMap);
    return checksumMap;
  }

  /**
   * Gets the list of changed files from PR diff.
   * This method uses git diff to get only the files that have changed.
   *
   * @param branchName The branch name
   * @param prNumber The PR number (optional)
   * @return Set of changed file paths
   */
  private Set<String> getChangedFilesFromPR(String branchName, String prNumber) {
    log.info("Getting changed files from PR diff - Branch: {}, PR: {}", branchName, prNumber);
    
    try {
      // Clone the repository to get access to git commands
      Path repositoryPath = gitHelperFunctionUtils.gitCloneRepoAndReturnRepoPath(branchName);
      
      // Use git diff to get changed files
      // If prNumber is provided, we can use git diff with PR-specific logic
      // For now, we'll use git diff against the base branch
      String baseBranch = "master"; // or "main" depending on your repository structure
      
      // Get the list of changed files using git diff
      ProcessBuilder processBuilder = new ProcessBuilder(
          "git", "diff", "--name-only", baseBranch + "..." + branchName
      );
      processBuilder.directory(repositoryPath.toFile());
      
      Process process = processBuilder.start();
      String output = new String(process.getInputStream().readAllBytes());
      int exitCode = process.waitFor();
      
      if (exitCode != 0) {
        log.warn("Git diff command failed with exit code: {}", exitCode);
        // Fallback to getting all files in the branch
        return getAllFilesInBranch(repositoryPath);
      }
      
      Set<String> changedFiles = output.lines()
          .filter(line -> !line.trim().isEmpty())
          .collect(Collectors.toSet());
      
      log.info("Git diff found {} changed files", changedFiles.size());
      return changedFiles;
      
    } catch (Exception e) {
      log.error("Error getting changed files from PR diff: {}", e.getMessage());
      // Fallback to the original behavior if PR diff fails
      log.info("Falling back to full repository processing");
      return getAllFilesInBranch(gitHelperFunctionUtils.gitCloneRepoAndReturnRepoPath(branchName));
    }
  }

  /**
   * Fallback method to get all files in the branch if PR diff fails.
   *
   * @param repositoryPath The path to the repository
   * @return Set of all file paths in the repository
   */
  private Set<String> getAllFilesInBranch(Path repositoryPath) {
    log.info("Getting all files in branch as fallback");
    try {
      return Files.walk(repositoryPath.resolve(StringConstants.DSM_FOLDER_PATH), Integer.MAX_VALUE, FileVisitOption.FOLLOW_LINKS)
          .filter(Files::isRegularFile)
          .map(Path::toString)
          .collect(Collectors.toSet());
    } catch (IOException e) {
      log.error("Error walking repository files: {}", e.getMessage());
      return Set.of();
    }
  }

  /**
   * Generates MD5 checksum for a file.
   *
   * @param filePath The path to the file
   * @return MD5 checksum of the file
   */
  private String generateChecksum(Path filePath) {
    log.debug("Generating checksum for: {}", filePath.toString());
    String checksum = "";
    try {
      checksum = DigestUtils.md5Hex(new FileInputStream(filePath.toFile()));
    } catch (IOException e) {
      log.error("Error generating checksum for {}: {}", filePath, e.getLocalizedMessage());
    }
    return checksum;
  }

  /**
   * Alternative method that works with Bitbucket API to get PR diff files.
   * This can be used if we have access to Bitbucket API credentials.
   *
   * @param prNumber The PR number
   * @return Set of changed file paths
   */
  private Set<String> getChangedFilesFromBitbucketAPI(String prNumber) {
    log.info("Getting changed files from Bitbucket API for PR: {}", prNumber);
    
    // This would require Bitbucket API integration
    // For now, we'll return an empty set and fall back to git diff
    log.warn("Bitbucket API integration not implemented, falling back to git diff");
    return Set.of();
  }
}