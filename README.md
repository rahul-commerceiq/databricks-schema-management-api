# README #

Databricks Schema Management API

### What is this repository for? ###

* To manage schema for Databricks.

### Version 0.0.1

## Added
* Validation of DML queries in Beta environment.
* Getting validated but non migrated files list.
* Executing the migration for validated files.
* Enforcing YAML structure for queries is a standard practice, with the exception of queries related to creating views.
* Generating queries dynamically out of yaml files.

## Removed
* N/A


## Limitations
* Not all kinds of DML (Data Manipulation Language) operations are supported.
* There is no validation supported in the production environment.
* Some dependency queries cannot be validated in a single call. For instance, the creation of a table and a corresponding view will fail during view validation if the table is not present.  
*Rollback of drop table queries needs human intervention.

## Further enhancements required
* Rollback for add column.
* Rollback for drop column.
* Validation in Prod environment.
* Extend the service to enable the execution of unsupported DML queries and the creation of functions.