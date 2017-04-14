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
import java.io.OutputStream;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletResponse;

/**
 * This HTTP servlet response wrapper will GZIP the response when the given threshold has exceeded and the response
 * content type matches one of the given mimetypes.
 *
 * @author Bauke Scholtz
 * @since 1.1
 */
public class GzipHttpServletResponse extends HttpServletResponseOutputWrapper {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final Pattern NO_TRANSFORM =
		Pattern.compile("((.*)[\\s,])?no-transform([\\s,](.*))?", Pattern.CASE_INSENSITIVE);

	// Properties -----------------------------------------------------------------------------------------------------

	private final int threshold;
	private final Set<String> mimetypes;
    private long contentLength;
	private String vary;
	private boolean noGzip;
	private boolean closing;
	private GzipThresholdOutputStream output;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new GZIP HTTP servlet response based on the given wrapped response, threshold and mimetypes.
	 * @param wrapped The wrapped response.
	 * @param threshold The GZIP buffer threshold.
	 * @param mimetypes The mimetypes which needs to be compressed with GZIP.
	 */
	public GzipHttpServletResponse(HttpServletResponse wrapped,
                                   int threshold,
                                   Set<String> mimetypes) {
		super(wrapped);
		this.threshold = threshold;
		this.mimetypes = mimetypes;
    }

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public void setContentLength(int contentLength) {
		// Get hold of content length locally to avoid it from being set on responses which will actually be gzipped.
		this.contentLength = contentLength;
	}

	// @Override Servlet 3.1.
	public void setContentLengthLong(long contentLength) {
		// Get hold of content length locally to avoid it from being set on responses which will actually be gzipped.
		this.contentLength = contentLength;
	}

	@Override
	public void setHeader(String name, String value) {
		super.setHeader(name, value);

		if (name != null) {
			String lowerCasedName = name.toLowerCase();

			if ("vary".equals(lowerCasedName)) {
				vary = value;
			}
			else if ("content-range".equals(lowerCasedName)) {
				noGzip = (value != null);
			}
			else if ("cache-control".equals(lowerCasedName)) {
				noGzip = (value != null && NO_TRANSFORM.matcher(value).matches());
			}
		}
	}

	@Override
	public void addHeader(String name, String value) {
		super.addHeader(name, value);

		if (name != null && value != null) {
			String lowerCasedName = name.toLowerCase();

			if ("vary".equals(lowerCasedName)) {
				vary = ((vary != null) ? (vary + ",") : "") + value;
			}
			else if ("content-range".equals(lowerCasedName)) {
				noGzip = true;
			}
			else if ("cache-control".equals(lowerCasedName)) {
				noGzip = (noGzip || NO_TRANSFORM.matcher(value).matches());
			}
		}
	}

	@Override
	public void flushBuffer() throws IOException {
		if (isCommitted()) {
			super.flushBuffer();
		}
	}

	@Override
	public void reset() {
		super.reset();

		if (!isCommitted()) {
			contentLength = 0;
			vary = null;
			noGzip = false;

			if (output != null) {
				output.reset();
			}
		}
	}

	@Override
	public void close() throws IOException {
		closing = true;
		super.close();
		closing = false;
	}

	@Override
	protected OutputStream createOutputStream() {
		output = new GzipThresholdOutputStream(threshold);
		return output;
	}

	// Inner classes --------------------------------------------------------------------------------------------------

	/**
	 * This output stream will switch to GZIP compression when the given threshold is exceeded.
	 * <p>
	 * This is an inner class because it needs to be able to manipulate the response headers once the decision whether
	 * to GZIP or not has been made.
	 *
	 * @author Bauke Scholtz
	 */
	private class GzipThresholdOutputStream extends ResettableBufferedOutputStream {

		// Constructors -----------------------------------------------------------------------------------------------

		public GzipThresholdOutputStream(int threshold) {
			super(threshold);
		}

		// Actions ----------------------------------------------------------------------------------------------------

		/**
		 * Create GZIP output stream if necessary. That is, when the given <code>doGzip</code> argument is
		 * <code>true</code>, the current response does not have the <code>Cache-Control: no-transform</code> or
		 * <code>Content-Range</code> headers, the current response is not committed, the content type is not
		 * <code>null</code> and the content type matches one of the mimetypes.
		 */
		@Override
		public OutputStream createOutputStream(boolean doGzip) throws IOException {
			HttpServletResponse originalResponse = (HttpServletResponse) getResponse();

			if (doGzip && !noGzip && (closing || !isCommitted())) {
				String contentType = getContentType();

				if (contentType != null && mimetypes.contains(contentType.split(";", 2)[0])) {
					addHeader("Content-Encoding", "gzip");
					setHeader("Vary", (!isOneOf(vary, null, "*") ? (vary + ",") : "") + "Accept-Encoding");

					// Remove Content-Length header that was set before we knew about whether to zip the response or not.
                    // The servlet container will now set the header (at least Undertow does) based on its internal
                    // write buffer position to the actual number of written bytes. This fixes a bug in the
                    // "BalusC" file servlet code, because they assume the Content-Length is the original size of the
                    // resource. This is not the case, RFC 2616 says its the transfer-length of the message body,
                    // thus it must the size of the gzipped data, not the original size.
                    setHeader("Content-Length", null);

                    return new GZIPOutputStream(originalResponse.getOutputStream());
				}
			}

			if (!doGzip) {
				setContentLength(getWrittenBytes());
			}

			if (contentLength > 0) {
				originalResponse.setHeader("Content-Length", String.valueOf(contentLength));
			}

			return originalResponse.getOutputStream();
		}

	}

	/* ############################################################################ */

    /**
     * Returns <code>true</code> if the given object equals one of the given objects.
     * @param <T> The generic object type.
     * @param object The object to be checked if it equals one of the given objects.
     * @param objects The argument list of objects to be tested for equality.
     * @return <code>true</code> if the given object equals one of the given objects.
     */
    @SafeVarargs
    public static <T> boolean isOneOf(T object, T... objects) {
        for (Object other : objects) {
            if (Objects.equals(object, other)) {
                return true;
            }
        }

        return false;
    }
}