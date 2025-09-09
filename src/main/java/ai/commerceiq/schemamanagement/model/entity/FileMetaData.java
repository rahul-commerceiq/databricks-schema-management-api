package ai.commerceiq.schemamanagement.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "file_metadata")
public class FileMetaData {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int version;
  @Column(name = "absolute_file_path", nullable = false)
  private String absoluteFilePath;
  @Column(name = "file_name", nullable = false, unique = true)
  private String fileName;
  @Column(name = "checksum", nullable = false)
  private String checksum;
  @Column(name = "created_by", nullable = false)
  private String created_by;
  @Column(name = "updated_by", nullable = false)
  private String updated_by;

  public FileMetaData() {
  }


}
