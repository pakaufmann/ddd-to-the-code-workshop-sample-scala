#!/bin/bash

podman pod create -p 8081:8081 -p 8082:8082 -p 8083:8083 --name example

docker run -dt --name accounting --pod example localhost/accounting:0.1.0-SNAPSHOT
docker run -dt --name rental --pod example localhost/rental:0.1.0-SNAPSHOT
docker run -dt --name registration --pod example localhost/registration:0.1.0-SNAPSHOT