package ai.commerceiq.schemamanagement.service;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.DecryptionFailureException;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.InternalServiceErrorException;
import com.amazonaws.services.secretsmanager.model.InvalidParameterException;
import com.amazonaws.services.secretsmanager.model.InvalidRequestException;
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AWSSecretManagerService {

  public String getSecret(String region, String secretType, String env, String user) {
    String secretName = String.format("%s/%s/%s", secretType, env, user);
    log.info("Inside getSecret method with params :: {}", secretName);

    AWSSecretsManager client = AWSSecretsManagerClientBuilder.standard().withRegion(region).build();

    String secret, decodedBinarySecret;
    GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest().withSecretId(
        secretName);
    GetSecretValueResult getSecretValueResult;

    try {
      getSecretValueResult = client.getSecretValue(getSecretValueRequest);
    } catch (DecryptionFailureException | ResourceNotFoundException | InvalidRequestException |
             InvalidParameterException | InternalServiceErrorException e) {
      log.error("Error while getting secret:: {}", e.getMessage());
      throw e;
    }

    if (getSecretValueResult.getSecretString() != null) {
      log.info("Getting secret as string..");
      secret = getSecretValueResult.getSecretString();
      return secret;
    } else {
      log.info("Getting secret as binary..");
      decodedBinarySecret = new String(
          Base64.getDecoder().decode(getSecretValueResult.getSecretBinary()).array());
      return decodedBinarySecret;
    }
  }
}
