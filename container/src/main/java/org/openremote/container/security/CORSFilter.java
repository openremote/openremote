package org.openremote.container.security;

import io.undertow.util.StatusCodes;
import org.jboss.resteasy.spi.CorsHeaders;
import org.openremote.container.web.file.HttpFilter;
import org.openremote.model.util.TextUtil;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class CORSFilter extends HttpFilter {
    protected boolean allowCredentials = true;
    protected String allowedMethods;
    protected String allowedHeaders;
    protected String exposedHeaders;
    protected int corsMaxAge = -1;
    protected Set<String> allowedOrigins = new HashSet<>();

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public String getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(String allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public String getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(String allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public String getExposedHeaders() {
        return exposedHeaders;
    }

    public void setExposedHeaders(String exposedHeaders) {
        this.exposedHeaders = exposedHeaders;
    }

    public int getCorsMaxAge() {
        return corsMaxAge;
    }

    public void setCorsMaxAge(int corsMaxAge) {
        this.corsMaxAge = corsMaxAge;
    }

    public Set<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(Set<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(HttpServletRequest request, HttpServletResponse response, HttpSession session, FilterChain chain) throws ServletException, IOException {
        String origin = request.getHeader(CorsHeaders.ORIGIN);
        boolean isOptions = request.getMethod().equals(HttpMethod.OPTIONS);

        if (origin == null) {
            chain.doFilter(request, response);
            return;
        }

        if (!originOk(origin)) {
            response.sendError(StatusCodes.FORBIDDEN, "Origin not allowed");
            return;
        }

        if (isOptions) {
            response.setStatus(StatusCodes.OK);
            response.setHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            if (allowCredentials) {
                response.setHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            }

            String requestMethods = request.getHeader(CorsHeaders.ACCESS_CONTROL_REQUEST_METHOD);
            if (!TextUtil.isNullOrEmpty(requestMethods)) {
                if (allowedMethods != null)
                {
                    requestMethods = this.allowedMethods;
                }
                response.setHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_METHODS, requestMethods);
            }

            String requestHeaders = request.getHeader(CorsHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
            if (!TextUtil.isNullOrEmpty(requestHeaders)) {
                if (allowedHeaders != null)
                {
                    requestHeaders = this.allowedHeaders;
                }
                response.setHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_HEADERS, requestHeaders);
            }

            if (corsMaxAge > -1)
            {
                response.setHeader(CorsHeaders.ACCESS_CONTROL_MAX_AGE, Integer.toString(corsMaxAge));
            }
        } else {
            response.setHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);

            if (allowCredentials) {
                response.setHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            }

            if (exposedHeaders != null) {
                response.setHeader(CorsHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, exposedHeaders);
            }

            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {

    }

    protected boolean originOk(String origin) {
        // startsWith test allows for host matching without explicit port mapping
        return allowedOrigins.contains("*") || allowedOrigins.contains(origin) || allowedOrigins.stream().anyMatch(origin::startsWith);
    }

}
