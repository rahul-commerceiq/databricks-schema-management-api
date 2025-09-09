# Databricks Schema Management API #

All notable changes for this service are logged in this file.

### 0.0.39

[CAT-01] HOTFIX : Fixed RDS password expiry issue.

### 0.0.38

[CAT-01] FEATURE: Added drop function,update function and update view DDLs.

### 0.0.37

[CAT-01] FEATURE: Added time travel properties for table creation DDL.

### 0.0.36

[CAT-01] FEATURE: Create table with multiple columns partition.

### 0.0.35

[CAT-01] FEATURE: Create normal view for non-client specific table.

### 0.0.34

[CAT-01] BUG: Return empty string instead of null from getQueryType func.

### 0.0.33

[CAT-01] BUG: clean up entire tables s3 path folder.

### 0.0.32

[CAT-01] Recreate view on altering the table's schema.

### 0.0.31

[CAT-01] Updated docker image to enable NewRelic APM monitoring.

### 0.0.30

[CAT-01] Updated the query to select non migrated files based on env.

### 0.0.29

[CAT-01] Bug fix to qa application properties on s3 uri.

### 0.0.28

[CAT-01] Force drop table Feature: Handle possible known edge cases.

### 0.0.27

[CAT-01] Force drop table feature.

### 0.0.26

[CAT-01] appended dbx to environment based conditions.

### 0.0.25

[CAT-01] Added AWS STS library

### 0.0.24

[CAT-01] Added support for default column values.

### 0.0.23

[CAT-01] Added support for Auto increment for custom and default values.

### 0.0.22

[CAT-01] Bug fix to qa application properties on s3 uri.

### 0.0.21

[CAT-01] Bug fix to update migration_status DB version.

### 0.0.20

[CAT-01] Autoincrement implementation for LONG and BIGINT column types.

### 0.0.19

[CAT-01] Executing all the migration queries and raise PD for failed queries.

### 0.0.18

[CAT-01] Delete dynamic view at common view catalog

### 0.0.17

[CAT-01] S3 path bug fix.

### 0.0.16

[CAT-01] Bug fix for create schema.

### 0.0.15

[CAT-01] Rewrite the query to check all the files are migrated in lower environment before migrating
into prod.

### 0.0.14

[CAT-01] Handle DBX get location API's bad request response.

### 0.0.13

[CAT-01] Comment deploy infra step for master branch in bitbucket pipeline.

### 0.0.12

[CAT-01] Increase the RDS instance version to 8.0.35 from 8.0.33.

### 0.0.11

[CAT-01] Fixed TF RDS issue.

### 0.0.10

[CAT-01] Create schema at view catalogs

### 0.0.9

[CAT-01] BUG fix Reading location from properties files.

### 0.0.8

[CAT-01] Column object validation for mandatory fields in CREATE_TABLE.

### 0.0.7

[CAT-01] BUG fix, On absence of Nullable field, value should be set to true.

### 0.0.6

[CAT-01] Added drop-tables api.

### 0.0.5

[CAT-01] BUG fix, create schema in view catalogs along with normal catalog.

### 0.0.4

[CAT-01] Integrated Pager Duty.

### 0.0.3

[CAT-01] Files should be merged to Master branch of DMS for migration.

### 0.0.2

[CAT-01] Initial cut of Databricks Schema Management API.

### 0.0.1

[CAT-01] Added IaC for Databricks Schema Management API