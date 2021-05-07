# Deploying Json Socket Adapter

This assumes that the Kubernetes cluster is already up and running, and kubectl and kamel can successfully communicate with the cluster.
This integration uses a kafka-config configmap that was created in a previous step. If you didn't do it, go back and create that now

- Create the integration configuration

```
# create directory to store Camel definitions
mkdir -p /opt/sas/routes
cd /opt/sas/routes
```

- Copy the [JsonSocket.java](routes/JsonSocket.java) file into the routes directory above

- Run the integration

```
# if node-affitity is needed, enable its trait by setting:
#  --trait affinity.enabled=true 
# and specify the label 
#  --trait affinity.node-affinity-labels=netspeed=40g
# we used netspeed=40g as an example, but could be anything

# if toleration is needed, change it below
# --trait toleration.taints="netspeed=40g:NoSchedule"
# we used netspeed=40g:NoSchedule as an example

# run it
kamel run -d mvn:javax.xml.bind:jaxb-api:2.3.1 -d mvn:com.sun.xml.bind:jaxb-core:2.3.0.1 -d mvn:com.sun.xml.bind:jaxb-impl:2.3.2 -d camel-kafka --trait affinity.enabled=false --trait affinity.node-affinity-labels=netspeed=40g --trait toleration.enabled=true --trait toleration.taints="netspeed=40g:NoSchedule" --configmap=kafka-config --trait jvm.options="-Xms5632m -Xmx5632m" --trait container.request-cpu=4000m --trait container.request-memory=6144Mi JsonSocket.java

# get status
kamel get json-socket

# get pod name
json_socket_pod_name=$(kubectl get pods --selector=camel.apache.org/integration=json-socket -o jsonpath='{.items[0].metadata.name}')
echo $json_socket_pod_name

# check the logs
kubectl logs $json_socket_pod_name -f

# scale the deployment (start with 10 replicas)
kubectl scale it json-socket --replicas=10

# expose the deployment
# we are using nodeport as an example, but can be different
kubectl expose deployment json-socket --port 5514 --protocol TCP --type NodePort

# get service info
kubectl describe svc json-socket
```

- In the kafka broker, subscribe to the json-data topic
```
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic json-data --group test-consumer
```

- Now publish some data into the socket  
  If you used NodePort service type above, you can use any node as the target. Just use the node port you got from the service description.
```
NODE=lab-1
NODEPORT=31208
while (true);
do
 echo "{\"field1\": \"foo\", \"field2\": \"bar\", \"field3\": \"`date +'%Y-%m-%dT%H:%M:%S.%s'`\"}" | netcat -N $NODE $NODEPORT;
done
```

- You should receive the messages in the kafka-console-consumer.sh terminal
```
{"field1": "foo", "field2": "bar", "field3": "2021-05-05T18:48:39.1620240519"}
{"field1": "foo", "field2": "bar", "field3": "2021-05-05T18:48:39.1620240519"}
{"field1": "foo", "field2": "bar", "field3": "2021-05-05T18:48:39.1620240519"}
{"field1": "foo", "field2": "bar", "field3": "2021-05-05T18:48:39.1620240519"}
{"field1": "foo", "field2": "bar", "field3": "2021-05-05T18:48:39.1620240519"}
{"field1": "foo", "field2": "bar", "field3": "2021-05-05T18:48:39.1620240519"}
{"field1": "foo", "field2": "bar", "field3": "2021-05-05T18:48:39.1620240519"}
{"field1": "foo", "field2": "bar", "field3": "2021-05-05T18:48:39.1620240519"}
{"field1": "foo", "field2": "bar", "field3": "2021-05-05T18:48:39.1620240519"}
{"field1": "foo", "field2": "bar", "field3": "2021-05-05T18:48:39.1620240519"}
{"field1": "foo", "field2": "bar", "field3": "2021-05-05T18:48:39.1620240519"}
{"field1": "foo", "field2": "bar", "field3": "2021-05-05T18:48:39.1620240519"}
```