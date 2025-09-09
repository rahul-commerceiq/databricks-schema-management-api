package ai.commerceiq.schemamanagement.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "migration_status")
public class MigrationStatus {

  @Id
  private int version;
  @Column(name = "is_migrated")
  private boolean isMigrated;
  @Column(name = "created_by")
  private String created_by;
  @Column(name = "updated_by")
  private String updated_by;

  public MigrationStatus() {
  }

  public MigrationStatus(int version, String userName, boolean isMigrated) {
    this.version = version;
    this.isMigrated = isMigrated;
    this.created_by = userName;
    this.updated_by = userName;
  }


}
