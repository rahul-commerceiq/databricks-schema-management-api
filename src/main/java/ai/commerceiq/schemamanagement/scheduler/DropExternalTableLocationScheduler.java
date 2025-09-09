package ai.commerceiq.schemamanagement.scheduler;


import ai.commerceiq.schemamanagement.model.entity.DropTableStatus;
import ai.commerceiq.schemamanagement.repository.DropTableStatusRepository;
import ai.commerceiq.schemamanagement.utils.PagerDutyUtils;
import ai.commerceiq.schemamanagement.utils.StringConstants;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Uri;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
@Slf4j
public class DropExternalTableLocationScheduler {


  @Autowired
  PagerDutyUtils pagerDutyUtils;
  @Autowired
  private DropTableStatusRepository dropTableStatusRepository;
  @Value("${databricks.s3path.cleanup-threshold-days}")
  private Long thresholdDaysToCleanUp;

  public static String addSlashToS3uriIfNeeded(String s3String) {
    log.info("Inside addSlashToS3uriIfNeeded");
    if (!s3String.endsWith("/")) {
      s3String += "/";
    }
    log.info("Edited s3Uri:: {}", s3String);
    return s3String;
  }

  @Scheduled(cron = "0 0 11 * * *") // Execute every day at 11:00 AM
  public void dropTablesExternalLocation() {
    log.info("Inside dropTablesExternalLocation");

    long thresholdDaysCleanUpInSeconds =
        thresholdDaysToCleanUp * StringConstants.DAYS_IN_MILLISECONDS;
    Date thresholdDays = new Date(System.currentTimeMillis() - thresholdDaysCleanUpInSeconds);
    log.info("Deleting tables older than:: {}", thresholdDays);

    List<DropTableStatus> dropTablesList = dropTableStatusRepository.findAllUpdatedThirtyDaysAgo(
        thresholdDays);
    log.info("Tables location to be dropped:: {}", dropTablesList);

    if (!dropTablesList.isEmpty()) {
      cleanUpS3Location(dropTablesList);
    } else {
      log.info("No tables to clean up");
    }

  }

  //Called from  scheduler and migration api
  public void cleanUpS3Location(List<DropTableStatus> dropTableList) {
    log.info("Inside cleanUpS3Location ");
    S3Client s3Client = S3Client.create();
    S3Utilities s3Utilities = s3Client.utilities();
    for (DropTableStatus dropTableObject : dropTableList) {
      log.info("Processing to delete s3 location:: {}", dropTableObject.getLocation());
      URI uri = URI.create(addSlashToS3uriIfNeeded(dropTableObject.getLocation()));
      S3Uri s3Uri = s3Utilities.parseUri(uri);

      boolean s3PathDeleted = getAndDeleteS3ObjectKeys(s3Client, s3Uri);

      if (s3PathDeleted) {
        dropTableObject.setStatus(StringConstants.DELETED);
        dropTableStatusRepository.save(dropTableObject);
      }
    }

    s3Client.close();
  }

  //Called from cleanupS3 Api
  public void deleteS3Paths(List<String> s3Paths) {
    S3Client s3Client = S3Client.create();
    S3Utilities s3Utilities = s3Client.utilities();
    for (String s3Path : s3Paths) {
      URI uri = URI.create(addSlashToS3uriIfNeeded(s3Path));
      S3Uri s3Uri = s3Utilities.parseUri(uri);
      getAndDeleteS3ObjectKeys(s3Client, s3Uri);
    }
  }

  //Called from drop table api
  public void deleteS3Path(String s3Path) {
    S3Client s3Client = S3Client.create();
    S3Utilities s3Utilities = s3Client.utilities();
    URI uri = URI.create(addSlashToS3uriIfNeeded(s3Path));
    S3Uri s3Uri = s3Utilities.parseUri(uri);
    getAndDeleteS3ObjectKeys(s3Client, s3Uri);
  }


  private boolean getAndDeleteS3ObjectKeys(S3Client s3Client, S3Uri s3Uri) {
    log.info("Inside getS3ObjectKeys");

    String bucketName = s3Uri.bucket().orElse(null);
    String prefix = s3Uri.key().orElse(null);
    log.info("Bucket and path:: {} {}", bucketName, prefix);

    ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
        .bucket(bucketName)
        .prefix(prefix)
        .build();
    ListObjectsV2Response listObjectsResponse;
    long totalDeletedObjCount = 0;
    boolean isS3ObjectKeysDeleted = false;

    do {
      listObjectsResponse = s3Client.listObjectsV2(listObjectsRequest);

      List<ObjectIdentifier> s3objectKeys = listObjectsResponse.contents().stream()
          .map(s3Object -> ObjectIdentifier.builder().key(s3Object.key()).build())
          .collect(Collectors.toList());
      totalDeletedObjCount += s3objectKeys.size();
      log.info("S3 object count::{}, total objects deleted so far:: {}", s3objectKeys.size(),
          totalDeletedObjCount);
      if (!s3objectKeys.isEmpty()) {
        isS3ObjectKeysDeleted = deleteS3Objects(s3Client, s3Uri, s3objectKeys);
      } else {
        log.info("Nothing to delete at given S3 URI: {}", s3Uri);
      }

      String continuationToken = listObjectsResponse.nextContinuationToken();
      listObjectsRequest = listObjectsRequest.toBuilder()
          .continuationToken(continuationToken)
          .build();
      log.info("continuationToken:: {} , isTruncated:: {}, isS3ObjectKeysDeleted:: {}",
          continuationToken, listObjectsResponse.isTruncated(), isS3ObjectKeysDeleted);
    } while (listObjectsResponse.isTruncated() && isS3ObjectKeysDeleted);
    log.info("TotalDeleted object count:: {}", totalDeletedObjCount);
    return isS3ObjectKeysDeleted;
  }


  private boolean deleteS3Objects(S3Client s3Client, S3Uri s3Uri, List<ObjectIdentifier> keys) {

    log.info("Inside deleteS3Objects");
    String bucketName = s3Uri.bucket().orElse(null);
    String folderKey = s3Uri.key().orElse(null);

    log.info("Deleting s3 objects:: BucketName: {} folderKey: {} keys: {}", bucketName, folderKey,
        keys);
    if (bucketName == null || folderKey == null) {
      log.error("Error while deleting S3 path. Invalid bucket / folder path : {} {}", bucketName,
          folderKey);
      return false;
    }

    DeleteObjectsRequest multiObjectDeleteRequest = DeleteObjectsRequest.builder()
        .bucket(bucketName).delete(Delete.builder().objects(keys).build()).build();

    try {
      s3Client.deleteObjects(multiObjectDeleteRequest);
      log.info("Multiple objects are deleted at:: {} ", folderKey);
      return true;
    } catch (S3Exception e) {
      log.info("Error while deleting s3 object keys:: {}", e.awsErrorDetails().errorMessage());
      String error_message = "Error while deleting s3 object keys at " + s3Uri;
      pagerDutyUtils.triggerPagerDutyAlert(error_message);
      return false;
    }

  }

}