package ai.commerceiq.schemamanagement.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.when;

import ai.commerceiq.schemamanagement.model.response.FileValidationResultModel;
import ai.commerceiq.schemamanagement.model.response.ValidateApiResponse;
import ai.commerceiq.schemamanagement.utils.MysqlConnectionUtils;
import ai.commerceiq.schemamanagement.utils.YamlFileProcessingAndValidationUtil;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

  @Mock
  private DSMRepoCloningService dsmRepoCloningService;
  @Mock
  private MysqlConnectionUtils mysqlConnectionUtils;
  @Mock
  private YamlFileProcessingAndValidationUtil yamlFileProcessingAndValidationUtil;

  @InjectMocks
  private ValidationService validationService;

  @Test
  void testValidateFiles() {
    String branchName = "testBranch";
    String userName = "testUser";

    when(dsmRepoCloningService.cloneRepoAndGenerateFileChecksums(branchName))
        .thenReturn(Map.of(Paths.get("2024_02_19__create_one.yaml"), "checksumone",
            Paths.get("2024_02_19__create_two.yaml"), "checksumtwo"));

    when(mysqlConnectionUtils.filterValidatedNonMigratedFiles(anyList()))
        .thenReturn(Map.of(Paths.get("2024_02_19__create_one.yaml"), "checksumone"));

    when(yamlFileProcessingAndValidationUtil.processAndValidateFiles(anyList()))
        .thenReturn(List.of(new FileValidationResultModel()));

    assertDoesNotThrow(() -> validationService.validateFiles(branchName, userName));
  }

  @Test
  void testBuildValidationResponse() {
    List<FileValidationResultModel> validationResults = List.of(new FileValidationResultModel());
    String userName = "testUser";
    boolean isOverAllValidationSuccessful = true;

    ValidateApiResponse response = validationService.buildValidationResponse(validationResults,
        userName, isOverAllValidationSuccessful);

    assertEquals(userName, response.getValidatedBy());
    assertEquals(validationResults, response.getDetails());
    assertEquals(isOverAllValidationSuccessful ? "Success" : "FAILED", response.getStatus());
  }
}
