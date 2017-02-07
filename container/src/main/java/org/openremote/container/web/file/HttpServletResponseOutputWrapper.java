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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Convenience class for extending {@link HttpServletResponseWrapper} wherein the servlet response {@link OutputStream}
 * has to be replaced by a custom implementation. This saves the developer from writing repeated
 * {@link #getOutputStream()}, {@link #getWriter()} and {@link #flushBuffer()} boilerplate. All the developer has to do
 * is to implement the {@link #createOutputStream()} accordingly. This will in turn be used by both
 * {@link #getOutputStream()} and {@link #getWriter()}.
 * <p>
 * The boolean property <code>passThrough</code>, which defaults to <code>false</code> also enables the developer to
 * control whether to pass through to the wrapped {@link ServletOutputStream} or not.
 * <p>
 *
 * @author Bauke Scholtz
 * @since 1.1
 */
public abstract class HttpServletResponseOutputWrapper extends HttpServletResponseWrapper {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String ERROR_GETOUTPUT_ALREADY_CALLED =
		"getOutputStream() has already been called on this response.";
	private static final String ERROR_GETWRITER_ALREADY_CALLED =
		"getWriter() has already been called on this response.";

	// Properties -----------------------------------------------------------------------------------------------------

	private ServletOutputStream output;
	private PrintWriter writer;
	private ResettableBuffer buffer;
	private boolean passThrough;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new {@link HttpServletResponseOutputWrapper} which wraps the given response.
	 * @param wrappedResponse The wrapped response.
	 */
	public HttpServletResponseOutputWrapper(HttpServletResponse wrappedResponse) {
		super(wrappedResponse);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the custom implementation of the servlet response {@link OutputStream}.
	 * @return The custom implementation of the servlet response {@link OutputStream}.
	 */
	protected abstract OutputStream createOutputStream();

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (passThrough) {
			return super.getOutputStream();
		}

		if (writer != null) {
			throw new IllegalStateException(ERROR_GETWRITER_ALREADY_CALLED);
		}

		if (output == null) {
			buffer = new ResettableBufferedOutputStream(createOutputStream(), getBufferSize());
			output = new DefaultServletOutputStream((OutputStream) buffer);
		}

		return output;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (passThrough) {
			return super.getWriter();
		}

		if (output != null) {
			throw new IllegalStateException(ERROR_GETOUTPUT_ALREADY_CALLED);
		}

		if (writer == null) {
			buffer = new ResettableBufferedWriter(new OutputStreamWriter(createOutputStream(),
				getCharacterEncoding()), getBufferSize(), getCharacterEncoding());
			writer = new PrintWriter((Writer) buffer);
		}

		return writer;
	}

	@Override
	public void flushBuffer() throws IOException {
		super.flushBuffer();

		if (passThrough) {
			return;
		}

		if (writer != null) {
			writer.flush();
		}
		else if (output != null) {
			output.flush();
		}
	}

	/**
	 * Close the response body. This closes any created writer or output stream.
	 * @throws IOException When an I/O error occurs.
	 */
	public void close() throws IOException {
		if (writer != null) {
			writer.close();
		}
		else if (output != null) {
			output.close();
		}
	}

	@Override
	public void reset() {
		super.reset();

		if (buffer != null) {
			buffer.reset();
		}
	}

	// Getters/setters ------------------------------------------------------------------------------------------------

	/**
	 * Returns whether the response is committed or not. The response is also considered committed when the resettable
	 * buffer has been flushed.
	 * @return <code>true</code> if the response is committed, otherwise <code>false</code>.
	 */
	@Override
	public boolean isCommitted() {
		return super.isCommitted() || (buffer != null && !buffer.isResettable());
	}

	/**
	 * Returns whether the writing has to be passed through to the wrapped {@link ServletOutputStream}.
	 * @return <code>true</code>, if the writing has to be passed through to the wrapped {@link ServletOutputStream},
	 * otherwise <code>false</code>.
	 */
	public boolean isPassThrough() {
		return passThrough;
	}

	/**
	 * Sets whether the writing has to be passed through to the wrapped {@link ServletOutputStream}.
	 * @param passThrough set to <code>true</code> if the writing has to be passed through to the wrapped
	 * {@link ServletOutputStream}.
	 */
	public void setPassThrough(boolean passThrough) {
		this.passThrough = passThrough;
	}

}