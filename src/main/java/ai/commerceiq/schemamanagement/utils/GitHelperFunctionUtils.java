package ai.commerceiq.schemamanagement.utils;

import ai.commerceiq.schemamanagement.exception.GitOperationFailureException;
import ai.commerceiq.schemamanagement.service.AWSSecretManagerService;
import com.amazonaws.regions.Regions;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GitHelperFunctionUtils {

  @Autowired
  private AWSSecretManagerService awsSecretManagerService;

  public Path gitCloneRepoAndReturnRepoPath(String branchName) {
    log.info("Inside cloneRepository ");
    log.info("Branch::{}", branchName);
    Path repoPath = Paths.get(StringConstants.VERSION_MIGRATION_REPOSITORY_NAME);
    String absoluteRepoPath = repoPath.toAbsolutePath().toString();
    log.info("Repository Path: {}", absoluteRepoPath);
    try {
      File deploymentFolder = new File(absoluteRepoPath);
      if (deploymentFolder.exists()) {
        FileUtils.deleteDirectory(deploymentFolder);
        log.info("Removing existing folder {}..", absoluteRepoPath);
      }

      // Clone the repository
      log.info("Started cloning the repo with branch {}..", branchName);
      String password = getGitPassword();
      CloneCommand cloneCommand = Git.cloneRepository()
          .setURI(StringConstants.VERSION_MIGRATION_GIT_URL).setCredentialsProvider(
              new UsernamePasswordCredentialsProvider(StringConstants.GIT_CIQ_READUSER, password))
          .setDirectory(repoPath.toFile()).setBranch(branchName);

      Git git = cloneCommand.call();
      // Check if the branch exists
      if (!branchExists(git, branchName)) {
        log.info("Specified Branch does not exist: {}", branchName);
        throw new GitOperationFailureException(
            new Exception("Specified git branch does not exist"));//custom exception
      }
    } catch (GitAPIException e) {
      log.error("Error during Git operation: {}", e.getMessage());
      throw new GitOperationFailureException(e);
    } catch (IOException e) {
      log.error("Error: {}", e.getMessage());
    }
    return repoPath;
  }

  private String getGitPassword() {
    try {
      log.info("Inside getGitPassword ");
      JSONObject cred = new JSONObject(
          awsSecretManagerService.getSecret(Regions.US_WEST_2.getName(), StringConstants.GIT,
              StringConstants.CIQ, StringConstants.READUSER));
      return cred.optString(StringConstants.GIT_CIQ_READUSER, "");
    } catch (Exception e) {
      log.error("Error in getGitPassword: {}", StackTraceLoggingUtils.getStackTrace(e));
      return "";
    }
  }

  private boolean branchExists(Git git, String branchName) throws GitAPIException {
    return git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call().stream()
        .anyMatch(ref -> ref.getName().equals("refs/heads/" + branchName));
  }
}
