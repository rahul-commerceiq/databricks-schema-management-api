package ai.commerceiq.schemamanagement.exception;

public class GitOperationFailureException extends RuntimeException {

  public GitOperationFailureException(Exception e) {
    super(e.getMessage());
  }
}
