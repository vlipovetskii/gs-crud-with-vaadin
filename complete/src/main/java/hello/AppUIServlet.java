package hello;


import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.SessionInitEvent;
import com.vaadin.flow.server.SessionInitListener;
import com.vaadin.flow.spring.SpringServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

//@Component("springServlet")
// @Component
//@Primary
// @WebServlet(urlPatterns = "/app/*", name = "AppUIServlet", asyncSupported = false) // , asyncSupported = true)
// @VaadinServletConfiguration(ui = AppUi::class, productionMode = false)
// @VaadinServletConfiguration(productionMode = false)
public class AppUIServlet extends SpringServlet implements SessionInitListener {

    private final static Logger log = LoggerFactory.getLogger(Application.class);

    public AppUIServlet(ApplicationContext context) {
        super(context);
    }

    @Override
    protected void servletInitialized() throws ServletException {
        super.servletInitialized();
        log.info("servletInitialized");
        getService().addSessionInitListener(this);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.service(request, response);
        log.info("service");
    }

    @Override
    public void sessionInit(SessionInitEvent event) throws ServiceException {
        log.info("sessionInit");
    }
}
