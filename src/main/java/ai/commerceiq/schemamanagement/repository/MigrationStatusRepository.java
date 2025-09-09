package ai.commerceiq.schemamanagement.repository;

import ai.commerceiq.schemamanagement.model.entity.MigrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MigrationStatusRepository extends JpaRepository<MigrationStatus, Integer> {

}