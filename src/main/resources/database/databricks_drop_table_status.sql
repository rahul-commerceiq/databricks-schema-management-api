CREATE TABLE databricks_drop_table_status (
  request_id INT AUTO_INCREMENT PRIMARY KEY,
  table_name VARCHAR(255) NOT NULL,
  location VARCHAR(1000),
  status ENUM('deleted', 'inProgress', 'queued','skipped','obsolete') NOT NULL,
  version INT NOT NULL,
  force_delete TINYINT(1) DEFAULT '0',
  change_set_id INT NOT NULL,
  created_by VARCHAR(50) DEFAULT 'dsm',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_by VARCHAR(50) DEFAULT 'dsm',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (version) REFERENCES databricks_metadata_beta.file_metadata(version)
);

