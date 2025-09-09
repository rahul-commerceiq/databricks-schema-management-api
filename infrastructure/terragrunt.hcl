locals {
  common = read_terragrunt_config("${get_path_to_repo_root()}/infrastructure/commons.hcl")
}


generate "backend" {
  path      = "backend.tf"
  if_exists = "overwrite_terragrunt"
  contents  = <<EOF
    terraform {
      backend "s3" {
        bucket         = "ciq-uswe2-terraform-state-store"
        key            = "ciq/${get_env("REPO_NAME")}/${path_relative_to_include()}/${local.common.locals.env}/terraform.tfstate"
        region         = "${get_env("AWS_DEFAULT_REGION")}"
        encrypt        = true
        dynamodb_table = "terraform_state_lock"
      }
    }
    EOF
}

generate "provider" {
  path      = "provider.tf"
  if_exists = "overwrite_terragrunt"
  contents  = <<EOF
    provider "aws" {
      region = "us-west-2"
    }
    EOF
}
