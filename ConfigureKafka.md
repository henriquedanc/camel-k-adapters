# Kafka configuration

This assumes that Kafka cluster is up and running

- in your Kafka cluster, create the topics that will be used (start with 10 partitions per topic, optimal number will have to be defined during load tests)
```
# create topics
#cd KAFKA_DIR
bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic syslog-data  --partitions 10 --replication-factor 1
bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic json-data --partitions 10 --replication-factor 1
bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic snmp-data --partitions 10 --replication-factor 1

# check
bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic syslog-data
bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic json-data
bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic snmp-data
```

- in your K8S cluster, create a configmap for camel with kafka broker information, updating with your broker info
```
cat <<EOF | tee kafka-configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: kafka-config
data:
  application.properties: |
    kafka.brokers=BROKER1:9092,BROKER2:9092,BROKER3:9092,BROKER4:9092,BROKER5:9092
EOF

kubectl apply -f kafka-configmap.yaml
```