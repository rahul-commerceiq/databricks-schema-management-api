package ai.commerceiq.schemamanagement.model.entity;

import ai.commerceiq.schemamanagement.model.response.QueryDetailsModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
@Table(name = "validation_metadata")
public class ValidationMetaData {

  @Id
  @Column(name = "version", nullable = false)
  private int version;
  @Column(name = "is_validated", nullable = false)
  private boolean isValidated;
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "queries", nullable = false, columnDefinition = "jsonb")
  private Map<Integer, String> queries;

  public ValidationMetaData() {
  }


  public ValidationMetaData(List<QueryDetailsModel> queriesList, int version) {
    this.queries = new HashMap<>();
    for (QueryDetailsModel queryDetail : queriesList) {
      this.queries.put(queryDetail.getId(), queryDetail.getQuery());
    }
    this.isValidated = true;
    this.version = version;
  }

}

