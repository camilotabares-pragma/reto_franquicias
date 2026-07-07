# Salida con la URL pública del balanceador
output "url_publica_alb" {
  value       = aws_lb.main.dns_name
  description = "URL pública del balanceador"
}
