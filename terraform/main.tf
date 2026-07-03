# 1. EL BLOQUE TERRAFORM: Le decimos qué herramientas necesitamos descargar
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# 2. EL PROVIDER: Le decimos en qué región de AWS vamos a trabajar
provider "aws" {
  region = "us-east-1"
}

# 3. EL RECURSO: ¡Aquí está la magia! Le pedimos que cree un repositorio ECR
resource "aws_ecr_repository" "mi_repositorio" {
  name                 = "franquicias-api-repo" # El nombre que tendrá en AWS
  image_tag_mutability = "MUTABLE"              # Permite sobreescribir imágenes con la etiqueta "latest"

  force_delete = true                           # Si algún día destruimos la infra, que borre el repo aunque tenga imágenes dentro
}

# Crear la red principal
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  tags = { Name = "franquicias-vpc" }
}

# Subred Pública 1 (Zona A)
resource "aws_subnet" "public_a" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.1.0/24"
  availability_zone = "us-east-1a"
  map_public_ip_on_launch = true
  tags = { Name = "franquicias-pub-a" }
}

# Subred Pública 2 (Zona B)
resource "aws_subnet" "public_b" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.2.0/24"
  availability_zone = "us-east-1b"
  map_public_ip_on_launch = true
  tags = { Name = "franquicias-pub-b" }
}

# El "Internet Gateway" para que la red tenga salida a internet
resource "aws_internet_gateway" "gw" {
  vpc_id = aws_vpc.main.id
  tags   = { Name = "franquicias-igw" }
}

# Tabla de enrutamiento para decirle a las subredes cómo salir a internet
resource "aws_route_table" "rt" {
  vpc_id = aws_vpc.main.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.gw.id
  }
}

resource "aws_route_table_association" "a" {
  subnet_id      = aws_subnet.public_a.id
  route_table_id = aws_route_table.rt.id
}

resource "aws_route_table_association" "b" {
  subnet_id      = aws_subnet.public_b.id
  route_table_id = aws_route_table.rt.id
}

resource "aws_security_group" "ecs_sg" {
  name        = "franquicias-ecs-sg"
  description = "Permitir trafico a la API"
  vpc_id      = aws_vpc.main.id

  # Permitir entrada en el puerto 8080 (API)
  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Permitir entrada en el puerto 80 (Para el Balanceador de carga)
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Permitir CUALQUIER salida (para que la API pueda hablar con Mongo Atlas en internet)
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# El clúster lógico de ECS
resource "aws_ecs_cluster" "main" {
  name = "franquicias-cluster"
}

# La definición de la tarea (El plano de cuánta RAM y CPU queremos)
resource "aws_ecs_task_definition" "app" {
  family                   = "franquicias-api-task"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"  # 0.25 vCPU (Lo mínimo y más barato)
  memory                   = "1024"  # 512 MB RAM
  execution_role_arn       = aws_iam_role.ecs_execution_role.arn

  # Definición del contenedor de Docker
  container_definitions = jsonencode([{
    name      = "franchise-api"
    image     = "367553824468.dkr.ecr.us-east-1.amazonaws.com/franquicias-api-repo:latest"
    essential = true
    portMappings = [{
      containerPort = 8080
      hostPort      = 8080
    }]
    # VARIABLES DE ENTORNO: Aquí le inyectas la conexión a Mongo Atlas
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

# El Balanceador de Carga que recibirá las peticiones de internet
resource "aws_lb" "main" {
  name               = "franquicias-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.ecs_sg.id]
  subnets            = [aws_subnet.public_a.id, aws_subnet.public_b.id]
}

# El Target Group: Le dice al balanceador a qué puerto redirigir el tráfico interno
resource "aws_lb_target_group" "app" {
  name        = "franquicias-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    path                = "/actuator/health" # O la ruta de tu Swagger si no tienes Actuator
    healthy_threshold   = 3
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 30
    matcher             = "200"
  }
}

# El oyente (Listener): Escucha en el puerto 80 público y lo manda al Target Group
resource "aws_lb_listener" "front_end" {
  load_balancer_arn = aws_lb.main.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }
}

# El Servicio ECS: El encargado de mantener el contenedor vivo y conectado al balanceador
resource "aws_ecs_service" "main" {
  name            = "franquicias-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = 1 # Queremos solo 1 copia corriendo para no gastar dinero
  launch_type     = "FARGATE"

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

# OUTPUT: Terraform nos imprimirá en pantalla la URL del balanceador cuando termine
output "url_publica_alb" {
  value       = aws_lb.main.dns_name
  description = "Usa esta URL para probar tus endpoints en Postman"
}

# Crear el rol de ejecución para ECS Task
resource "aws_iam_role" "ecs_execution_role" {
  name = "franquicias-ecs-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })
}

# Adjuntar la política oficial de AWS para que pueda leer de ECR y escribir Logs
resource "aws_iam_role_policy_attachment" "ecs_execution" {
  role       = aws_iam_role.ecs_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Crear un grupo de logs en CloudWatch
resource "aws_cloudwatch_log_group" "api_logs" {
  name              = "/ecs/franquicias-api"
  retention_in_days = 7 # Borra los logs antiguos después de una semana para no cobrarte de más
}