package ai.commerceiq.schemamanagement.utils;

import ai.commerceiq.schemamanagement.exception.GitOperationFailureException;
import ai.commerceiq.schemamanagement.service.AWSSecretManagerService;
import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Utility class for Git diff operations using Bitbucket API.
 * This class handles API calls to retrieve PR diffs and download individual files,
 * optimizing the validation pipeline by avoiding full repository clones.
 */
@Service
@Slf4j
public class GitDiffHelperUtils {

  private static final String BITBUCKET_API_BASE = "https://api.bitbucket.org/2.0";
  private static final int MAX_RETRIES = 3;
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
  
  @Autowired
  private AWSSecretManagerService awsSecretManagerService;
  
  @Value("${bitbucket.workspace:commerceiq}")
  private String bitbucketWorkspace;
  
  @Value("${bitbucket.repository.slug:databricks-schema-management}")
  private String repositorySlug;
  
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  
  public GitDiffHelperUtils() {
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(REQUEST_TIMEOUT)
        .build();
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Retrieves the list of changed files in a pull request.
   *
   * @param pullRequestId The ID of the pull request
   * @return List of file paths that were changed in the PR
   */
  public List<String> getChangedFilesInPR(String pullRequestId) {
    log.info("Retrieving changed files for PR: {}", pullRequestId);
    
    String url = String.format("%s/repositories/%s/%s/pullrequests/%s/diffstat",
        BITBUCKET_API_BASE, bitbucketWorkspace, repositorySlug, pullRequestId);
    
    try {
      String responseBody = makeAuthenticatedRequest(url);
      return parseChangedFilesFromDiffstat(responseBody);
    } catch (Exception e) {
      log.error("Error retrieving changed files for PR {}: {}", pullRequestId, e.getMessage());
      throw new GitOperationFailureException(e);
    }
  }

  /**
   * Retrieves the list of changed files between two branches.
   *
   * @param sourceBranch The source branch
   * @param baseBranch   The base branch to compare against
   * @return List of file paths that were changed between the branches
   */
  public List<String> getChangedFilesBetweenBranches(String sourceBranch, String baseBranch) {
    log.info("Retrieving changed files between {} and {}", sourceBranch, baseBranch);
    
    String url = String.format("%s/repositories/%s/%s/diff/%s..%s",
        BITBUCKET_API_BASE, bitbucketWorkspace, repositorySlug, baseBranch, sourceBranch);
    
    try {
      String responseBody = makeAuthenticatedRequest(url);
      return parseChangedFilesFromDiff(responseBody);
    } catch (Exception e) {
      log.error("Error retrieving changed files between branches {} and {}: {}", 
          sourceBranch, baseBranch, e.getMessage());
      throw new GitOperationFailureException(e);
    }
  }

  /**
   * Downloads the content of a specific file from a branch.
   *
   * @param filePath The path of the file to download
   * @param branch   The branch from which to download the file
   * @return The content of the file as a byte array
   */
  public byte[] downloadFileContent(String filePath, String branch) {
    log.debug("Downloading file: {} from branch: {}", filePath, branch);
    
    String url = String.format("%s/repositories/%s/%s/src/%s/%s",
        BITBUCKET_API_BASE, bitbucketWorkspace, repositorySlug, branch, filePath);
    
    try {
      String responseBody = makeAuthenticatedRequest(url);
      return responseBody.getBytes(StandardCharsets.UTF_8);
    } catch (Exception e) {
      log.error("Error downloading file {} from branch {}: {}", filePath, branch, e.getMessage());
      throw new GitOperationFailureException(e);
    }
  }

  /**
   * Makes an authenticated HTTP request to the Bitbucket API.
   *
   * @param url The URL to make the request to
   * @return The response body as a string
   * @throws IOException          If there's a network error
   * @throws InterruptedException If the request is interrupted
   */
  private String makeAuthenticatedRequest(String url) throws IOException, InterruptedException {
    String credentials = getBasicAuthCredentials();
    
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Authorization", "Basic " + credentials)
        .header("Accept", "application/json")
        .timeout(REQUEST_TIMEOUT)
        .GET()
        .build();
    
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    
    if (response.statusCode() >= 200 && response.statusCode() < 300) {
      return response.body();
    } else {
      String errorMsg = String.format("HTTP %d: %s", response.statusCode(), response.body());
      log.error("Bitbucket API error for URL {}: {}", url, errorMsg);
      throw new IOException(errorMsg);
    }
  }

  /**
   * Retrieves basic authentication credentials for Bitbucket API.
   *
   * @return Base64 encoded credentials
   */
  private String getBasicAuthCredentials() {
    try {
      log.debug("Retrieving Bitbucket credentials from AWS Secrets Manager");
      JSONObject cred = new JSONObject(
          awsSecretManagerService.getSecret(Regions.US_WEST_2.getName(), StringConstants.GIT,
              StringConstants.CIQ, StringConstants.READUSER));
      
      String username = StringConstants.GIT_CIQ_READUSER;
      String password = cred.optString(StringConstants.GIT_CIQ_READUSER, "");
      
      String credentials = username + ":" + password;
      return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      log.error("Error retrieving Bitbucket credentials: {}", StackTraceLoggingUtils.getStackTrace(e));
      throw new RuntimeException("Failed to retrieve Bitbucket credentials", e);
    }
  }

  /**
   * Parses changed files from the diffstat API response.
   *
   * @param responseBody The JSON response from the diffstat endpoint
   * @return List of changed file paths
   */
  private List<String> parseChangedFilesFromDiffstat(String responseBody) {
    List<String> changedFiles = new ArrayList<>();
    
    try {
      JsonNode root = objectMapper.readTree(responseBody);
      JsonNode values = root.get("values");
      
      if (values != null && values.isArray()) {
        for (JsonNode fileNode : values) {
          JsonNode newFile = fileNode.get("new");
          if (newFile != null) {
            JsonNode pathNode = newFile.get("path");
            if (pathNode != null) {
              String filePath = pathNode.asText();
              changedFiles.add(filePath);
              log.debug("Found changed file: {}", filePath);
            }
          }
        }
      }
      
      log.info("Parsed {} changed files from diffstat", changedFiles.size());
      return changedFiles;
      
    } catch (Exception e) {
      log.error("Error parsing diffstat response: {}", e.getMessage());
      throw new RuntimeException("Failed to parse changed files from diffstat", e);
    }
  }

  /**
   * Parses changed files from the diff API response.
   * This is used for branch-to-branch comparisons.
   *
   * @param responseBody The response from the diff endpoint
   * @return List of changed file paths
   */
  private List<String> parseChangedFilesFromDiff(String responseBody) {
    List<String> changedFiles = new ArrayList<>();
    
    try {
      // The diff API response is typically in unified diff format
      // We need to parse the diff headers to extract file paths
      String[] lines = responseBody.split("\n");
      
      for (String line : lines) {
        if (line.startsWith("diff --git")) {
          // Extract file path from git diff header
          // Format: diff --git a/path/to/file b/path/to/file
          String[] parts = line.split(" ");
          if (parts.length >= 4) {
            String filePath = parts[3].substring(2); // Remove "b/" prefix
            changedFiles.add(filePath);
            log.debug("Found changed file: {}", filePath);
          }
        }
      }
      
      log.info("Parsed {} changed files from diff", changedFiles.size());
      return changedFiles;
      
    } catch (Exception e) {
      log.error("Error parsing diff response: {}", e.getMessage());
      throw new RuntimeException("Failed to parse changed files from diff", e);
    }
  }

  /**
   * Checks if a pull request exists and is accessible.
   *
   * @param pullRequestId The ID of the pull request
   * @return true if the PR exists and is accessible, false otherwise
   */
  public boolean pullRequestExists(String pullRequestId) {
    log.debug("Checking if PR {} exists", pullRequestId);
    
    String url = String.format("%s/repositories/%s/%s/pullrequests/%s",
        BITBUCKET_API_BASE, bitbucketWorkspace, repositorySlug, pullRequestId);
    
    try {
      makeAuthenticatedRequest(url);
      return true;
    } catch (Exception e) {
      log.warn("PR {} does not exist or is not accessible: {}", pullRequestId, e.getMessage());
      return false;
    }
  }

  /**
   * Gets pull request information including source and destination branches.
   *
   * @param pullRequestId The ID of the pull request
   * @return JsonNode containing PR information
   */
  public JsonNode getPullRequestInfo(String pullRequestId) {
    log.debug("Getting PR info for PR: {}", pullRequestId);
    
    String url = String.format("%s/repositories/%s/%s/pullrequests/%s",
        BITBUCKET_API_BASE, bitbucketWorkspace, repositorySlug, pullRequestId);
    
    try {
      String responseBody = makeAuthenticatedRequest(url);
      return objectMapper.readTree(responseBody);
    } catch (Exception e) {
      log.error("Error retrieving PR info for {}: {}", pullRequestId, e.getMessage());
      throw new GitOperationFailureException(e);
    }
  }
}