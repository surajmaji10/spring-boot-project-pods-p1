
docker run -p 8080:8080 --rm --name app1 --add-host=host.docker.internal:host-gateway account-service:latest &

docker run -p 8081:8080 --rm --name app2 --add-host=host.docker.internal:host-gateway wallet-service:latest &

docker run -p 8082:8080 --rm --name app3 --add-host=host.docker.internal:host-gateway marketplace-service:latest &




