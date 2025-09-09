package ai.commerceiq.schemamanagement.model.general;

import com.github.dikhan.pagerduty.client.events.domain.EventResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagerDutyResponse {

  private String status;
  private String message;
  private String dedupKey;
  private String errors;

  public PagerDutyResponse(EventResult eventResultBuilder) {
    this.status = eventResultBuilder.getStatus();
    this.message = eventResultBuilder.getMessage();
    this.dedupKey = eventResultBuilder.getDedupKey();
    this.errors = eventResultBuilder.getErrors();
  }

  @Override
  public String toString() {
    return "status: " + status + ", message: " + message + ", dedupKey: " + dedupKey + ", errors: "
        + errors;
  }
}
