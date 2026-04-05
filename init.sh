#!/bin/bash

echo "Building application..."
./mvnw package -DskipTests

echo "Starting infrastructure..."
docker compose up -d --build

echo "Waiting for MariaDB to be healthy..."
until docker exec mariadb mariadb -ucatalog -pcatalog -e "SELECT 1" > /dev/null 2>&1; do
  echo "MariaDB not ready yet, waiting..."
  sleep 3
done

echo "Granting Debezium required privileges..."
docker exec mariadb mariadb -uroot -proot -e "GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'catalog'@'%'; FLUSH PRIVILEGES;"

echo "Waiting for Kafka Connect to be healthy..."
until curl -s http://localhost:8083/connectors > /dev/null 2>&1; do
  echo "Kafka Connect not ready yet, waiting..."
  sleep 5
done

echo "Registering Debezium connector..."
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @debezium-connector.json

echo "Done. Infrastructure is ready."