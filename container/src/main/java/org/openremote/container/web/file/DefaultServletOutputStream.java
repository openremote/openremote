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

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

/**
 * <p>
 * A default implementation of abstract servlet output stream.
 *
 * @author Bauke Scholtz
 * @since 2.6
 */
public class DefaultServletOutputStream extends ServletOutputStream {

	private OutputStream output;

	/**
	 * Constructs a default servlet output stream which delegates to given output stream.
	 * @param output The output stream to let this servlet output stream delegate to.
	 */
	public DefaultServletOutputStream(OutputStream output) {
		this.output = output;
	}

	@Override
	public void write(int b) throws IOException {
		output.write(b);
	}

	@Override
	public void write(byte[] bytes) throws IOException {
		output.write(bytes);
	}

	@Override
	public void write(byte[] bytes, int offset, int length) throws IOException {
		output.write(bytes, offset, length);
	}

	@Override
	public void flush() throws IOException {
		output.flush();
	}

	@Override
	public void close() throws IOException {
		output.close();
	}

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
	    throw new UnsupportedOperationException("Not supported");
    }
}