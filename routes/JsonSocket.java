import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.netty.DefaultChannelHandlerFactory;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;

public class JsonSocket extends RouteBuilder {
  
    ChannelHandler getDlmDecoder() throws Exception {
        return new DefaultChannelHandlerFactory() {
           @Override
           public ChannelHandler newChannelHandler() {
               return new DelimiterBasedFrameDecoder(10240, // max frame size
                                                     true,  // strip delimiter from mesage
                                                     Delimiters.lineDelimiter() // the delimiter
                                                    );
           }
        };
    }
  
    StringDecoder strDec = new StringDecoder();

    @Override
    public void configure() throws Exception {

       bindToRegistry("dlmDec", getDlmDecoder());
       bindToRegistry("strDec", strDec);

       from("netty://tcp://0.0.0.0:5514?sync=false&decoders=#dlmDec,#strDec")
        .to("log:socket?groupActiveOnly=true&groupDelay=30000&groupInterval=30000")
        .to("direct:toKafka");

       from("direct:toKafka")
        .to("kafka:json-data?brokers={{kafka.brokers}}")
        .to("log:toKafka?groupActiveOnly=true&groupDelay=30000&groupInterval=30000");

    }
}
