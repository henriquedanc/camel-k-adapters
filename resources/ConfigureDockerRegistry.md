# Private Docker registry deployment

You only need to do this if you don't have an available registry that can be used.
This is a quick way of creating an insecure registry, and should be used only for test purposes.
This assumes that your K8S cluster is up and running.

- In your K8S cluster, copy the required ffiles

# create directory to store Camel definitions
mkdir -p /opt/sas/resources
cd /opt/sas/resources
```
- Copy the following files nto the resources directory above
-- [docker-registry-pv-volume.yaml](docker-registry-pv-volume.yaml)
-- [docker-registry-pv-claim.yaml](docker-registry-pv-claim.yaml)
-- [docker-registry-deployment.yaml](docker-registry-deployment.yaml)

- create resources
```
# if you have dynamic volume provisioning enabled in the cluster, you don't need to create the volume.
# just change the class in the PVC definition and create it.
kubectl apply -f docker-registry-pv-volume.yaml
kubectl apply -f docker-registry-pv-claim.yaml
kubectl apply -f docker-registry-pv-deployment.yaml

kubectl expose deployment docker-registry --type=ClusterIP --port=5000 --target-port=5000 --name=docker-registry
# get service IP
kubectl describe svc docker-registry
```

- edit docker daemon settings (on all cluster nodes) to allow pulling images from our insecure repo, and restart it
```
# edit the /etc/docker/daemon.json file, adding the following line
# "insecure-registries" : ["REGISTRY_SERVICE_IP:5000"]
sudo systemctl restart docker
```