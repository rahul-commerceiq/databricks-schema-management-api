package ai.commerceiq.schemamanagement.exception;

public class ValidationMetadataInsertException extends RuntimeException {

  public ValidationMetadataInsertException(String errorMessage) {
    super("Error: Failed to insert metadata to database. " + errorMessage);
  }
}
