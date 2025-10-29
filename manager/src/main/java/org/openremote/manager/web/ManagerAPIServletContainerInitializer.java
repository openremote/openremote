package org.openremote.manager.web;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;

import java.util.Set;

/**
 * This is a Jakarta Servlet 3.0 initializer for the manager REST API; resources can be added
 */
public class ManagerAPIServletContainerInitializer implements ServletContainerInitializer {
    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {

        ServletRegistration.Dynamic servlet = ctx.addServlet(
            "ResteasyServlet",
            HttpServlet30Dispatcher.class
        );

        servlet.setInitParameter(
                "jakarta.ws.rs.Application",
                ManagerAPIApplication.class.getName()
        );

        servlet.addMapping("/*");
        servlet.setLoadOnStartup(1);
        servlet.setAsyncSupported(true);
    }
}
