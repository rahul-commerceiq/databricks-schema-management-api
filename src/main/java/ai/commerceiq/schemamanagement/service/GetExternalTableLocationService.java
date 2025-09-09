package ai.commerceiq.schemamanagement.service;


import ai.commerceiq.schemamanagement.utils.StackTraceLoggingUtils;
import ai.commerceiq.schemamanagement.utils.StringConstants;
import com.amazonaws.regions.Regions;
import java.util.Collections;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class GetExternalTableLocationService {

  @Autowired
  AWSSecretManagerService awsSecretManagerService;
  @Value("${databricks.api.url}")
  private String apiUrl;

  @Value("${spring.current.env}")
  private String env;

  @Value("${databricks.api.token.secret}")
  private String apiTokenSecretName;


  private HttpHeaders getHttpHeaders() {
    HttpHeaders headers = new HttpHeaders();
    String apiToken = getDBXApiToken();
    headers.setBearerAuth(apiToken);
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    return headers;
  }

  public String getTableLocation(String tableName) throws RuntimeException {
    try {
      RestTemplate restTemplate = new RestTemplate();
      String url = apiUrl + tableName;
      log.info("Databricks API url:: {}", url);
      HttpEntity<Void> httpEntity = new HttpEntity<>(getHttpHeaders());
      ParameterizedTypeReference<HashMap<String, Object>> responseType = new ParameterizedTypeReference<HashMap<String, Object>>() {
      };

      val response = restTemplate.exchange(url, HttpMethod.GET, httpEntity, responseType);
      HashMap<String, Object> responseBody = response.getBody();
      String location = (String) responseBody.get("storage_location");
      log.info("table's external location:: {}", location);
      return location;

    } catch (HttpClientErrorException ex) {
      String responseBody = ex.getResponseBodyAsString();
      throw new RuntimeException(responseBody);
    }
  }

  private String getDBXApiToken() {
    log.info("Inside getDBXApiToken ");

    try {
      JSONObject cred = new JSONObject(
          awsSecretManagerService.getSecret(Regions.US_WEST_2.getName(), StringConstants.DATABRICKS,
              env, apiTokenSecretName));
      return cred.optString(StringConstants.AUTH_TOKEN, "");
    } catch (Exception e) {
      log.error("Error in getDBXApiToken: {}", StackTraceLoggingUtils.getStackTrace(e));
      return "";
    }

  }
}




