import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.syslog.SyslogDataFormat;

public class SyslogSocket extends RouteBuilder {

     SyslogDataFormat syslogDf = new SyslogDataFormat();

     @Override
     public void configure() throws Exception {
             from("netty://udp://0.0.0.0:514?sync=false&allowDefaultCodec=false")
              .to("log:socket?groupActiveOnly=true&groupDelay=30000&groupInterval=30000")
              .unmarshal(syslogDf)
              .setHeader("syslogTimestamp").simple("${body.timestamp.time}")
              .choice()
              .when(simple("${body} is 'org.apache.camel.component.syslog.Rfc5424SyslogMessage'"))
                .to("direct:rfc5424")
              .otherwise()
                .to("direct:rfc3164");
                  
             from("direct:rfc5424")
        	   .setBody().simple("${exchangeId}|${body.facility}|${body.severity}|${date:header.syslogTimestamp:YYYY-MM-dd HH:mm:ss.SSSZ}|${body.hostname}|${body.logMessage}|${body.appName}|${body.procId}|${body.msgId}|${body.structuredData}")
        	   .to("log:rfc5424?groupActiveOnly=true&groupDelay=30000&groupInterval=30000")
        	   .to("direct:toKafka");
        	  
        	 from("direct:rfc3164")
        	   .setBody().simple("${exchangeId}|${body.facility}|${body.severity}|${date:header.syslogTimestamp:YYYY-MM-dd HH:mm:ss.000+0000}|${body.hostname}|${body.logMessage}")
        	   .to("log:rfc3164?groupActiveOnly=true&groupDelay=30000&groupInterval=30000")
        	   .to("direct:toKafka");
        	  
        	 from("direct:toKafka")
               .to("kafka:syslog-data?brokers={{kafka.brokers}}")
               .to("log:toKafka?groupActiveOnly=true&groupDelay=30000&groupInterval=30000");

    }
}