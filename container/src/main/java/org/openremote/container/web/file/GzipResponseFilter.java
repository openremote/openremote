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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableSet;

/**
 * <p>
 * The {@link GzipResponseFilter} will apply GZIP compression on responses whenever applicable. GZIP will greatly reduce
 * the response size when applied on character based responses like HTML, CSS and JS, on average it can save up to ~70%
 * of bandwidth.
 * <p>
 * While GZIP is normally to be configured in the servlet container (e.g. <code>&lt;Context compression="on"&gt;</code>
 * in Tomcat, or <code>&lt;property name="compression" value="on"&gt;</code> in Glassfish), this filter allows a
 * servlet container independent way of configuring GZIP compression and also allows enabling GZIP compression anyway
 * on 3rd party hosts where you have no control over servlet container configuration.
 *
 * <h3>Installation</h3>
 * <p>
 * To get it to run, map this filter on the desired <code>&lt;url-pattern&gt;</code> or maybe even on the
 * <code>&lt;servlet-name&gt;</code> of the <code>FacesServlet</code>. A <code>Filter</code> is by default dispatched
 * on <code>REQUEST</code> only, you might want to explicitly add the <code>ERROR</code> dispatcher to get it to run
 * on error pages as well.
 * <pre>
 * &lt;filter&gt;
 *     &lt;filter-name&gt;gzipResponseFilter&lt;/filter-name&gt;
 *     &lt;filter-class&gt;org.omnifaces.filter.GzipResponseFilter&lt;/filter-class&gt;
 * &lt;/filter&gt;
 * &lt;filter-mapping&gt;
 *     &lt;filter-name&gt;gzipResponseFilter&lt;/filter-name&gt;
 *     &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 *     &lt;dispatcher&gt;REQUEST&lt;/dispatcher&gt;
 *     &lt;dispatcher&gt;ERROR&lt;/dispatcher&gt;
 * &lt;/filter-mapping&gt;
 * </pre>
 * <p>
 * Mapping on <code>/*</code> may be too global as some types of requests (comet, long polling, etc) cannot be gzipped.
 * In that case, consider mapping it to the exact <code>&lt;servlet-name&gt;</code> of the FacesServlet in the
 * same <code>web.xml</code>.
 * <pre>
 * &lt;filter&gt;
 *     &lt;filter-name&gt;gzipResponseFilter&lt;/filter-name&gt;
 *     &lt;filter-class&gt;org.omnifaces.filter.GzipResponseFilter&lt;/filter-class&gt;
 * &lt;/filter&gt;
 * &lt;filter-mapping&gt;
 *     &lt;filter-name&gt;gzipResponseFilter&lt;/filter-name&gt;
 *     &lt;servlet-name&gt;facesServlet&lt;/servlet-name&gt;
 *     &lt;dispatcher&gt;REQUEST&lt;/dispatcher&gt;
 *     &lt;dispatcher&gt;ERROR&lt;/dispatcher&gt;
 * &lt;/filter-mapping&gt;
 * </pre>
 *
 * <h3>Configuration (optional)</h3>
 * <p>
 * This filter supports two initialization parameters which needs to be placed in <code>&lt;filter&gt;</code> element
 * as follows:
 * <pre>
 * &lt;init-param&gt;
 *     &lt;description&gt;The threshold size in bytes. Must be a number between 0 and 9999. Defaults to 150.&lt;/description&gt;
 *     &lt;param-name&gt;threshold&lt;/param-name&gt;
 *     &lt;param-value&gt;150&lt;/param-value&gt;
 * &lt;/init-param&gt;
 * &lt;init-param&gt;
 *     &lt;description&gt;The mimetypes which needs to be compressed. Must be a commaseparated string. Defaults to the below values.&lt;/description&gt;
 *     &lt;param-name&gt;mimetypes&lt;/param-name&gt;
 *     &lt;param-value&gt;
 *         text/plain, text/html, text/xml, text/css, text/javascript, text/csv, text/rtf,
 *         application/xml, application/xhtml+xml, application/javascript, application/json,
 *         image/svg+xml
 *     &lt;/param-value&gt;
 * &lt;/init-param&gt;
 * </pre>
 * <p>
 * The default <code>threshold</code> is thus 150 bytes. This means that when the response is not larger than 150 bytes,
 * then it will not be compressed with GZIP. Only when it's larger than 150 bytes, then it will be compressed. A
 * threshold of between 150 and 1000 bytes is recommended due to overhead and latency of compression/decompression.
 * The value must be a number between 0 and 9999. A value larger than 2000 is not recommended.
 * <p>
 * The <code>mimetypes</code> represents a comma separated string of mime types which needs to be compressed. It's
 * exactly that value which appears in the <code>Content-Type</code> header of the response. The in the above example
 * mentioned mime types are already the default values. Note that GZIP does not have any benefit when applied on
 * binary mimetypes like images, office documents, PDF files, etcetera. So setting it for them is not recommended.
 * <p>
 * The <code>zipped</code> init parameter is a list of file name extensions which are considered already zipped, so only
 * a gzip header must be added to the response for such files.
 *
 * @author Bauke Scholtz
 * @since 1.1
 * @see GzipHttpServletResponse
 * @see HttpServletResponseOutputWrapper
 * @see ResettableBuffer
 * @see ResettableBufferedOutputStream
 * @see ResettableBufferedWriter
 * @see HttpFilter
 */
public class GzipResponseFilter extends HttpFilter {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String INIT_PARAM_THRESHOLD = "threshold";
	private static final String INIT_PARAM_MIMETYPES = "mimetypes";

	private static final int DEFAULT_THRESHOLD = 150;
	private static final Set<String> DEFAULT_MIMETYPES = unmodifiableSet(new HashSet<>(Arrays.asList(
	    "text/plain", "text/html", "text/xml", "text/css", "text/javascript", "text/csv", "text/rtf",
		"application/xml", "application/xhtml+xml", "application/javascript", "application/json",
		"image/svg+xml"
	)));

	private static final String ERROR_THRESHOLD = "The 'threshold' init param must be a number between 0 and 9999."
		+ " Encountered an invalid value of '%s'.";

	// Vars -----------------------------------------------------------------------------------------------------------

	private Set<String> mimetypes = DEFAULT_MIMETYPES;
	private int threshold = DEFAULT_THRESHOLD;

	// Actions --------------------------------------------------------------------------------------------------------


    public GzipResponseFilter() {
    }

    public GzipResponseFilter(String[] mimetypes) {
        this.mimetypes = new HashSet<>(Arrays.asList(mimetypes));
    }

    /**
	 * Initializes the filter parameters.
	 */
	@Override
	public void init() throws ServletException {
		String thresholdParam = getInitParameter(INIT_PARAM_THRESHOLD);

		if (thresholdParam != null) {
			if (!thresholdParam.matches("[0-9]{1,4}")) {
				throw new ServletException(format(ERROR_THRESHOLD, thresholdParam));
			}
			else {
				threshold = Integer.valueOf(thresholdParam);
			}
		}

		String mimetypesParam = getInitParameter(INIT_PARAM_MIMETYPES);
		if (mimetypesParam != null) {
			mimetypes = new HashSet<>(Arrays.asList(mimetypesParam.split("\\s*,\\s*")));
		}
    }

	/**
	 * Perform the filtering job. Only if the client accepts GZIP based on the request headers, then wrap the response
	 * in a {@link GzipHttpServletResponse} and pass it through the filter chain.
	 */
	@Override
	public void doFilter
		(HttpServletRequest request, HttpServletResponse response, HttpSession session, FilterChain chain)
			throws ServletException, IOException
	{
		if (acceptsGzip(request)) {
            GzipHttpServletResponse gzipResponse = new GzipHttpServletResponse(response, threshold, mimetypes);
            chain.doFilter(request, gzipResponse);
            gzipResponse.close(); // Mandatory for the case the threshold limit hasn't been reached.
		}
		else {
            chain.doFilter(request, response);
		}
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Returns whether the given request indicates that the client accepts GZIP encoding.
	 * @param request The request to be checked.
	 * @return <code>true</code> if the client accepts GZIP encoding, otherwise <code>false</code>.
	 */
	private static boolean acceptsGzip(HttpServletRequest request) {
		for (Enumeration<String> e = request.getHeaders("Accept-Encoding"); e.hasMoreElements();) {
			if (e.nextElement().contains("gzip")) {
				return true;
			}
		}

		return false;
	}

}