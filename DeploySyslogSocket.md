# Deploying Syslog Socket Adapter

This assumes that the Kubernetes cluster is already up and running, and kubectl and kamel can successfully communicate with the cluster.
This integration uses a kafka-config configmap that was created in a previous step. If you didn't do it, go back and create that now

- Create the integration configuration

```
# create directory to store Camel definitions
mkdir -p /opt/sas/routes
cd /opt/sas/routes
```

- Copy the [SyslogSocket.java](routes/SyslogSocket.java) file into the routes directory above

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
kamel run -d mvn:javax.xml.bind:jaxb-api:2.3.1 -d mvn:com.sun.xml.bind:jaxb-core:2.3.0.1 -d mvn:com.sun.xml.bind:jaxb-impl:2.3.2 -d camel-syslog -d camel-kafka --trait affinity.enabled=false --trait affinity.node-affinity-labels=netspeed=40g --trait toleration.enabled=true --trait toleration.taints="netspeed=40g:NoSchedule" --configmap=kafka-config --trait jvm.options="-Xms5632m -Xmx5632m" --trait container.request-cpu=4000m --trait container.request-memory=6144Mi SyslogSocket.java

# get status
kamel get syslog-socket

# get pod name
syslog_socket_pod_name=$(kubectl get pods --selector=camel.apache.org/integration=syslog-socket -o jsonpath='{.items[0].metadata.name}')
echo $syslog_socket_pod_name

# check the logs
kubectl logs $syslog_socket_pod_name -f

# scale the deployment (start with 10 replicas)
kubectl scale it syslog-socket --replicas=10

# expose the deployment
# we are using nodeport as an example, but can be different
kubectl expose deployment syslog-socket --port 514 --protocol UDP --type NodePort

# get service info
kubectl describe svc syslog-socket
```

- In the kafka broker, subscribe to the syslog-data topic
```
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic syslog-data --group test-consumer
```

- Now publish some data into the socket  
  If you used NodePort service type above, you can use any node as the target. Just use the node port you got from the service description.
```
NODE=lab-2
NODEPORT=30022
while (true); 
do 
 echo "<165>1 `date +'%Y-%m-%dT%H:%M:%S.%s'` mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"] BOMAn application event log entry..." | netcat -u -w0 $NODE $NODEPORT;
 echo "<34>`LC_TIME=en_US.utf8 date +'%b %d %H:%M:%S'` mymachine su: 'su root' failed for lonvick on /dev/pts/8" | netcat -u -w0 $NODE $NODEPORT;
done

```

- You should receive the messages in the kafka-console-consumer.sh terminal
```
A6574D304CCABAD-00000000000003FC|LOCAL4|NOTICE|2021-05-06 12:41:12.162+0000|mymachine.example.com|BOMAn application event log entry...|evntslog|-|ID47|[exampleSDID@32473 iut="3" eventSource="Application" eventID="1011"]
A6574D304CCABAD-00000000000003FD|AUTH|CRIT|2021-05-06 12:41:12.000+0000|mymachine|su: 'su root' failed for lonvick on /dev/pts/8||||
A6574D304CCABAD-00000000000003FE|LOCAL4|NOTICE|2021-05-06 12:41:12.162+0000|mymachine.example.com|BOMAn application event log entry...|evntslog|-|ID47|[exampleSDID@32473 iut="3" eventSource="Application" eventID="1011"]
A6574D304CCABAD-00000000000003FF|AUTH|CRIT|2021-05-06 12:41:12.000+0000|mymachine|su: 'su root' failed for lonvick on /dev/pts/8||||
A6574D304CCABAD-0000000000000400|LOCAL4|NOTICE|2021-05-06 12:41:12.162+0000|mymachine.example.com|BOMAn application event log entry...|evntslog|-|ID47|[exampleSDID@32473 iut="3" eventSource="Application" eventID="1011"]
A6574D304CCABAD-00000000000003FF|AUTH|CRIT|2021-05-06 12:41:12.000+0000|mymachine|su: 'su root' failed for lonvick on /dev/pts/8||||
```

- Another possible test with loggen
```
# install syslog-ng
sudo yum install syslog-ng
```

# run loggen
```
loggen -i -D -I 60 -P -p "[fooSDID@32474 iut=\"4\" eventSource=\"Port\" eventID=\"99\"][barSDID@32475 eventSource=\"Rack\" eventID=\"88\"]" $NODE $NODEPORT
```

- you should receive the messages in the kafka-console-consumer.sh terminal
```
A6574D304CCABAD-000000000000DEA5|AUTH|INFO|2021-05-06 10:43:06.000+0000|localhost|ï»¿seq: 0000056724, thread: 0000, runid: 1620304926, stamp: 2021-05-06T12:43:06 PADDPADDPADDPA|prg00000|1234|-|[fooSDID@32474 iut="4" eventSource="Port" eventID="99"][barSDID@32475 eventSource="Rack" eventID="88"]
A6574D304CCABAD-000000000000DEA6|AUTH|INFO|2021-05-06 10:43:06.000+0000|localhost|ï»¿seq: 0000056725, thread: 0000, runid: 1620304926, stamp: 2021-05-06T12:43:06 PADDPADDPADDPA|prg00000|1234|-|[fooSDID@32474 iut="4" eventSource="Port" eventID="99"][barSDID@32475 eventSource="Rack" eventID="88"]
A6574D304CCABAD-000000000000DEA7|AUTH|INFO|2021-05-06 10:43:06.000+0000|localhost|ï»¿seq: 0000056726, thread: 0000, runid: 1620304926, stamp: 2021-05-06T12:43:06 PADDPADDPADDPA|prg00000|1234|-|[fooSDID@32474 iut="4" eventSource="Port" eventID="99"][barSDID@32475 eventSource="Rack" eventID="88"]
A6574D304CCABAD-000000000000DEA8|AUTH|INFO|2021-05-06 10:43:06.000+0000|localhost|ï»¿seq: 0000056727, thread: 0000, runid: 1620304926, stamp: 2021-05-06T12:43:06 PADDPADDPADDPA|prg00000|1234|-|[fooSDID@32474 iut="4" eventSource="Port" eventID="99"][barSDID@32475 eventSource="Rack" eventID="88"]
A6574D304CCABAD-000000000000DEA9|AUTH|INFO|2021-05-06 10:43:06.000+0000|localhost|ï»¿seq: 0000056728, thread: 0000, runid: 1620304926, stamp: 2021-05-06T12:43:06 PADDPADDPADDPA|prg00000|1234|-|[fooSDID@32474 iut="4" eventSource="Port" eventID="99"][barSDID@32475 eventSource="Rack" eventID="88"]
A6574D304CCABAD-000000000000DEAA|AUTH|INFO|2021-05-06 10:43:06.000+0000|localhost|ï»¿seq: 0000056729, thread: 0000, runid: 1620304926, stamp: 2021-05-06T12:43:06 PADDPADDPADDPA|prg00000|1234|-|[fooSDID@32474 iut="4" eventSource="Port" eventID="99"][barSDID@32475 eventSource="Rack" eventID="88"]
```