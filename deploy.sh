#!/bin/bash

# create Resource group
az group create --name rental-example --location switzerlandnorth

# create service bus
az servicebus namespace create --name rentalBus --resource-group rental-example --sku Standard

az servicebus topic create --resource-group rental-example --namespace-name rentalBus --name accounting~wallet-initialized
az servicebus topic create --resource-group rental-example --namespace-name rentalBus --name registration~user-registration-completed
az servicebus topic create --resource-group rental-example --namespace-name rentalBus --name rental~booking-completed
az servicebus topic create --resource-group rental-example --namespace-name rentalBus --name registration~phone-number-verification-code-generated

az servicebus topic subscription create --resource-group rental-example --namespace-name rentalBus --topic-name registration~phone-number-verification-code-generated --name registration

az servicebus topic subscription create --resource-group rental-example --namespace-name rentalBus --topic-name rental~booking-completed --name accounting
az servicebus topic subscription create --resource-group rental-example --namespace-name rentalBus --topic-name registration~user-registration-completed --name accounting

az servicebus topic subscription create --resource-group rental-example --namespace-name rentalBus --topic-name registration~user-registration-completed --name rental
az servicebus topic subscription create --resource-group rental-example --namespace-name rentalBus --topic-name rental~booking-completed --name rental

# create SQL databases
MY_IP=$(curl http://whatismyip.akamai.com/)
az sql server create --name rental-example --resource-group rental-example --location switzerlandnorth --admin-user rental --admin-password r3ntalPw
az sql server firewall-rule create --resource-group rental-example --server rental-example -n AllowYourIp --start-ip-address=$MY_IP --end-ip-address=$MY_IP
az sql server firewall-rule create --resource-group rental-example --server rental-example -n AllowAzureServices --start-ip-address=0.0.0.0 --end-ip-address=0.0.0.0

az sql db create --resource-group rental-example --server rental-example --name rental
az sql db create --resource-group rental-example --server rental-example --name accounting
az sql db create --resource-group rental-example --server rental-example --name registration

# create container registry
az acr create --resource-group rental-example --name rentalRegistry --sku Basic

echo "Im UI unter Container Registries > Access Keys > Admin User enablen und das Passwort unten einsetzen"
read DOCKER_PASSWORD

docker login rentalregistry.azurecr.io -u rentalRegistry -p $DOCKER_PASSWORD

# create Kubernetes cluster
az aks create --resource-group rental-example --name rental-cluster --node-count 2 --generate-ssh-keys --attach-acr rentalRegistry
az aks get-credentials --resource-group rental-example --name rental-cluster --overwrite-existing

# build and push images to container registry
sbt docker:publishLocal

docker tag localhost/registration:0.1.0-SNAPSHOT rentalregistry.azurecr.io/registration:0.1.0-SNAPSHOT
docker push rentalregistry.azurecr.io/registration:0.1.0-SNAPSHOT

docker tag localhost/rental:0.1.0-SNAPSHOT rentalregistry.azurecr.io/rental:0.1.0-SNAPSHOT
docker push rentalregistry.azurecr.io/rental:0.1.0-SNAPSHOT

docker tag localhost/accounting:0.1.0-SNAPSHOT rentalregistry.azurecr.io/accounting:0.1.0-SNAPSHOT
docker push rentalregistry.azurecr.io/accounting:0.1.0-SNAPSHOT

# deploy services
kubectl apply -f registration/registration.yaml
kubectl apply -f accounting/accounting.yaml
kubectl apply -f rental/rental.yaml
