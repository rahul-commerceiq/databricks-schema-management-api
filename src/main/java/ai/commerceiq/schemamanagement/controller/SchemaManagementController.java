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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
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

  /**
   * Legacy validation endpoint for backward compatibility.
   * Uses full repository cloning approach.
   */
  @GetMapping(value = "/validate", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> validate(@RequestParam String branchName,
      @RequestParam String userName) {
    log.info("Validation request - Branch: {}, User: {}", branchName, userName);
    ValidateApiResponse response = validationService.validateFiles(branchName, userName);
    return ResponseEntity.ok(response);
  }

  /**
   * New optimized validation endpoint that supports PR diff-based validation.
   * This endpoint provides better performance by processing only changed files.
   * 
   * @param branchName The name of the branch to validate
   * @param userName The user performing the validation
   * @param usePRDiff Whether to use PR diff-based validation (default: true)
   * @param prNumber The PR number for diff-based validation (optional)
   * @return Validation response
   */
  @GetMapping(value = "/validate-optimized", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> validateOptimized(
      @RequestParam String branchName,
      @RequestParam String userName,
      @RequestParam(defaultValue = "true") boolean usePRDiff,
      @RequestParam(required = false) String prNumber) {
    
    log.info("Optimized validation request - Branch: {}, User: {}, UsePRDiff: {}, PR: {}", 
        branchName, userName, usePRDiff, prNumber);
    
    try {
      ValidateApiResponse response = validationService.validateFiles(branchName, userName, usePRDiff, prNumber);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error during optimized validation: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError()
          .body("Validation failed: " + e.getMessage());
    }
  }

  /**
   * PR diff-specific validation endpoint.
   * This endpoint is specifically designed for PR-based validation workflows.
   * 
   * @param branchName The name of the branch to validate
   * @param userName The user performing the validation
   * @param prNumber The PR number for diff-based validation
   * @return Validation response
   */
  @GetMapping(value = "/validate-pr-diff", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> validatePRDiff(
      @RequestParam String branchName,
      @RequestParam String userName,
      @RequestParam String prNumber) {
    
    log.info("PR diff validation request - Branch: {}, User: {}, PR: {}", 
        branchName, userName, prNumber);
    
    try {
      ValidateApiResponse response = validationService.validateFiles(branchName, userName, true, prNumber);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error during PR diff validation: {}", e.getMessage(), e);
      return ResponseEntity.internalServerError()
          .body("PR diff validation failed: " + e.getMessage());
    }
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