variable "region" {
  type    = string
  default = "ap-southeast-1"
}

variable "name" {
  type    = string
  default = "mini-core-transfer"
}

variable "app_image" {
  description = "Image cua capstone, vd <account>.dkr.ecr.ap-southeast-1.amazonaws.com/transfer:1.0.0"
  type        = string
}

variable "desired_count" {
  type    = number
  default = 2 # 2 task o 2 AZ: mat 1 AZ van con 1
}

variable "db_multi_az" {
  description = "true cho production (standby dong bo AZ khac). Lab de false cho re."
  type        = bool
  default     = false
}

variable "budget_usd" {
  type    = string
  default = "30"
}

variable "budget_email" {
  type = string
}
