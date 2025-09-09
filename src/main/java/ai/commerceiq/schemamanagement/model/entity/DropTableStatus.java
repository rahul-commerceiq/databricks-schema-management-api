package ai.commerceiq.schemamanagement.model.entity;

import ai.commerceiq.schemamanagement.utils.StringConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "databricks_drop_table_status")
public class DropTableStatus {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "request_id")
  private int requestId;
  @Column(name = "table_name", nullable = false)
  private String tableName;
  @Column(name = "status", nullable = false)
  private String status;
  @Column(name = "location")
  private String location;
  @Column(name = "version", nullable = false)
  private int version;
  @Column(name = "change_set_id", nullable = false)
  private int changeSetId;
  @Column(name = "force_delete", columnDefinition = "BOOLEAN DEFAULT FALSE")
  private boolean forceDelete = false; // default value false

  public DropTableStatus(String tableName, String location, int version, int changeSetId) {
    this.tableName = tableName;
    this.version = version;
    this.location = location;
    this.status = StringConstants.DROP_TABLE_STATUS_QUEUED;
    this.changeSetId = changeSetId;
  }

  public DropTableStatus(String tableName, int version, int changeSetId, boolean forceDelete) {
    this.tableName = tableName;
    this.version = version;
    this.status = StringConstants.DROP_TABLE_STATUS_QUEUED;
    this.changeSetId = changeSetId;
    this.forceDelete = forceDelete;
  }

}

