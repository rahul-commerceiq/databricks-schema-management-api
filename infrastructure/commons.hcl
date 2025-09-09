locals {
  env = "prod"
}

inputs = {
  env = local.env
  tags = {
    Created_by = get_aws_caller_identity_arn()
    Provisioned_by = "terraform"
    env            = local.env
    client         = "ALL"
    org            = "CIQ"
    own            = "platform"
    pl_ciq_adv     = "false"
    pl_ciq_data    = "false"
    pl_ciq_ms      = "false"
    pl_ciq_sales   = "false"
    product        = "platform"
    system         = "databricks-schema-management-api"
    team           = "Core Platform"
    stack          = "dbx"
  }
}