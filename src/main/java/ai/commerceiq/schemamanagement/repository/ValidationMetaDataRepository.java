package ai.commerceiq.schemamanagement.repository;

import ai.commerceiq.schemamanagement.model.entity.ValidationMetaData;
import ai.commerceiq.schemamanagement.utils.StringConstants.Queries;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ValidationMetaDataRepository extends JpaRepository<ValidationMetaData, Integer> {

  @Query(value = Queries.SELECT_QUERIES_AND_VERSION_BY_PATHS, nativeQuery = true)
  List<ValidationMetaData> findQueriesAndVersionByAbsoluteFilePaths(
      @Param("absoluteFilePaths") List<String> absoluteFilePaths);

}

