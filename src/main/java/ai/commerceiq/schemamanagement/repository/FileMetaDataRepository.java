package ai.commerceiq.schemamanagement.repository;


import ai.commerceiq.schemamanagement.model.entity.FileMetaData;
import ai.commerceiq.schemamanagement.utils.StringConstants.Queries;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface FileMetaDataRepository extends JpaRepository<FileMetaData, Long> {

  @Query(value = Queries.GET_MIGRATED_FILES_CHECKSUM_TEMPLATE, nativeQuery = true)
  List<Object[]> getMigratedFilesChecksum();

  @Query(value = Queries.GET_VALIDATED_NON_MIGRATED_FILES_CHECKSUM_TEMPLATE, nativeQuery = true)
  List<Object[]> getValidatedNonMigratedFilesChecksum(List<String> filePaths);

  @Query(value = Queries.SELECT_FILE_METADATA_RECORD, nativeQuery = true)
  Optional<FileMetaData> findByAbsoluteFilePath(String absoluteFilePath);

  @Query(value = Queries.GET_NON_MIGRATED_FILES_PATH_BETA_TEMPLATE, nativeQuery = true)
  List<String> findFileNamesNotInMigrationStatusBeta();

  @Query(value = Queries.GET_NON_MIGRATED_FILES_PATH_PROD_TEMPLATE, nativeQuery = true)
  List<String> findFileNamesNotInMigrationStatusProd();

  @Query(value = Queries.GET_NON_MIGRATED_FILES_PATH_QA_TEMPLATE, nativeQuery = true)
  List<String> findFileNamesNotInMigrationStatusQa();

  @Query(value = Queries.SELECT_MIGRATED_FILES_FROM_LIST, nativeQuery = true)
  List<String> findMigratedFilesInList(List<String> filePaths);

  @Query(value = Queries.SELECT_FILES_NOT_MIGRATED_IN_BETA_FROM_GIVEN_FILE_LIST, nativeQuery = true)
  List<String> findNonBetaMigratedFilesInList(List<String> filePaths);

  @Query(value = Queries.SELECT_FILES_NOT_MIGRATED_IN_BETA_AND_QA_FROM_GIVEN_FILE_LIST, nativeQuery = true)
  List<String> findNonBetaAndQaMigratedFilesInList(List<String> filePaths);
}
