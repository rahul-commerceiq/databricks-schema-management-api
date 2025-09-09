package ai.commerceiq.schemamanagement.utils;


import ai.commerceiq.schemamanagement.model.entity.DropTableStatus;
import ai.commerceiq.schemamanagement.repository.DropTableStatusRepository;
import ai.commerceiq.schemamanagement.scheduler.DropExternalTableLocationScheduler;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MigrationExecutionHelperUtils {

  @Autowired
  private DropTableStatusRepository dropTableStatusRepository;
  @Autowired
  private DropExternalTableLocationScheduler dropExternalTableLocationScheduler;

  public void handleS3LocationForDroppedTable(String fullTableName, String tableLocation,
      int version, int changeSetId) {

    DropTableStatus dropTableStatus = dropTableStatusRepository.checkForceDeleteForDroppedTableEntryMigration(
        version, changeSetId);
    if (dropTableStatus != null) {
      dropTableStatus.setLocation(tableLocation);
      dropTableStatusRepository.save(dropTableStatus);
      dropExternalTableLocationScheduler.cleanUpS3Location(List.of(dropTableStatus));
    } else {
      dropTableStatusRepository.save(
          new DropTableStatus(fullTableName, tableLocation, version, changeSetId));

    }


  }
}
