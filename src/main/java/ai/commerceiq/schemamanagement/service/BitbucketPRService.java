package ai.commerceiq.schemamanagement.service;

import ai.commerceiq.schemamanagement.model.general.FilePathAndChecksumEntity;
import ai.commerceiq.schemamanagement.model.general.PullRequestDiffFile;
import ai.commerceiq.schemamanagement.utils.StringConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class BitbucketPRService {

  @Value("${bitbucket.workspace:commerceiq}")
  private String bitbucketWorkspace;
  
  @Value("${bitbucket.repository.slug:databricks-schema-management}")
  private String repositorySlug;
  
  @Value("${bitbucket.api.base.url:https://api.bitbucket.org/2.0}")
  private String bitbucketApiBaseUrl;

  @Autowired
  private AWSSecretManagerService awsSecretManagerService;

  private final RestTemplate restTemplate = new RestTemplate();
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Fetches the changed files in a pull request and returns them with checksums.
   * Only files in the DSM_FOLDER_PATH directory are considered.
   *
   * @param pullRequestId The ID of the pull request
   * @return Map of file paths to their checksums for changed files
   */
  public Map<Path, String> getPRDiffFiles(String pullRequestId) {
    log.info("Fetching PR diff files for PR: {}", pullRequestId);
    
    try {
      List<PullRequestDiffFile> changedFiles = fetchPRDiffFromBitbucket(pullRequestId);
      return processChangedFilesForPR(changedFiles, pullRequestId);
    } catch (Exception e) {
      log.error("Error fetching PR diff files: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to fetch PR diff files", e);
    }
  }

  /**
   * Gets the list of files changed in the PR that need validation.
   * Only returns files in the DSM_FOLDER_PATH directory.
   *
   * @param pullRequestId The ID of the pull request
   * @return List of FilePathAndChecksumEntity objects for validation
   */
  public List<FilePathAndChecksumEntity> getPRFilesToValidate(String pullRequestId) {
    log.info("Getting PR files to validate for PR: {}", pullRequestId);
    
    try {
      List<PullRequestDiffFile> changedFiles = fetchPRDiffFromBitbucket(pullRequestId);
      Map<Path, String> filesWithChecksums = processChangedFilesForPR(changedFiles, pullRequestId);
      List<FilePathAndChecksumEntity> filesToValidate = new ArrayList<>();
      
      for (Map.Entry<Path, String> entry : filesWithChecksums.entrySet()) {
        filesToValidate.add(new FilePathAndChecksumEntity(entry.getKey(), entry.getValue()));
      }
      
      log.info("Found {} files to validate in PR {}", filesToValidate.size(), pullRequestId);
      return filesToValidate;
      
    } catch (Exception e) {
      log.error("Error getting PR files to validate: {}", e.getMessage(), e);
      return new ArrayList<>();
    }
  }

  /**
   * Fetches the diff information from Bitbucket API for the given PR.
   */
  private List<PullRequestDiffFile> fetchPRDiffFromBitbucket(String pullRequestId) throws IOException {
    // Use the diffstat endpoint which gives us a cleaner list of changed files
    String apiUrl = String.format("%s/repositories/%s/%s/pullrequests/%s/diffstat", 
        bitbucketApiBaseUrl, bitbucketWorkspace, repositorySlug, pullRequestId);
    
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + getBitbucketAccessToken());
    headers.set("Accept", "application/json");
    
    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);
    
    if (!response.getStatusCode().is2xxSuccessful()) {
      throw new RuntimeException("Failed to fetch PR diffstat from Bitbucket: " + response.getStatusCode());
    }
    
    return parseDiffStatResponse(response.getBody(), pullRequestId);
  }

  /**
   * Parses the Bitbucket diffstat response and extracts changed files.
   */
  private List<PullRequestDiffFile> parseDiffStatResponse(String diffStatResponse, String pullRequestId) throws IOException {
    List<PullRequestDiffFile> changedFiles = new ArrayList<>();
    
    JsonNode rootNode = objectMapper.readTree(diffStatResponse);
    JsonNode valuesNode = rootNode.get("values");
    
    if (valuesNode != null && valuesNode.isArray()) {
      for (JsonNode fileNode : valuesNode) {
        JsonNode newFileNode = fileNode.get("new");
        JsonNode oldFileNode = fileNode.get("old");
        String status = fileNode.get("status").asText();
        
        String filePath = null;
        if (newFileNode != null && !newFileNode.isNull()) {
          filePath = newFileNode.get("path").asText();
        } else if (oldFileNode != null && !oldFileNode.isNull()) {
          filePath = oldFileNode.get("path").asText();
        }
        
        // Only process files in the DSM folder path
        if (filePath != null && filePath.startsWith(StringConstants.DSM_FOLDER_PATH.toString())) {
          PullRequestDiffFile diffFile = new PullRequestDiffFile();
          diffFile.setFilePath(filePath);
          diffFile.setStatus(status);
          
          // Extract lines added/removed if available
          if (fileNode.has("lines_added")) {
            diffFile.setLinesAdded(fileNode.get("lines_added").asInt());
          }
          if (fileNode.has("lines_removed")) {
            diffFile.setLinesRemoved(fileNode.get("lines_removed").asInt());
          }
          
          changedFiles.add(diffFile);
        }
      }
    }
    
    log.info("Found {} changed files in PR {}", changedFiles.size(), pullRequestId);
    return changedFiles;
  }

  /**
   * Processes the changed files and fetches their content to generate checksums.
   */
  private Map<Path, String> processChangedFilesForPR(List<PullRequestDiffFile> changedFiles, String pullRequestId) {
    Map<Path, String> filesWithChecksums = new HashMap<>();
    
    // Get the source branch information once for all files
    String sourceBranch = null;
    if (!changedFiles.isEmpty()) {
      try {
        sourceBranch = getPRSourceBranch(pullRequestId);
      } catch (Exception e) {
        log.error("Failed to get PR source branch: {}", e.getMessage());
        return filesWithChecksums;
      }
    }
    
    for (PullRequestDiffFile diffFile : changedFiles) {
      try {
        // Skip deleted files
        if ("removed".equals(diffFile.getStatus())) {
          continue;
        }
        
        String fileContent = fetchFileContentFromBranch(diffFile.getFilePath(), sourceBranch);
        String checksum = generateChecksum(fileContent);
        
        Path filePath = Paths.get(diffFile.getFilePath());
        filesWithChecksums.put(filePath, checksum);
        
      } catch (Exception e) {
        log.warn("Failed to process file {}: {}", diffFile.getFilePath(), e.getMessage());
      }
    }
    
    return filesWithChecksums;
  }

  /**
   * Gets the source branch name for the given PR.
   */
  private String getPRSourceBranch(String pullRequestId) throws IOException {
    String prDetailsUrl = String.format("%s/repositories/%s/%s/pullrequests/%s", 
        bitbucketApiBaseUrl, bitbucketWorkspace, repositorySlug, pullRequestId);
    
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + getBitbucketAccessToken());
    headers.set("Accept", "application/json");
    
    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<String> response = restTemplate.exchange(prDetailsUrl, HttpMethod.GET, entity, String.class);
    
    if (!response.getStatusCode().is2xxSuccessful()) {
      throw new RuntimeException("Failed to fetch PR details");
    }
    
    JsonNode prDetails = objectMapper.readTree(response.getBody());
    return prDetails.get("source").get("branch").get("name").asText();
  }

  /**
   * Fetches the content of a specific file from the given branch.
   */
  private String fetchFileContentFromBranch(String filePath, String branchName) throws IOException {
    String fileContentUrl = String.format("%s/repositories/%s/%s/src/%s/%s", 
        bitbucketApiBaseUrl, bitbucketWorkspace, repositorySlug, branchName, filePath);
    
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + getBitbucketAccessToken());
    headers.set("Accept", "text/plain");
    
    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<String> fileResponse = restTemplate.exchange(fileContentUrl, HttpMethod.GET, entity, String.class);
    
    if (!fileResponse.getStatusCode().is2xxSuccessful()) {
      throw new RuntimeException("Failed to fetch file content for: " + filePath);
    }
    
    return fileResponse.getBody();
  }

  /**
   * Generates MD5 checksum for the given content.
   */
  private String generateChecksum(String content) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] hashBytes = md.digest(content.getBytes(StandardCharsets.UTF_8));
      
      StringBuilder sb = new StringBuilder();
      for (byte b : hashBytes) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
      
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("MD5 algorithm not available", e);
    }
  }

  /**
   * Gets the Bitbucket access token from AWS Secrets Manager.
   */
  private String getBitbucketAccessToken() {
    try {
      // Retrieve the Bitbucket access token from AWS Secrets Manager
      // This should be configured similar to how Git credentials are handled
      return awsSecretManagerService.getSecret("us-west-2", "bitbucket", "ciq", "access-token");
    } catch (Exception e) {
      log.error("Error retrieving Bitbucket access token: {}", e.getMessage());
      throw new RuntimeException("Failed to retrieve Bitbucket access token", e);
    }
  }

}