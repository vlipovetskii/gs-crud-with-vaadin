package hello;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.spring.SpringBootAutoConfiguration;
import com.vaadin.flow.spring.VaadinConfigurationProperties;
import com.vaadin.flow.spring.VaadinServletConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.ServletForwardingController;

import java.util.HashMap;
import java.util.Map;

// How to override default Spring Vaadin Servlet

/*
com.vaadin.flow.spring.VaadinConfigurationProperties.java - configuration properties for Vaadin Spring Boot.
	urlMapping property value default "/*"
	asyncSupported property value default true

com.vaadin.flow.spring.SpringBootAutoConfiguration
        servletRegistrationBean: Creates a {@link ServletRegistrationBean} instance with Spring aware Vaadin servlet.
        servletRegistrationBean registers com.vaadin.flow.spring.SpringServlet (extends VaadinServlet) with mapping, that depends on value of VaadinConfigurationProperties.urlMapping.

        If VaadinConfigurationProperties.urlMapping == "/*" (e.g. default value), then VaadinServletConfiguration.VAADIN_SERVLET_MAPPING ("/vaadinServlet/*") is leveraged instead of "/*".
        else value of VaadinConfigurationProperties.urlMapping is leveraged.
        In essence, this mapping is leveraged only for async (websocket),
        since
        vaadinForwardingController registers ServletForwardingController bean, that is leveraged to forward all the requests to SpringServlet:
            see. controller.setServletName(ClassUtils.getShortNameAsProperty(SpringServlet.class));
            org.springframework.web.servlet.DispatcherServlet that is mapped to "/*" forwards via ServletForwardingController all the requests to SpringServlet.

        In order to leverage custom Spring VaadinServlet instead of com.vaadin.flow.spring.SpringServlet, do the following:

        Declare custom Spring VaadinServlet
            see. AppUIServlet.java

        Declare two beans in SpringServletDiConfig below


        Before
            ...
            o.s.b.w.servlet.ServletRegistrationBean  : Servlet dispatcher mapped to [/*]
            o.s.b.w.servlet.ServletRegistrationBean  : Servlet springServlet mapped to [/vaadinServlet/*]
            ...

        after
            ...
            o.s.b.w.servlet.ServletRegistrationBean  : Servlet dispatcher mapped to [/*]
            o.s.b.w.servlet.ServletRegistrationBean  : Servlet appUIServlet mapped to [/vaadinServlet/*]
            ...

	This solution has small drawback: 2 servlet contexts.
	All these urls will work (and processed by AppUIServlet)
		http://localhost:8080
		http://localhost:8080/vaadinServlet

	If (AppUIServlet mapped to "/*") async (websocket) support can't be registered:
		java.util.NoSuchElementException: null
			at java.base/java.util.HashMap$HashIterator.nextNode(HashMap.java:1500) ~[na:na]
			at java.base/java.util.HashMap$KeyIterator.next(HashMap.java:1521) ~[na:na]
			at org.atmosphere.util.IOUtils.guestRawServletPath(IOUtils.java:283) [atmosphere-runtime-2.4.24.vaadin1.jar:2.4.24.vaadin1]
			...
*/

@Configuration
@EnableConfigurationProperties(VaadinConfigurationProperties.class)
// Important: Without importing VaadinServletConfiguration.class, vaadinForwardingController in SpringServletDiConfig is not called even if it has @Primary annotation
// Without importing SpringBootAutoConfiguration.class, servletRegistrationBean in SpringServletDiConfig doesn't override servletRegistrationBean in SpringBootAutoConfiguration
@Import({VaadinServletConfiguration.class, SpringBootAutoConfiguration.class})
public class SpringServletDiConfig {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private VaadinConfigurationProperties configurationProperties;

    // CUSTOM_SERVLET_MAPPING can have any value (including "/vaadinServlet/*"), except "/*".
    // CUSTOM_SERVLET_MAPPING ("/*") -> makeContextRelative(mapping.replace("*", "")) -> "context://" ->
    // 		java.util.NoSuchElementException: null
    //			at java.base/java.util.HashMap$HashIterator.nextNode(HashMap.java:1500) ~[na:na]
    //			at java.base/java.util.HashMap$KeyIterator.next(HashMap.java:1521) ~[na:na]
    //			at org.atmosphere.util.IOUtils.guestRawServletPath(IOUtils.java:283) [atmosphere-runtime-2.4.24.vaadin1.jar:2.4.24.vaadin1]
    private static final String CUSTOM_SERVLET_MAPPING = "/vaadinServlet/*";

    /**
     * The code is copied from com.vaadin.flow.spring.VaadinMVCWebAppInitializer, since makeContextRelative in VaadinMVCWebAppInitializer has package private visibility.
     */
    private static String makeContextRelative(String url) {
        // / -> context://
        // foo -> context://foo
        // /foo -> context://foo
        if (url.startsWith("/")) {
            url = url.substring(1);
        }
        return "context://" + url;
    }

    // @Primary
    @Bean
    public ServletRegistrationBean<AppUIServlet> servletRegistrationBean() {
        String mapping = CUSTOM_SERVLET_MAPPING;

        ServletRegistrationBean<AppUIServlet> registration = new ServletRegistrationBean<>(new AppUIServlet(context), mapping);

        Map<String, String> initParameters = new HashMap<>();
        initParameters.put(Constants.SERVLET_PARAMETER_PUSH_URL, makeContextRelative(mapping.replace("*", "")));
        registration.setInitParameters(initParameters);

        registration.setAsyncSupported(configurationProperties.isAsyncSupported());
        registration.setName(ClassUtils.getShortNameAsProperty(AppUIServlet.class));
        return registration;
    }

    /**
     * Override vaadinForwardingController bean, that is registered in com.vaadin.flow.spring.VaadinServletConfiguration
     */
    // @Primary
    @Bean
    public Controller vaadinForwardingController() {
        ServletForwardingController controller = new ServletForwardingController();
        controller.setServletName(ClassUtils.getShortNameAsProperty(AppUIServlet.class));
        return controller;
    }

}
