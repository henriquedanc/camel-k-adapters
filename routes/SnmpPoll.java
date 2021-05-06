import org.apache.camel.builder.RouteBuilder;

public class SnmpPoll extends RouteBuilder {

    @Override
    public void configure() throws Exception {

		from("snmp:{{snmp.hostport}}?protocol=udp&type=POLL&scheduler.initialDelay=1000&scheduler.delay={{snmp.delay}}&timeout={{snmp.timeout}}&snmpVersion={{snmp.version}}&snmpCommunity={{snmp.community}}&oids={{snmp.oids}}")
     .setHeader("pollTimestamp").simple("${date:now:YYY-MM-dd'T'HH:mm:ss.SSS}")
    .to("log:snmpPoll?groupActiveOnly=true&groupDelay=30000&groupInterval=30000")
         .split().xpath("/snmp/entry")
         .to("log:snmpEntry?groupActiveOnly=true&groupDelay=30000&groupInterval=30000")
         .setBody().xquery("concat(/entry/oid, \"|\", /entry/value)", String.class)
         .setBody().simple("${exchangeId}|${body}|${header.pollTimestamp}")
         .to("direct:toKafka");
         
        from("direct:toKafka")
         .to("kafka:snmp-data?brokers={{kafka.brokers}}")
         .to("log:toKafka?groupActiveOnly=true&groupDelay=30000&groupInterval=30000");

    }
}