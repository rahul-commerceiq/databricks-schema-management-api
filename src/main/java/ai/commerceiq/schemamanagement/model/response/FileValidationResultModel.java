package ai.commerceiq.schemamanagement.model.response;

import ai.commerceiq.schemamanagement.model.general.FilePathAndChecksumEntity;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FileValidationResultModel {

  List<QueryDetailsModel> queryList;
  String filePath;
  String fileName;
  String checksum;
  String Result;
  String Error;
  String fileDdlType;


  public FileValidationResultModel(FilePathAndChecksumEntity filePathAndChecksumEntity) {
    this.filePath = filePathAndChecksumEntity.getFilePath().toString();
    this.fileName = filePathAndChecksumEntity.getFileName();
    this.checksum = filePathAndChecksumEntity.getChecksum();
  }


}
