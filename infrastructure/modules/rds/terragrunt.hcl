#Include terragrunt.hcl files from parent folders
include "root" {
  path = find_in_parent_folders()
}

include "commons" {
  path = find_in_parent_folders("commons.hcl")
  expose = true
}

terraform {
  source = "git::ssh://bitbucket.org/commerceiq/terraform-modules.git//rds"
}

inputs = {
  apply_immediately       = true
  identifier              = "databricks-schema-management"
  storage_type            = "gp3"
  engine                  = "mysql"
  engine_version          = "8.0.35"
  major_engine_version    = "8.0"
  family                  = "mysql8.0"
  instance_class          = "db.t4g.small"
  allocated_storage       = 200
  max_allocated_storage   = 1000
  backup_retention_period = 7
  backup_window           = "00:00-00:30"
  maintenance_window      = "sun:05:00-sun:05:30"
  multi_az                = false
  publicly_accessible     = false
  vpc_id                  = "vpc-a237cac7"
  vpc_security_group_ids = ["sg-845cece1"]
  db_subnet_group_name    = "boomerang-private-subnet-group"
  parameter_group_name    = "dsm-mysql-8-0"
  parameter_group_family  = "mysql8.0"
  parameter_group_desc    = "default.mysql8.0"
  deletion_protection     = "true"
  parameters = [
    {
      name  = "max_connections"
      value = "250"
    }
  ]
  username                    = "dsm"
  password                    = "tR9tecuph3pr"
  manage_master_user_password = false
  env                         = include.commons.inputs.tags.env
  own                         = include.commons.inputs.tags.own
  product                     = include.commons.inputs.tags.product
  system                      = include.commons.inputs.tags.system
  stack                       = include.commons.inputs.tags.stack
  override_suffix             = true
}
