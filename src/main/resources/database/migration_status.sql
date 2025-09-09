CREATE TABLE migration_status (
  version INT ,
  is_migrated BOOLEAN DEFAULT FALSE,
  created_by VARCHAR(50) DEFAULT 'dsm',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_by VARCHAR(50) DEFAULT 'dsm',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (version),
  FOREIGN KEY (version) REFERENCES databricks_metadata_beta.file_metadata(version)
);