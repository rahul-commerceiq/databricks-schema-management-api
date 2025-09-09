CREATE TABLE file_metadata (
  absolute_file_path varchar(500) NOT NULL,
  file_name varchar(50) NOT NULL,
  checksum varchar(50) NOT NULL,
  version int  AUTO_INCREMENT,
  created_by varchar(50) DEFAULT 'dsm',
  created_at timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by varchar(50) DEFAULT 'dsm',
  updated_at timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (version),
  UNIQUE KEY absolute_file_path (absolute_file_path)
) 