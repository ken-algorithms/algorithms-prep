resource "aws_db_subnet_group" "main" {
  name       = "${var.name}-db"
  subnet_ids = aws_subnet.private[*].id
}

resource "aws_db_instance" "main" {
  identifier                  = "${var.name}-db"
  engine                      = "postgres"
  engine_version              = "16"
  instance_class              = "db.t4g.medium"
  allocated_storage           = 50
  db_name                     = "transfer"
  username                    = "transfer_admin"
  manage_master_user_password = true # mat khau nam trong Secrets Manager, khong nam trong code hay state
  db_subnet_group_name        = aws_db_subnet_group.main.name
  vpc_security_group_ids      = [aws_security_group.db.id]
  multi_az                    = var.db_multi_az
  storage_encrypted           = true
  backup_retention_period     = 7
  deletion_protection         = false # lab: de xoa duoc. Production: true
  skip_final_snapshot         = true  # lab. Production: false + final_snapshot_identifier
}

# Bai hoc tuan 15-16: tao budget TRUOC khi tao bat cu thu gi ton tien.
resource "aws_budgets_budget" "monthly" {
  name         = "${var.name}-monthly"
  budget_type  = "COST"
  limit_amount = var.budget_usd
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 80
    threshold_type             = "PERCENTAGE"
    notification_type          = "FORECASTED"
    subscriber_email_addresses = [var.budget_email]
  }
}

output "alb_dns_name" {
  value = aws_lb.main.dns_name
}

output "db_master_secret_arn" {
  value = aws_db_instance.main.master_user_secret[0].secret_arn
}
