/*
 * Copyright 2017 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.openremote.container.web.file;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * <p>
 * The {@link HttpFilter} is abstract filter specifically for HTTP requests. It provides a convenient abstract
 * {@link #doFilter(HttpServletRequest, HttpServletResponse, HttpSession, FilterChain)} method directly providing the
 * HTTP servlet request, response and session, so that there's no need to cast them everytime in the
 * {@link #doFilter(ServletRequest, ServletResponse, FilterChain)} implementation. Also, default implementations of
 * {@link #init(FilterConfig)} and {@link #destroy()} are provided, so that there's no need to implement them every time
 * even when not really needed.
 * <p>
 * It's a bit the idea of using the convenient {@link HttpServlet} abstract servlet class instead of the barebones
 * {@link Servlet} interface.
 *
 * <h3>Usage</h3>
 * <p>
 * To use it, just let your custom filter extend from {@link HttpFilter} instead of implement {@link Filter}.
 * For example:
 * <pre>
 * &#64;WebFilter("/app/*")
 * public class LoginFilter extends HttpFilter {
 *
 *     &#64;Override
 *     public void doFilter(HttpServletRequest request, HttpServletResponse response, HttpSession session, FilterChain chain)
 *         throws ServletException, IOException
 *     {
 *         if (session != null &amp;&amp; session.getAttribute("user") != null) {
 *             chain.doFilter(request, response);
 *         }
 *         else {
 *             Servlets.facesRedirect(request, response, "login.xhtml");
 *         }
 *     }
 * }
 * </pre>
 *
 * @author Arjan Tijms
 * @author Bauke Scholtz
 */
public abstract class HttpFilter implements Filter {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String ERROR_NO_FILTERCONFIG = "FilterConfig is not available."
		+ " It seems that you've overriden HttpFilter#init(FilterConfig)."
		+ " You should be overriding HttpFilter#init() instead, otherwise you have to call super.init(config).";

	// Properties -----------------------------------------------------------------------------------------------------

	private FilterConfig filterConfig;

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Called by the servlet container when the filter is about to be placed into service. This implementation stores
	 * the {@link FilterConfig} object for later use by the getter methods. It's recommended to <strong>not</strong>
	 * override this method. Instead, just use {@link #init()} method. When overriding this method anyway, don't forget
	 * to call <code>super.init(config)</code>, otherwise the getter methods will throw an illegal state exception.
	 */
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;
		init();
	}

	/**
	 * Convenience init() method without FilterConfig parameter which will be called by init(FilterConfig).
	 * @throws ServletException When filter's initialization failed.
	 */
	public void init() throws ServletException {
		//
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
		throws ServletException, IOException
	{
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		HttpSession session = httpRequest.getSession(false);
		doFilter(httpRequest, httpResponse, session, chain);
	}

	/**
	 * Filter the HTTP request. The session argument is <code>null</code> if there is no session.
	 * @param request The HTTP request.
	 * @param response The HTTP response.
	 * @param session The HTTP session, if any, else <code>null</code>.
	 * @param chain The filter chain to continue.
	 * @throws ServletException As wrapper exception when something fails in the request processing.
	 * @throws IOException Whenever something fails at I/O level.
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public abstract void doFilter
		(HttpServletRequest request, HttpServletResponse response, HttpSession session, FilterChain chain)
			throws ServletException, IOException;

	@Override
	public void destroy() {
		filterConfig = null;
	}

	// Getters --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the filter config.
	 * @return The filter config.
	 */
	protected FilterConfig getFilterConfig() {
		checkFilterConfig();
		return filterConfig;
	}

	/**
	 * Returns the value of the filter init parameter associated with the given name.
	 * @param name The filter init parameter name to return the associated value for.
	 * @return The value of the filter init parameter associated with the given name.
	 */
	protected String getInitParameter(String name) {
		checkFilterConfig();
		return filterConfig.getInitParameter(name);
	}

	/**
	 * Returns the servlet context.
	 * @return The servlet context.
	 */
	protected ServletContext getServletContext() {
		checkFilterConfig();
		return filterConfig.getServletContext();
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Check if the filter config is been set and thus the enduser has properly called super.init(config) when
	 * overriding the init(config).
	 * @throws IllegalStateException When this is not the case.
	 */
	private void checkFilterConfig() {
		if (filterConfig == null) {
			throw new IllegalStateException(ERROR_NO_FILTERCONFIG);
		}
	}

}