# ==========================================
# 1. COCKROACHDB SERVERLESS (The Database)
# ==========================================

resource "cockroach_cluster" "db" {
  name           = "trading-cluster"
  cloud_provider = "GCP"

  serverless = {
    usage_limits = {
      request_unit_limit = 10000000 # Monthly request unit limit
    }
  }

  regions = [{
    name = var.region
  }]
}

resource "cockroach_sql_user" "app_user" {
  name       = "trading_user"
  password   = var.db_password
  cluster_id = cockroach_cluster.db.id
}

resource "cockroach_database" "trading_db" {
  name       = "trading_db"
  cluster_id = cockroach_cluster.db.id
}

# ==========================================
# 2. GOOGLE CLOUD RUN (The Backend)
# ==========================================

# Enable Artifact Registry API
resource "google_project_service" "artifact_registry_api" {
  service = "artifactregistry.googleapis.com"
  disable_on_destroy = false
}

# Create Docker Repository
resource "google_artifact_registry_repository" "repo" {
  location      = var.region
  repository_id = "trading-backend-repo"
  format        = "DOCKER"
  depends_on    = [google_project_service.artifact_registry_api]
}

# Deploy Cloud Run Service
resource "google_cloud_run_service" "backend" {
  name     = "trading-backend-service"
  location = var.region

  template {
    metadata {
      annotations = {
        # Scale to Zero (Cost Saving)
        "autoscaling.knative.dev/minScale" = "0"
        # Max Limit (Cost Safety)
        "autoscaling.knative.dev/maxScale" = "1"
      }
    }

    spec {
      containers {
        # Image will be built/pushed by GitHub Actions
        image = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.repo.repository_id}/trading-app:latest"

        # Inject Dynamic DB Credentials from Cockroach Resources
        env {
          name  = "SPRING_DATASOURCE_URL"
          # Construct the JDBC URL dynamically: jdbc:postgresql://{host}:{port}/{database}?sslmode=verify-full
          value = "jdbc:postgresql://${cockroach_cluster.db.regions[0].sql_dns}:26257/${cockroach_database.trading_db.name}?sslmode=verify-full"
        }
        env {
          name  = "SPRING_DATASOURCE_USERNAME"
          value = "${cockroach_sql_user.app_user.name}"
        }
        env {
          name  = "SPRING_DATASOURCE_PASSWORD"
          value = var.db_password
        }

        # === SECURITY CONFIGURATION ===
        env {
          name = "APP_SERVICE_TOKEN"
          value = var.service_token
        }

        # Inject CORS Origin
        env {
          name = "APP_CORS_ALLOWED_ORIGINS"
          value = var.app_cors_allowed_origins
        }
      }
    }
  }

  traffic {
    percent         = 100
    latest_revision = true
  }
}

# Make Service Network-Public (Application Security handles Auth)
data "google_iam_policy" "noauth" {
  binding {
    role = "roles/run.invoker"
    members = ["allUsers"]
  }
}

resource "google_cloud_run_service_iam_policy" "noauth" {
  location    = google_cloud_run_service.backend.location
  project     = google_cloud_run_service.backend.project
  service     = google_cloud_run_service.backend.name
  policy_data = data.google_iam_policy.noauth.policy_data
}