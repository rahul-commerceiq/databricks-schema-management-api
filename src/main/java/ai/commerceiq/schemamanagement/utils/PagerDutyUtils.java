package ai.commerceiq.schemamanagement.utils;

import ai.commerceiq.schemamanagement.exception.MigrationException;
import ai.commerceiq.schemamanagement.exception.MigrationFailedException;
import ai.commerceiq.schemamanagement.model.general.PagerDutyDetails;
import ai.commerceiq.schemamanagement.model.general.PagerDutyResponse;
import com.github.dikhan.pagerduty.client.events.PagerDutyEventsClient;
import com.github.dikhan.pagerduty.client.events.domain.Payload;
import com.github.dikhan.pagerduty.client.events.domain.Severity;
import com.github.dikhan.pagerduty.client.events.domain.TriggerIncident;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PagerDutyUtils {

  @Value("${spring.profiles.active}")
  private String env;

  public PagerDutyResponse triggerAlertEvent(PagerDutyDetails pagerDutyDetails) {
    log.info("Inside triggerAlertEvent");
    PagerDutyEventsClient pagerDutyEventsClient = PagerDutyEventsClient.create();
    try {
      Payload payload = Payload.Builder.newBuilder().setSummary(pagerDutyDetails.getSummary())
          .setSource(pagerDutyDetails.getSource()).setSeverity(pagerDutyDetails.getSeverity())
          .setTimestamp(OffsetDateTime.now()).setEventClass(pagerDutyDetails.getEventClass())
          .setGroup(pagerDutyDetails.getGroup())
          .setCustomDetails(pagerDutyDetails.getCustomDetails().toString()).build();

      TriggerIncident incident = TriggerIncident.TriggerIncidentBuilder.newBuilder(
              pagerDutyDetails.getRoutingKey(), payload).setDedupKey(pagerDutyDetails.getDedupKey())
          .build();
      return new PagerDutyResponse(pagerDutyEventsClient.trigger(incident));
    } catch (Exception e) {
      log.info("Error which sending pagerduty alert:: {}", e.getLocalizedMessage());
      return PagerDutyResponse.builder().message(e.getMessage()).status("FAILED").build();
    }
  }

  public void triggerPagerDutyAlert(MigrationException migrationException) {
    log.info("Inside triggerPagerDutyAlert");

    JSONObject detailsMap = createMigrationDetailsMap(migrationException);
    PagerDutyDetails pagerDutyDetails = buildPagerDutyDetails(detailsMap);

    PagerDutyResponse pagerDutyResponse = triggerAlertEvent(pagerDutyDetails);
    log.info("triggered PD: " + pagerDutyResponse.toString());
  }

  public void triggerPagerDutyAlert(String message) {
    log.info("Inside triggerPagerDutyAlert");

    JSONObject detailsMap = createMigrationDetailsMap(message);
    PagerDutyDetails pagerDutyDetails = buildPagerDutyDetails(detailsMap);

    PagerDutyResponse pagerDutyResponse = triggerAlertEvent(pagerDutyDetails);
    log.info("triggered PD: " + pagerDutyResponse.toString());
  }

  private JSONObject createMigrationDetailsMap(MigrationException migrationException) {
    log.info("Inside createDetailsMap");
    JSONObject detailsMap = new JSONObject();
    detailsMap.put("JobURL", migrationException.getJobUrl());
    detailsMap.put("Error", migrationException.getMessage());
    detailsMap.put("User", migrationException.getUserName());
    if (migrationException instanceof MigrationFailedException) {
      detailsMap.put("details",
          ((MigrationFailedException) migrationException).getFailedMigrationDetails());
    }
    return detailsMap;
  }

  private JSONObject createMigrationDetailsMap(String message) {
    log.info("Inside createDetailsMap");
    JSONObject detailsMap = new JSONObject();
    detailsMap.put("Error", message);
    return detailsMap;
  }

  private PagerDutyDetails buildPagerDutyDetails(JSONObject detailsMap) {
    log.info("Inside buildPagerDutyDetails");
    Severity severity;
    if (env.equals(StringConstants.BETA_ENV)) {
      severity = Severity.ERROR;
    } else {
      severity = Severity.CRITICAL;
    }
    String summary = StringConstants.DATABRICKS_MIGRATION_FAILURE.replace("{env}",
        this.env.toUpperCase());
    return PagerDutyDetails.builder()
        .routingKey(StringConstants.PAGERDUTY_INTEGRATION_KEY)
        .summary(summary)
        .source(StringConstants.DATABRICKS_SCHEMA_MANAGEMENT_API)
        .eventClass(StringConstants.ALERT)
        .severity(severity)
        .group(StringConstants.TRIGGERS)
        .customDetails(detailsMap)
        .build();
  }

}
