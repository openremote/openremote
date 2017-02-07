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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * This HTTP servlet response implementation buffers the entire response body. The buffered response body is available
 * as a byte array via the {@link #getBuffer()} method. Note that the buffered response writer will use same character
 * encoding as is been set on the response supplied to the constructor and that this way any
 * {@link ServletResponse#setCharacterEncoding(String)} calls on the included JSP/Servlet resource have thus no effect.
 *
 * @author Bauke Scholtz
 * @since 1.1
 */
public class BufferedHttpServletResponse extends HttpServletResponseOutputWrapper {

	// Properties -----------------------------------------------------------------------------------------------------

	private final ByteArrayOutputStream buffer;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a buffered HTTP servlet response which wraps the given response.
	 * @param response The response to be wrapped.
	 */
	public BufferedHttpServletResponse(HttpServletResponse response) {
		super(response);
		buffer = new ByteArrayOutputStream(response.getBufferSize());
	}

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	protected OutputStream createOutputStream() {
		return buffer;
	}

	/**
	 * Flushes and returns the buffered response.
	 * @return The buffered response.
	 * @throws IOException When an I/O error occurs.
	 */
	public byte[] getBuffer() throws IOException {
		close();
		return buffer.toByteArray();
	}

	/**
	 * Flushes and returns the buffered response as a string which is encoded using the character encoding provided by
	 * {@link #getCharacterEncoding()}.
	 * @return The buffered response as string.
	 * @throws IOException When an I/O error occurs.
	 */
	public String getBufferAsString() throws IOException {
		return new String(getBuffer(), getCharacterEncoding());
	}

}