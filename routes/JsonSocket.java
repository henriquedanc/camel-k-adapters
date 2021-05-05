import org.apache.camel.builder.RouteBuilder;

public class JsonSocket extends RouteBuilder {

     @Override
     public void configure() throws Exception {
         from("netty://tcp://0.0.0.0:5514?sync=false")
          .to("log:socket?groupActiveOnly=true&groupDelay=30000&groupInterval=30000")
          .to("direct:toKafka");

         from("direct:toKafka")
           .to("kafka:syslog-data?brokers={{kafka.brokers}}")
           .to("log:toKafka?groupActiveOnly=true&groupDelay=30000&groupInterval=30000");

    }
}
