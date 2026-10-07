# Khung tham khao cho capstone tuan 15-16. Da `terraform validate`, CHUA apply len AWS that.
terraform {
  required_version = ">= 1.10"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
  # State dung chung cho ca nhom: S3 + khoa bang lockfile (Terraform 1.10+), khong can bang DynamoDB.
  # backend "s3" {
  #   bucket       = "my-tfstate-bucket"
  #   key          = "capstone/terraform.tfstate"
  #   region       = "ap-southeast-1"
  #   use_lockfile = true
  #   encrypt      = true
  # }
}

provider "aws" {
  region = var.region
  default_tags {
    tags = { Project = var.name, ManagedBy = "terraform" }
  }
}
