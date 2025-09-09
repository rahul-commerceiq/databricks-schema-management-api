CREATE TABLE validation_metadata (
    queries JSON NOT NULL,
    version INT PRIMARY KEY,
    is_validated BOOLEAN NOT NULL,
    created_by VARCHAR(50) DEFAULT 'dsm',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) DEFAULT 'dsm',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (version) REFERENCES databricks_metadata_beta.file_metadata(version)
);