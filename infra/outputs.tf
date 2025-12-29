output "cloud_run_url" {
  description = "The public URL of the Cloud Run service"
  value       = google_cloud_run_service.backend.status[0].url
}

output "db_connection_string" {
  description = "The JDBC connection string for the database (Sensitive)"
  value       = "jdbc:postgresql://${cockroach_cluster.db.regions[0].sql_dns}:26257/${cockroach_database.trading_db.name}?sslmode=verify-full"
  sensitive   = true
}