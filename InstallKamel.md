# Installing Apache Camel in Kubernetes

This assumes that the Kubernetes cluster is already up and running, and kubectl can successfully communicate with the cluster.

- Get kamel binary, untar it

```
# create target directory for kamel
sudo mkdir -p /opt/sas/kamel

# download and uncompress kamel
cd /tmp
wget https://github.com/apache/camel-k/releases/download/v1.4.0/camel-k-client-1.4.0-linux-64bit.tar.gz
tar -zxvf camel-k-client-1.4.0-linux-64bit.tar.gz -C /opt/sas/kamel

# put the kamel bin directory in the PATH
export PATH=$PATH:/opt/sas/kamel
```

- Install camel-k in kubernetes

For this step, we will registry information to be able to push/pull images to/from it
Below is an example, but all the following options can be used

      --registry string                     A Docker registry that can be used to publish images
      --registry-auth-file string           A docker registry configuration file containing authorization tokens for pushing and pulling images
      --registry-auth-password string       The docker registry authentication password
      --registry-auth-server string         The docker registry authentication server
      --registry-auth-username string       The docker registry authentication username
      --registry-insecure                   Configure to configure registry access in insecure mode or not
      --registry-secret string              A secret used to push/pull images to the Docker registry


```
# example, specify you specific registry info
kamel install --registry HOST:PORT --registry-auth-username USER --registry-auth-password PASS
```

- run a simple integration

```
# create directory to store Camel definitions
mkdir -p /opt/sas/routes
cd /opt/sas/routes

# create a (very) simple route
cat <<EOF | sudo tee Sample.java
import org.apache.camel.builder.RouteBuilder;

public class Sample extends RouteBuilder {
  @Override
  public void configure() throws Exception {
       from("timer:tick")
        .log("Hello Camel K!");
  }
}
EOF

# run it
kamel run Sample.java

# get status
# will start as Building and then Running
kamel get sample

# get pod name
sample_pod_name=$(kubectl get pods --selector=camel.apache.org/integration=sample -o jsonpath='{.items[0].metadata.name}')
echo $sample_pod_name

# check the log, there should be a new message every second
kubectl logs $sample_pod_name -f

```
