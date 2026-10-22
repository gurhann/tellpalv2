package com.tellpal.v2.user.web.mobile;

import java.io.IOException;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;
import org.apache.catalina.valves.ValveBase;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.coyote.ActionCode;
import jakarta.servlet.ServletException;

/** Yerel QA ve normal görsel handler'larına nonblocking bağlantı abort yeteneğini verir. */
@Component
@org.springframework.context.annotation.Profile({"local","test"})
public class ReadingImageComparisonTomcatConfiguration implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
    static final String ABORT=ReadingImageComparisonTomcatConfiguration.class.getName()+".abort";
    @Override public void customize(TomcatServletWebServerFactory factory) {
        factory.addContextValves(new ValveBase(true) {
            @Override public void invoke(Request request,Response response)throws IOException,ServletException {
                if(request.getDecodedRequestURI().startsWith(request.getContextPath()+"/api/test/reading-image-comparison")
                        || request.getDecodedRequestURI().startsWith(request.getContextPath()+"/api/library/reading-images"))
                    request.setAttribute(ABORT,(Runnable)()->request.getCoyoteRequest().action(ActionCode.CLOSE_NOW,new IOException("READING_RESPONSE_DEADLINE")));
                getNext().invoke(request,response);
            }
        });
    }
}
