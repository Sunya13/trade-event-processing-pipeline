output "backend_url" {
  value = digitalocean_app.backend.default_ingress
}

output "db_host" {
  value = digitalocean_database_cluster.postgres.host
  sensitive = true
}