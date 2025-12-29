terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 4.0"
    }
    cockroach = {
      source  = "cockroachdb/cockroach"
      version = "~> 1.0"
    }
  }

  # The configuration for the GCS backend is intentionally partial here.
  # The 'bucket' parameter is passed dynamically during 'terraform init' in GitHub Actions.
  # This makes the code reusable across different environments/projects.
  backend "gcs" {
    prefix  = "trade-event-processing-pipeline/state"
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

provider "cockroach" {
  # API Key is passed via the environment variable TF_VAR_cockroach_api_key
  apikey = var.cockroach_api_key
}