package ai.commerceiq.schemamanagement.advice;

import ai.commerceiq.schemamanagement.exception.EmptyMigrationFilesListException;
import ai.commerceiq.schemamanagement.exception.FilesNotMigratedToLowerEnvException;
import ai.commerceiq.schemamanagement.exception.GitOperationFailureException;
import ai.commerceiq.schemamanagement.exception.InValidFileNameException;
import ai.commerceiq.schemamanagement.exception.MigratedFilesForMigrationException;
import ai.commerceiq.schemamanagement.exception.MigrationException;
import ai.commerceiq.schemamanagement.exception.MigrationFailedException;
import ai.commerceiq.schemamanagement.exception.NoNewFilesFoundException;
import ai.commerceiq.schemamanagement.exception.OldFilesDeletedAndUpdatedException;
import ai.commerceiq.schemamanagement.exception.OldFilesDeletedException;
import ai.commerceiq.schemamanagement.exception.OldFilesUpdatedException;
import ai.commerceiq.schemamanagement.exception.ValidationMetadataInsertException;
import ai.commerceiq.schemamanagement.model.response.ExceptionResponse;
import ai.commerceiq.schemamanagement.model.response.MigrationApiFailedResponse;
import ai.commerceiq.schemamanagement.model.response.OldFilesDeletedAndUpdatedResponse;
import ai.commerceiq.schemamanagement.utils.PagerDutyUtils;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @Autowired
  PagerDutyUtils pagerDutyUtils;

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ExceptionResponse> handleGenericExceptions(Exception exception) {
    log.error("Runtime Exception:: {}", exception.getMessage());
    ExceptionResponse error = ExceptionResponse.builder().message(
            exception.getMessage().substring(0, Math.min(exception.getMessage().length(), 1000)))
        .build();
    return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(GitAPIException.class)
  public ResponseEntity<ExceptionResponse> handleGitCloneException(GitAPIException ex) {
    log.error("Exception during Git clone operation:: {}", ex.getMessage());
    ExceptionResponse error = ExceptionResponse.builder().message(
        "Error during Git clone: " + ex.getMessage()
            .substring(0, Math.min(ex.getMessage().length(), 1000))).build();
    return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(OldFilesUpdatedException.class)
  public ResponseEntity<OldFilesDeletedAndUpdatedResponse> handleOldFilesUpdatedException(
      OldFilesUpdatedException oldFilesUpdatedException) {
    log.error("Old files were updated in the branch:: {}",
        oldFilesUpdatedException.getUpdatedFiles());
    OldFilesDeletedAndUpdatedResponse oldFilesUpdatedResponse = OldFilesDeletedAndUpdatedResponse.builder()
        .message(oldFilesUpdatedException.getMessage()
            .substring(0, Math.min(oldFilesUpdatedException.getMessage().length(), 1000)))
        .updatedFiles(oldFilesUpdatedException.getUpdatedFiles()).build();

    return new ResponseEntity<>(oldFilesUpdatedResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(OldFilesDeletedException.class)
  public ResponseEntity<OldFilesDeletedAndUpdatedResponse> handleOldFilesDeletedException(
      OldFilesDeletedException oldFilesDeletedException) {
    log.error("Old files were deleted in the branch:: {}",
        oldFilesDeletedException.getDeletedFiles());
    OldFilesDeletedAndUpdatedResponse oldFilesDeletedResponse = OldFilesDeletedAndUpdatedResponse.builder()
        .message(oldFilesDeletedException.getMessage()
            .substring(0, Math.min(oldFilesDeletedException.getMessage().length(), 1000)))
        .deletedFiles(oldFilesDeletedException.getDeletedFiles()).build();
    return new ResponseEntity<>(oldFilesDeletedResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(OldFilesDeletedAndUpdatedException.class)
  public ResponseEntity<OldFilesDeletedAndUpdatedResponse> handleOldFilesDeletedAndUpdatedException(
      OldFilesDeletedAndUpdatedException oldFilesDeletedAndUpdatedException) {
    log.error("Old files were updated in the given branch:: {}",
        oldFilesDeletedAndUpdatedException.getUpdatedFiles());
    log.error("Old files were deleted in the given branch:: {}",
        oldFilesDeletedAndUpdatedException.getDeletedFiles());
    OldFilesDeletedAndUpdatedResponse oldFilesDeletedAndUpdatedResponse = OldFilesDeletedAndUpdatedResponse.builder()
        .message(oldFilesDeletedAndUpdatedException.getMessage()
            .substring(0, Math.min(oldFilesDeletedAndUpdatedException.getMessage().length(), 1000)))
        .deletedFiles(oldFilesDeletedAndUpdatedException.getDeletedFiles())
        .updatedFiles(oldFilesDeletedAndUpdatedException.getUpdatedFiles()).build();
    log.info("oldFilesDeletedAndUpdatedResponse:: {}", oldFilesDeletedAndUpdatedResponse);
    return new ResponseEntity<>(oldFilesDeletedAndUpdatedResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(GitOperationFailureException.class)
  public ResponseEntity<ExceptionResponse> handleGitOperationFailureException(
      GitOperationFailureException GitOperationFailureException) {
    log.info("Error:: {}", GitOperationFailureException.getMessage());
    ExceptionResponse error = ExceptionResponse.builder().message(
            GitOperationFailureException.getMessage()
                .substring(0, Math.min(GitOperationFailureException.getMessage().length(), 1000)))
        .build();
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ValidationMetadataInsertException.class)
  public ResponseEntity<ExceptionResponse> handleValidationMetadataInsertException(
      ValidationMetadataInsertException validationMetadataInsertException) {
    log.info("Error:: {}", validationMetadataInsertException.getMessage());
    ExceptionResponse error = ExceptionResponse.builder().message(
            validationMetadataInsertException.getMessage()
                .substring(0, Math.min(validationMetadataInsertException.getMessage().length(), 1000)))
        .build();

    return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
  }


  @ExceptionHandler(NoNewFilesFoundException.class)
  public ResponseEntity<ExceptionResponse> handleNoNewFilesFoundException(
      NoNewFilesFoundException noNewFilesFoundException) {
    log.info("Error:: {}", noNewFilesFoundException.getMessage());
    ExceptionResponse error = ExceptionResponse.builder().message(
        noNewFilesFoundException.getMessage()
            .substring(0, Math.min(noNewFilesFoundException.getMessage().length(), 1000))).build();

    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(InValidFileNameException.class)
  public ResponseEntity<ExceptionResponse> handleInvalidFileNameException(
      InValidFileNameException inValidFileNameException) {
    log.info("Error:: {}", inValidFileNameException.getMessage());
    ExceptionResponse error = ExceptionResponse.builder().message(
            inValidFileNameException.getMessage()
                .substring(0, Math.min(inValidFileNameException.getMessage().length(), 1000)))
        .details(inValidFileNameException.getInValidFileNamesList()).build();

    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  //Migration APIs exceptions

  @ExceptionHandler(MigrationException.class)
  public ResponseEntity<ExceptionResponse> handleGenericMigrationException(
      MigrationException migrationException) {
    log.info("Error during migration:: {}", migrationException.getMessage());
    ExceptionResponse error = ExceptionResponse.builder().message(migrationException.getMessage()
        .substring(0, Math.min(migrationException.getMessage().length(), 1000))).build();
    pagerDutyUtils.triggerPagerDutyAlert(migrationException);
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MigrationFailedException.class)
  public ResponseEntity<MigrationApiFailedResponse> handleMigrationFailedException(
      MigrationFailedException migrationFailedException) {
    log.info("Error:: {}", migrationFailedException.getLocalizedMessage());
    MigrationApiFailedResponse migrationApiFailedResponse = new MigrationApiFailedResponse(
        migrationFailedException);
    pagerDutyUtils.triggerPagerDutyAlert(migrationFailedException);
    return new ResponseEntity<>(migrationApiFailedResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(MigratedFilesForMigrationException.class)
  public ResponseEntity<ExceptionResponse> handleMigratedFilesForMigrationException(
      MigratedFilesForMigrationException migratedFilesForMigrationException) {
    log.info("Files in the migration files list that have already been migrated:: {}",
        migratedFilesForMigrationException.getFileList());
    ExceptionResponse error = ExceptionResponse.builder().message(
            migratedFilesForMigrationException.getMessage()
                .substring(0, Math.min(migratedFilesForMigrationException.getMessage().length(), 1000)))
        .details(migratedFilesForMigrationException.getFileList()).build();
    pagerDutyUtils.triggerPagerDutyAlert(migratedFilesForMigrationException);
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(FilesNotMigratedToLowerEnvException.class)
  public ResponseEntity<ExceptionResponse> handleFilesNotMigratedToBetaException(
      FilesNotMigratedToLowerEnvException filesNotMigratedToLowerEnvException) {
    log.info("Files in the migration files list that have not been migrated to beta:: {}",
        filesNotMigratedToLowerEnvException.getFileList());
    ExceptionResponse error = ExceptionResponse.builder().message(
        filesNotMigratedToLowerEnvException.getMessage().substring(0, Math.min(
            filesNotMigratedToLowerEnvException.getMessage().length(), 1000))).details(
        filesNotMigratedToLowerEnvException.getFileList()).build();
    pagerDutyUtils.triggerPagerDutyAlert(filesNotMigratedToLowerEnvException);
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(EmptyMigrationFilesListException.class)
  public ResponseEntity<ExceptionResponse> handleEmptyMigrationFilesListException(
      EmptyMigrationFilesListException emptyMigrationFilesListException) {
    log.info("Empty file list in request body for migration");
    ExceptionResponse error = ExceptionResponse.builder().message(
            emptyMigrationFilesListException.getMessage()
                .substring(0, Math.min(emptyMigrationFilesListException.getMessage().length(), 1000)))
        .build();
    pagerDutyUtils.triggerPagerDutyAlert(emptyMigrationFilesListException);
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

}

