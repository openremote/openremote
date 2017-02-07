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

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * This resettable buffered writer will buffer everything until the given buffer size, regardless of flush calls.
 * Only when the buffer size is exceeded, or when close is called, then the buffer will be actually flushed.
 * <p>
 * There is a {@link #reset()} method which enables the developer to reset the buffer, as long as it's not flushed yet,
 * which can be determined by {@link #isResettable()}.
 *
 * @author Bauke Scholtz
 * @see ResettableBufferedOutputStream
 */
public class ResettableBufferedWriter extends Writer implements ResettableBuffer {

	// Variables ------------------------------------------------------------------------------------------------------

	private Writer writer;
	private Charset charset;
	private CharArrayWriter buffer;
	private int bufferSize;
	private int writtenBytes;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new resettable buffered writer which wraps the given writer and forcibly buffers everything until
	 * the given buffer size in bytes, regardless of flush calls. The given character encoding is used to measure the
	 * amount of already written bytes in the buffer.
	 * regardless of flush calls.
	 * @param writer The wrapped writer.
	 * @param bufferSize The buffer size.
	 * @param characterEncoding The character encoding.
	 */
	public ResettableBufferedWriter(Writer writer, int bufferSize, String characterEncoding) {
		this.writer = writer;
		this.bufferSize = bufferSize;
		charset = Charset.forName(characterEncoding);
		buffer = new CharArrayWriter(bufferSize);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public void write(char[] chars, int offset, int length) throws IOException {
		if (buffer != null) {
			writtenBytes += charset.encode(CharBuffer.wrap(chars, offset, length)).limit();

			if (writtenBytes > bufferSize) {
				writer.write(buffer.toCharArray());
				writer.write(chars, offset, length);
				buffer = null;
			}
			else {
				buffer.write(chars, offset, length);
			}
		}
		else {
			writer.write(chars, offset, length);
		}
	}

	@Override
	public void reset() {
		buffer = new CharArrayWriter(bufferSize);
		writtenBytes = 0;
	}

	@Override
	public void flush() throws IOException {
		if (buffer == null) {
			writer.flush();
		}
	}

	@Override
	public void close() throws IOException {
		if (buffer != null) {
			writer.write(buffer.toCharArray());
			buffer = null;
		}

		writer.close();
	}

	@Override
	public boolean isResettable() {
		return buffer != null;
	}

}