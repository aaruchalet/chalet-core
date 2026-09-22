.PHONY: \
	mysql-up mysql-down mysql-start mysql-stop mysql-restart mysql-reset mysql-logs mysql-shell \
	mysql-temp-up mysql-temp-down mysql-temp-start mysql-temp-stop mysql-temp-restart mysql-temp-reset mysql-temp-logs mysql-temp-shell \
	app-up app-down app-start app-stop app-restart app-build app-logs \
	jenkins-up jenkins-down jenkins-start jenkins-stop jenkins-restart jenkins-reset jenkins-logs \
	stack-up stack-down stack-reset stack-status \
	build clean run-dev run-qa \
	k8s-mysql-up k8s-app-up k8s-up k8s-down k8s-logs

##################################################
# MySQL (Persistent)
##################################################

mysql-up:
	docker compose -p chalet up --build -d chalet-db

mysql-down:
	docker compose -p chalet stop chalet-db

mysql-start:
	docker compose -p chalet start chalet-db

mysql-stop:
	docker compose -p chalet stop chalet-db

mysql-restart:
	docker compose -p chalet restart chalet-db

mysql-reset:
	docker compose -p chalet down -v

mysql-logs:
	docker compose -p chalet logs -f chalet-db

mysql-shell:
	docker exec -it chalet-db mysql -uroot -proot chalet_db

##################################################
# MySQL (Temporary)
##################################################

mysql-temp-up:
	docker compose -p chalet --profile temp up --build -d chalet-db-temp

mysql-temp-down:
	docker compose -p chalet stop chalet-db-temp

mysql-temp-start:
	docker compose -p chalet start chalet-db-temp

mysql-temp-stop:
	docker compose -p chalet stop chalet-db-temp

mysql-temp-restart:
	docker compose -p chalet restart chalet-db-temp

mysql-temp-reset:
	docker compose -p chalet --profile temp down

mysql-temp-logs:
	docker compose -p chalet logs -f chalet-db-temp

mysql-temp-shell:
	docker exec -it chalet-db-temp mysql -uroot -proot chalet_db_tmp

##################################################
# Spring Boot Application
##################################################

app-up:
	docker compose -p chalet up --build -d chalet-core

app-down:
	docker compose -p chalet stop chalet-core

app-start:
	docker compose -p chalet start chalet-core

app-stop:
	docker compose -p chalet stop chalet-core

app-restart:
	docker compose -p chalet restart chalet-core

app-build:
	docker compose -p chalet build chalet-core

app-logs:
	docker compose -p chalet logs -f chalet-core

##################################################
# Jenkins
##################################################

jenkins-up:
	docker compose -p chalet --profile ci up -d jenkins

jenkins-down:
	docker compose -p chalet stop jenkins

jenkins-start:
	docker compose -p chalet start jenkins

jenkins-stop:
	docker compose -p chalet stop jenkins

jenkins-restart:
	docker compose -p chalet restart jenkins

jenkins-reset:
	docker compose -p chalet --profile ci down -v

jenkins-logs:
	docker compose -p chalet logs -f jenkins

##################################################
# Complete Stack
##################################################

stack-up:
	docker compose -p chalet --profile ci up --build -d

stack-down:
	docker compose -p chalet down

stack-reset:
	docker compose -p chalet down -v

stack-status:
	docker compose -p chalet ps

##################################################
# Gradle
##################################################

clean:
	./gradlew clean

build:
	./gradlew clean build

run-dev: mysql-up
	@echo "Starting Chalet Core with deterministic local datasource settings..."
	./gradlew bootRunDev

run-qa:
	./gradlew bootRun --args='--spring.profiles.active=qa'

##################################################
# Kubernetes
##################################################

k8s-mysql-up:
	kubectl apply -f k8s/mysql-pvc.yaml
	kubectl apply -f k8s/mysql-deployment.yaml
	kubectl apply -f k8s/mysql-service.yaml

k8s-app-up:
	kubectl apply -f k8s/configmap.yaml
	kubectl apply -f k8s/deployment.yaml
	kubectl apply -f k8s/service.yaml

k8s-up:
	kubectl apply -f k8s/configmap.yaml
	kubectl apply -f k8s/mysql-pvc.yaml
	kubectl apply -f k8s/mysql-deployment.yaml
	kubectl apply -f k8s/mysql-service.yaml
	kubectl apply -f k8s/deployment.yaml
	kubectl apply -f k8s/service.yaml

k8s-down:
	kubectl delete -f k8s/

k8s-logs:
	kubectl get deployments
	kubectl get pods
	kubectl get services