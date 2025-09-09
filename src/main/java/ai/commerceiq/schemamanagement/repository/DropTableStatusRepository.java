package ai.commerceiq.schemamanagement.repository;

import ai.commerceiq.schemamanagement.model.entity.DropTableStatus;
import ai.commerceiq.schemamanagement.utils.StringConstants.Queries;
import jakarta.transaction.Transactional;
import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface DropTableStatusRepository extends JpaRepository<DropTableStatus, Integer> {

  @Query(value = Queries.SELECT_DROP_TABLES_RECORD, nativeQuery = true)
  List<DropTableStatus> findAllUpdatedThirtyDaysAgo(@Param("thresholdDays") Date thirtyDaysAgo);

  @Query(value = Queries.CHECK_TO_FORCE_DROP_FORCE_DROP_TABLES_RECORD, nativeQuery = true)
  DropTableStatus checkForceDeleteForDroppedTableEntryMigration(@Param("version") int version,
      @Param("changeSetId") int changeSetId);

  @Query(value = Queries.BETA_SELECT_FORCE_DROP_TABLES_RECORD, nativeQuery = true)
  DropTableStatus checkForceDeleteForDroppedTableEntryBeta(@Param("version") int version,
      @Param("changeSetId") int changeSetId);


  @Query(value = Queries.QA_SELECT_FORCE_DROP_TABLES_RECORD, nativeQuery = true)
  DropTableStatus checkForceDeleteForDroppedTableEntryQa(@Param("version") int version,
      @Param("changeSetId") int changeSetId);

  @Transactional
  @Modifying
  @Query(value = Queries.INSERT_FORCE_DROP_TABLES_RECORD_TO_QA_DROP_STATUS_TBL, nativeQuery = true)
  void insertQAForceDropStatusRecord(@Param("tableName") String tableName,
      @Param("version") int version,
      @Param("changeSetId") int changeSetId);


  @Transactional
  @Modifying
  @Query(value = Queries.UPDATE_FORCE_DROP_TABLES_RECORD_TO_QA_DROP_STATUS_TBL, nativeQuery = true)
  void updateQAForceDropStatusRecord(@Param("tableName") String tableName,
      @Param("version") int version,
      @Param("changeSetId") int changeSetId);

  @Transactional
  @Modifying
  @Query(value = Queries.OBSOLETE_FORCE_DROP_TABLES_RECORD_TO_QA_DROP_STATUS_TBL, nativeQuery = true)
  void obsoleteQAForceDropStatusRecord(@Param("version") int version,
      @Param("changeSetId") int changeSetId);


  @Query(value = Queries.PROD_SELECT_FORCE_DROP_TABLES_RECORD, nativeQuery = true)
  DropTableStatus checkForceDeleteForDroppedTableEntryProd(@Param("version") int version,
      @Param("changeSetId") int changeSetId);


  @Transactional
  @Modifying
  @Query(value = Queries.INSERT_FORCE_DROP_TABLES_RECORD_TO_PROD_DROP_STATUS_TBL, nativeQuery = true)
  void insertProdForceDropStatusRecord(@Param("tableName") String tableName,
      @Param("version") int version,
      @Param("changeSetId") int changeSetId);


  @Transactional
  @Modifying
  @Query(value = Queries.UPDATE_FORCE_DROP_TABLES_RECORD_TO_PROD_DROP_STATUS_TBL, nativeQuery = true)
  void updateProdForceDropStatusRecord(@Param("tableName") String tableName,
      @Param("version") int version,
      @Param("changeSetId") int changeSetId);

  @Transactional
  @Modifying
  @Query(value = Queries.OBSOLETE_FORCE_DROP_TABLES_RECORD_TO_PROD_DROP_STATUS_TBL, nativeQuery = true)
  void obsoleteProdForceDropStatusRecord(@Param("version") int version,
      @Param("changeSetId") int changeSetId);
}

