# Recurso para el repositorio ECR
resource "aws_ecr_repository" "mi_repositorio" {
  name                 = "franquicias-api-repo" # Nombre del repositorio en AWS
  image_tag_mutability = "MUTABLE"              # Permite actualizar etiquetas de imagen

  force_delete = true                           # Elimina el repositorio junto con su contenido
}

# Clúster de ECS
resource "aws_ecs_cluster" "main" {
  name = "franquicias-cluster"
}

# Definición de la tarea ECS
resource "aws_ecs_task_definition" "app" {
  family                   = "franquicias-api-task"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"  # 0.25 vCPU
  memory                   = "1024"  # 512 MB de RAM
  execution_role_arn       = aws_iam_role.ecs_execution_role.arn

  # Definición del contenedor
  container_definitions = jsonencode([{
    name      = "franchise-api"
    image     = "367553824468.dkr.ecr.us-east-1.amazonaws.com/franquicias-api-repo:latest"
    essential = true
    portMappings = [{
      containerPort = 8080
      hostPort      = 8080
    }]
    # Variables de entorno
    environment = [
      { name = "SPRING_DATA_MONGODB_URI", value = "mongodb+srv://camilotabares_db_user:zR2MHeDZRUSrxaZQ@cluster0.9idr2vp.mongodb.net/franchise_db?appName=Cluster0" }
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = "/ecs/franquicias-api"
        "awslogs-region"        = "us-east-1"
        "awslogs-stream-prefix" = "ecs"
      }
    }
  }])
}

# Servicio ECS
resource "aws_ecs_service" "main" {
  name            = "franquicias-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = 1 # Cantidad de tareas deseadas
  launch_type     = "FARGATE"

  health_check_grace_period_seconds = 120

  network_configuration {
    subnets          = [aws_subnet.public_a.id, aws_subnet.public_b.id]
    security_groups  = [aws_security_group.ecs_sg.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.app.arn
    container_name   = "franchise-api"
    container_port   = 8080
  }

  depends_on = [aws_lb_listener.front_end]
}

# Grupo de logs en CloudWatch
resource "aws_cloudwatch_log_group" "api_logs" {
  name              = "/ecs/franquicias-api"
  retention_in_days = 7 # Retención de logs en días
}
