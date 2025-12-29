variable "project_id" {
  description = "Google Cloud Project ID"
  type        = string
}

variable "region" {
  description = "Google Cloud Region"
  type        = string
  default     = "us-central1"
}

variable "cockroach_api_key" {
  description = "API Key for CockroachDB Cloud"
  type        = string
  sensitive   = true
}

variable "db_password" {
  description = "Password for the database user"
  type        = string
  sensitive   = true
}

variable "service_token" {
  description = "Secret token for service-to-service auth (Frontend <-> Backend)"
  type        = string
  sensitive   = true
}