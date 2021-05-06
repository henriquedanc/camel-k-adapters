# Deploying Snmp Poll Adapter

This assumes that the Kubernetes cluster is already up and running, and kubectl and kamel can successfully communicate with the cluster.
This integration uses a kafka-config configmap that was created in a previous step. If you didn't do it, go back and create that now

- Create the integration configuration

```
# create directory to store Camel definitions
mkdir -p /opt/sas/routes
cd /opt/sas/routes
```

- Copy the [SnmpPoll.java](routes/SnmpPoll.java) file into the routes directory above

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
# for each distinct element that we will poll SNMP data from, set the variables below according to it and then run the integration
SNMP_NAME=kaf-3
SNMP_HOST=kaf-3
SNMP_PORT=161

# sample OIDs for CentOS
#percentage of user CPU time:    .1.3.6.1.4.1.2021.11.9.0
#percentages of system CPU time: .1.3.6.1.4.1.2021.11.10.0
#percentages of idle CPU time:   .1.3.6.1.4.1.2021.11.11.0
#Total RAM in machine:           .1.3.6.1.4.1.2021.4.5.0
#Total RAM used:                 .1.3.6.1.4.1.2021.4.6.0
#Total RAM Free:                 .1.3.6.1.4.1.2021.4.11.0
#Total RAM Shared:               .1.3.6.1.4.1.2021.4.13.0
#Total RAM Buffered:             .1.3.6.1.4.1.2021.4.14.0
#Total Cached Memory:            .1.3.6.1.4.1.2021.4.15.0

SNMP_OIDS=.1.3.6.1.4.1.2021.11.9.0,.1.3.6.1.4.1.2021.11.10.0,.1.3.6.1.4.1.2021.11.11.0,.1.3.6.1.4.1.2021.4.5.0,.1.3.6.1.4.1.2021.4.6.0,.1.3.6.1.4.1.2021.4.11.0,.1.3.6.1.4.1.2021.4.13.0,.1.3.6.1.4.1.2021.4.14.0,.1.3.6.1.4.1.2021.4.15.0
SNMP_VERSION=1 # 0: SNMPv1, 1: SNMPv2c, 3: SNMPv3
SNMP_COMMUNITY=public
SNMP_DELAY=200 # poll every 200ms
SNMP_TIMEOUT=1000 # 1s timeout

kamel run --name snmp-poll-$SNMP_NAME --property snmp.hostport=$SNMP_HOST:$SNMP_PORT --property snmp.oids="$SNMP_OIDS" --property snmp.version=$SNMP_VERSION --property snmp.community=$SNMP_COMMUNITY --property snmp.delay="$SNMP_DELAY" --property snmp.timeout=$SNMP_TIMEOUT -d mvn:javax.xml.bind:jaxb-api:2.3.1 -d mvn:com.sun.xml.bind:jaxb-core:2.3.0.1 -d mvn:com.sun.xml.bind:jaxb-impl:2.3.2 -d camel-snmp -d camel-saxon -d camel-kafka --trait affinity.enabled=false --trait affinity.node-affinity-labels=netspeed=40g --trait toleration.enabled=true --trait toleration.taints="netspeed=40g:NoSchedule" --configmap=kafka-config --trait jvm.options="-Xms2048m -Xmx2048m" --trait container.request-cpu=1000m --trait container.request-memory=2048Mi SnmpPoll.java

# get status
kamel get snmp-poll-$SNMP_NAME

# get pod name
snmp_poll_pod_name=$(kubectl get pods --selector=camel.apache.org/integration=snmp-poll-$SNMP_NAME -o jsonpath='{.items[0].metadata.name}')
echo $snmp_poll_pod_name

# check the logs
kubectl logs $snmp_poll_pod_name -f
```

- In the kafka broker, subscribe to the syslog-data topic
```
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic snmp-data --group test-consumer
```

- You should receive the messages in the kafka-console-consumer.sh terminal
```
F60953266A9A594-000000000000318E|1.3.6.1.4.1.2021.4.11.0|28894660|2021-05-06T19:33:44.792
F60953266A9A594-000000000000318F|1.3.6.1.4.1.2021.4.13.0|57804|2021-05-06T19:33:44.792
F60953266A9A594-0000000000003190|1.3.6.1.4.1.2021.4.14.0|2620|2021-05-06T19:33:44.792
F60953266A9A594-0000000000003191|1.3.6.1.4.1.2021.4.15.0|2094148|2021-05-06T19:33:44.792
F60953266A9A594-0000000000003193|1.3.6.1.4.1.2021.11.9.0|0|2021-05-06T19:33:44.992
F60953266A9A594-0000000000003194|1.3.6.1.4.1.2021.11.10.0|0|2021-05-06T19:33:44.992
F60953266A9A594-0000000000003195|1.3.6.1.4.1.2021.11.11.0|99|2021-05-06T19:33:44.992
F60953266A9A594-0000000000003196|1.3.6.1.4.1.2021.4.5.0|32779556|2021-05-06T19:33:44.992
F60953266A9A594-0000000000003197|1.3.6.1.4.1.2021.4.6.0|28894660|2021-05-06T19:33:44.992
F60953266A9A594-0000000000003198|1.3.6.1.4.1.2021.4.11.0|28894660|2021-05-06T19:33:44.992
F60953266A9A594-0000000000003199|1.3.6.1.4.1.2021.4.13.0|57804|2021-05-06T19:33:44.992
F60953266A9A594-000000000000319A|1.3.6.1.4.1.2021.4.14.0|2620|2021-05-06T19:33:44.992
F60953266A9A594-000000000000319B|1.3.6.1.4.1.2021.4.15.0|2094148|2021-05-06T19:33:44.992
F60953266A9A594-000000000000319D|1.3.6.1.4.1.2021.11.9.0|0|2021-05-06T19:33:45.193
F60953266A9A594-000000000000319E|1.3.6.1.4.1.2021.11.10.0|0|2021-05-06T19:33:45.193
F60953266A9A594-000000000000319F|1.3.6.1.4.1.2021.11.11.0|99|2021-05-06T19:33:45.193
F60953266A9A594-00000000000031A0|1.3.6.1.4.1.2021.4.5.0|32779556|2021-05-06T19:33:45.193
F60953266A9A594-00000000000031A1|1.3.6.1.4.1.2021.4.6.0|28894660|2021-05-06T19:33:45.193
F60953266A9A594-00000000000031A2|1.3.6.1.4.1.2021.4.11.0|28894660|2021-05-06T19:33:45.193
F60953266A9A594-00000000000031A3|1.3.6.1.4.1.2021.4.13.0|57804|2021-05-06T19:33:45.193
F60953266A9A594-00000000000031A4|1.3.6.1.4.1.2021.4.14.0|2620|2021-05-06T19:33:45.193
F60953266A9A594-00000000000031A5|1.3.6.1.4.1.2021.4.15.0|2094148|2021-05-06T19:33:45.193
F60953266A9A594-00000000000031A7|1.3.6.1.4.1.2021.11.9.0|0|2021-05-06T19:33:45.394
F60953266A9A594-00000000000031A8|1.3.6.1.4.1.2021.11.10.0|0|2021-05-06T19:33:45.394
F60953266A9A594-00000000000031A9|1.3.6.1.4.1.2021.11.11.0|99|2021-05-06T19:33:45.394
F60953266A9A594-00000000000031AA|1.3.6.1.4.1.2021.4.5.0|32779556|2021-05-06T19:33:45.394
F60953266A9A594-00000000000031AB|1.3.6.1.4.1.2021.4.6.0|28894660|2021-05-06T19:33:45.394
F60953266A9A594-00000000000031AC|1.3.6.1.4.1.2021.4.11.0|28894660|2021-05-06T19:33:45.394
F60953266A9A594-00000000000031AD|1.3.6.1.4.1.2021.4.13.0|57804|2021-05-06T19:33:45.394
F60953266A9A594-00000000000031AE|1.3.6.1.4.1.2021.4.14.0|2620|2021-05-06T19:33:45.394
F60953266A9A594-00000000000031AF|1.3.6.1.4.1.2021.4.15.0|2094148|2021-05-06T19:33:45.394
```