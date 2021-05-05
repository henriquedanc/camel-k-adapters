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

- in the kafka broker, subscribe to the syslog-data topic
```
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic syslog-data --group test-consumer
```

- now publish some data into the socket, through a node where the pod was scheduled to and the nodeport you got from the service description
```
NODE=lab-2
NODEPORT=30022
while (true); 
do 
 echo "<165>1 `date +'%Y-%m-%dT%H:%M:%S.%s'` mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"] BOMAn application event log entry..." | netcat -u -w0 $NODE $NODEPORT;
 echo "<34>`LC_TIME=en_US.utf8 date +'%b %d %H:%M:%S'` mymachine su: 'su root' failed for lonvick on /dev/pts/8" | netcat -u -w0 $NODE $NODEPORT;
done

```

- you should receive the messages in the kafka-console-consumer.sh terminal
```
0E09719D628327F-0000000000000000|LOCAL4|NOTICE|2021-05-05 18:57:27.162+0000|mymachine.example.com|BOMAn application event log entry...
|evntslog|-|ID47|[exampleSDID@32473 iut="3" eventSource="Application" eventID="1011"]
0E09719D628327F-0000000000000002|AUTH|CRIT|2021-05-05 19:01:12.965+0000|mymachine|su: 'su root' failed for lonvick on /dev/pts/8

0E09719D628327F-0000000000000003|AUTH|CRIT|2021-05-05 19:01:15.990+0000|mymachine|su: 'su root' failed for lonvick on /dev/pts/8

0E09719D628327F-0000000000000004|AUTH|CRIT|2021-05-05 19:01:23.652+0000|mymachine|su: 'su root' failed for lonvick on /dev/pts/8

0E09719D628327F-0000000000000006|LOCAL4|NOTICE|2021-05-05 19:01:35.162+0000|mymachine.example.com|BOMAn application event log entry...
|evntslog|-|ID47|[exampleSDID@32473 iut="3" eventSource="Application" eventID="1011"]
0E09719D628327F-0000000000000007|LOCAL4|NOTICE|2021-05-05 19:01:36.162+0000|mymachine.example.com|BOMAn application event log entry...
|evntslog|-|ID47|[exampleSDID@32473 iut="3" eventSource="Application" eventID="1011"]
```

- another possible test with loggen
```
# install syslog-ng
sudo yum install syslog-ng

# run loggen
```
loggen -i -D -I 60 -P -p "[test id=\"foo\" val=\"bar\"][test2 id=\"foo2\" val=\"bar2\"][test3 id=\"foo3\" val=\"bar3\"]" $NODE $NODEPORT
```

- you should receive the messages in the kafka-console-consumer.sh terminal
```
4DA09F269B0EF3F-000000000027BED2|AUTH|INFO|2021-05-05 19:16:36.000+0000|localhost|ï»¿seq: 0000056755, thread: 0000, runid: 1620249336, stamp: 2021-05-05T21:16:36 PADDPADDPADDPADDPADDPADDPADDPADDPAD
|prg00000|1234|-|[test id="foo" val="bar"][test2 id="foo2" val="bar2"][test3 id="foo3" val="bar3"]
4DA09F269B0EF3F-000000000027BED3|AUTH|INFO|2021-05-05 19:16:36.000+0000|localhost|ï»¿seq: 0000056756, thread: 0000, runid: 1620249336, stamp: 2021-05-05T21:16:36 PADDPADDPADDPADDPADDPADDPADDPADDPAD
|prg00000|1234|-|[test id="foo" val="bar"][test2 id="foo2" val="bar2"][test3 id="foo3" val="bar3"]
4DA09F269B0EF3F-000000000027BED4|AUTH|INFO|2021-05-05 19:16:36.000+0000|localhost|ï»¿seq: 0000056757, thread: 0000, runid: 1620249336, stamp: 2021-05-05T21:16:36 PADDPADDPADDPADDPADDPADDPADDPADDPAD
|prg00000|1234|-|[test id="foo" val="bar"][test2 id="foo2" val="bar2"][test3 id="foo3" val="bar3"]
4DA09F269B0EF3F-000000000027BED6|AUTH|INFO|2021-05-05 19:16:36.000+0000|localhost|ï»¿seq: 0000056759, thread: 0000, runid: 1620249336, stamp: 2021-05-05T21:16:36 PADDPADDPADDPADDPADDPADDPADDPADDPAD
|prg00000|1234|-|[test id="foo" val="bar"][test2 id="foo2" val="bar2"][test3 id="foo3" val="bar3"]
4DA09F269B0EF3F-000000000027BED5|AUTH|INFO|2021-05-05 19:16:36.000+0000|localhost|ï»¿seq: 0000056758, thread: 0000, runid: 1620249336, stamp: 2021-05-05T21:16:36 PADDPADDPADDPADDPADDPADDPADDPADDPAD
|prg00000|1234|-|[test id="foo" val="bar"][test2 id="foo2" val="bar2"][test3 id="foo3" val="bar3"]
4DA09F269B0EF3F-000000000027BED7|AUTH|INFO|2021-05-05 19:16:36.000+0000|localhost|ï»¿seq: 0000056760, thread: 0000, runid: 1620249336, stamp: 2021-05-05T21:16:36 PADDPADDPADDPADDPADDPADDPADDPADDPAD
|prg00000|1234|-|[test id="foo" val="bar"][test2 id="foo2" val="bar2"][test3 id="foo3" val="bar3"]
4DA09F269B0EF3F-000000000027BED8|AUTH|INFO|2021-05-05 19:16:36.000+0000|localhost|ï»¿seq: 0000056761, thread: 0000, runid: 1620249336, stamp: 2021-05-05T21:16:36 PADDPADDPADDPADDPADDPADDPADDPADDPAD
```