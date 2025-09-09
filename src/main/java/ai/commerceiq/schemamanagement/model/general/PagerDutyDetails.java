package ai.commerceiq.schemamanagement.model.general;

import com.github.dikhan.pagerduty.client.events.domain.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.json.JSONObject;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PagerDutyDetails {

  private String routingKey;
  private String dedupKey;
  private String summary;
  private String source;
  private Severity severity;
  private String component;
  private String group;
  private String eventClass;
  private JSONObject customDetails;
}
