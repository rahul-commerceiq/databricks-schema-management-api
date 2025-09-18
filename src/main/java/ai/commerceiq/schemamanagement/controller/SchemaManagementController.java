package ai.commerceiq.schemamanagement.controller;

import ai.commerceiq.schemamanagement.model.request.MigrateRequestModel;
import ai.commerceiq.schemamanagement.model.request.MigrationReqQueryParmsAndBody;
import ai.commerceiq.schemamanagement.model.response.DeleteTablesAndS3Response;
import ai.commerceiq.schemamanagement.model.response.MigrationApiSuccessfulResponse;
import ai.commerceiq.schemamanagement.model.response.NonMigratedFilesApiResponse;
import ai.commerceiq.schemamanagement.model.response.ValidateApiResponse;
import ai.commerceiq.schemamanagement.scheduler.DropExternalTableLocationScheduler;
import ai.commerceiq.schemamanagement.service.DropTableService;
import ai.commerceiq.schemamanagement.service.GetMigrationFilesService;
import ai.commerceiq.schemamanagement.service.MigrationService;
import ai.commerceiq.schemamanagement.service.ValidationService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/schemaManagement/v1")
public class SchemaManagementController {

  @Autowired
  private ValidationService validationService;
  @Autowired
  private MigrationService migrateService;
  @Autowired
  private GetMigrationFilesService getMigrationFilesService;
  @Autowired
  private DropTableService dropTableService;

  @Autowired
  private DropExternalTableLocationScheduler dropExternalTableLocationScheduler;

  @GetMapping(value = "/validate", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> validate(@RequestParam String branchName,
      @RequestParam String userName) {
    ValidateApiResponse response = validationService.validateFiles(branchName, userName);
    return ResponseEntity.ok(response);
  }

  /**
   * Enhanced validation endpoint that supports PR context for optimized processing.
   * This endpoint uses PR diff to process only changed files, reducing pipeline latency.
   *
   * @param branchName    The name of the branch to validate (required)
   * @param userName      The user performing the validation (required)
   * @param pullRequestId The ID of the pull request (optional - enables PR diff optimization)
   * @param baseBranch    The base branch to compare against (optional - defaults to master)
   * @return ResponseEntity containing the validation results
   */
  @GetMapping(value = "/validate-pr", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> validateWithPRContext(
      @RequestParam String branchName,
      @RequestParam String userName,
      @RequestParam(required = false) String pullRequestId,
      @RequestParam(required = false) String baseBranch) {
    ValidateApiResponse response = validationService.validateFilesWithPRContext(
        branchName, pullRequestId, baseBranch, userName);
    return ResponseEntity.ok(response);
  }

  @PostMapping(value = "/migrate", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> migrate(@RequestParam String userName, @RequestParam String jobUrl,
      @RequestBody MigrateRequestModel migrateReqBody) {
    MigrationReqQueryParmsAndBody migrationReqQueryParmsAndBody = new MigrationReqQueryParmsAndBody(
        migrateReqBody, userName, jobUrl);
    MigrationApiSuccessfulResponse response = migrateService.migrateFiles(
        migrationReqQueryParmsAndBody);
    return ResponseEntity.ok(response);
  }

  @GetMapping(value = "/non-migrated-files", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> getNonMigratedFiles() {
    NonMigratedFilesApiResponse response = getMigrationFilesService.getFilesToMigrate();
    return ResponseEntity.ok(response);
  }

  @GetMapping(value = "/health", produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<Object> health() {
    return ResponseEntity.ok("OK");
  }

  @PostMapping(value = "/cleanupS3", produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<Object> cleanupS3(@RequestBody List<String> s3Paths) {
    dropExternalTableLocationScheduler.deleteS3Paths(s3Paths);
    return ResponseEntity.ok("OK");
  }

  @PostMapping(value = "/drop-tables", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> dropTables(@RequestParam String userName,
      @RequestParam String jobUrl, @RequestBody List<String> tablesList) {
    List<DeleteTablesAndS3Response> response = dropTableService.dropTables(tablesList);
    return ResponseEntity.ok(response);
  }
}
