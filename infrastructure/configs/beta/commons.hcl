locals {
  env = "beta"
}

inputs = {
  env = local.env
  tags = {
    Created_by = get_aws_caller_identity_arn()
    Provisioned_by = "terraform"
    env            = local.env
    own            = "Core Platform"
    product        = "platform"
    system         = "databricks-schema-management-api"
    stack          = "dbx"
  }
}

