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

/**
 * This resettable buffered output stream will buffer everything until the given threshold buffer size, regardless of
 * flush calls. Only when the specified threshold buffer size is exceeded, or when {@link #close()} is called, then the
 * buffer will be actually flushed.
 * <p>
 * There is a {@link #reset()} method which enables the developer to reset the buffer, as long as it's not flushed yet,
 * which can be determined by {@link #isResettable()}.
 *
 * @author Bauke Scholtz
 * @see ResettableBufferedWriter
 */
public class ResettableBufferedOutputStream extends OutputStream implements ResettableBuffer {

	// Constants --------------------------------------------------------------------------------------------------

	private static final String ERROR_CLOSED = "Stream is already closed.";

	// Variables ------------------------------------------------------------------------------------------------------

	private OutputStream output;
	private ByteArrayOutputStream buffer;
	private int thresholdBufferSize;
	private int writtenBytes;
	private boolean closed;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new resettable buffered output stream which forcibly buffers everything until the given threshold
	 * buffer size, regardless of flush calls and calls. You need to override {@link #createOutputStream(boolean)} when
	 * using this constructor.
	 * @param thresholdBufferSize The threshold buffer size.
	 */
	public ResettableBufferedOutputStream(int thresholdBufferSize) {
		this(null, thresholdBufferSize);
	}

	/**
	 * Construct a new resettable buffered output stream which wraps the given output stream and forcibly buffers
	 * everything until the given threshold buffer size, regardless of flush calls. You do not need to override
	 * {@link #createOutputStream(boolean)} when using this constructor.
	 * @param output The wrapped output stream .
	 * @param thresholdBufferSize The threshold buffer size.
	 */
	public ResettableBufferedOutputStream(OutputStream output, int thresholdBufferSize) {
		this.output = output;
		this.thresholdBufferSize = thresholdBufferSize;
		buffer = new ByteArrayOutputStream(thresholdBufferSize);
	}

	// Actions --------------------------------------------------------------------------------------------------------

	@Override
	public void write(int b) throws IOException {
		write(new byte[] { (byte) b }, 0, 1);
	}

	@Override
	public void write(byte[] bytes) throws IOException {
		write(bytes, 0, bytes.length);
	}

	@Override
	public void write(byte[] bytes, int offset, int length) throws IOException {
		checkClosed();

		if (isResettable()) {
			writtenBytes += (length - offset);

			if (writtenBytes > thresholdBufferSize) {
				flushBuffer(true);
				output.write(bytes, offset, length);
			}
			else {
				buffer.write(bytes, offset, length);
			}
		}
		else {
			output.write(bytes, offset, length);
		}
	}

	private void flushBuffer(boolean thresholdBufferSizeExceeded) throws IOException {
		if (output == null) {
			if (thresholdBufferSizeExceeded) {
				writtenBytes = -1;
			}

			output = createOutputStream(thresholdBufferSizeExceeded);
		}

		output.write(buffer.toByteArray());
		buffer = null;
	}

	/**
	 * Returns the custom implementation of the {@link OutputStream}. This will only be called when the specified
	 * threshold buffer size is exceeded, or when {@link #close()} is called.
	 * @param thresholdBufferSizeExceeded Whether the threshold buffer size has exceeded.
	 * @return The custom implementation of the {@link OutputStream}.
	 * @throws IOException When an I/O error occurs.
	 */
	protected OutputStream createOutputStream(boolean thresholdBufferSizeExceeded) throws IOException {
		throw new UnsupportedOperationException(
			"You need to either override this method, or use the 2-arg constructor taking OutputStream instead.");
	}

	/**
	 * Returns the amount of so far written bytes in the threshold buffer. This will be 0 when the threshold buffer is
	 * empty and this will be -1 when the threshold has exceeded.
	 * @return The amount of so far written bytes in the threshold buffer.
	 */
	protected int getWrittenBytes() {
		return writtenBytes;
	}

	@Override
	public void reset() {
		if (isResettable()) {
			buffer = new ByteArrayOutputStream(thresholdBufferSize);
			writtenBytes = 0;
		}
	}

	@Override
	public void flush() throws IOException {
		checkClosed();

		if (!isResettable()) {
			output.flush();
		}
	}

	@Override
	public void close() throws IOException {
		if (closed) {
			return;
		}

		if (isResettable()) {
			flushBuffer(false);
		}

		output.close();
		closed = true;
	}

	@Override
	public boolean isResettable() {
		return buffer != null;
	}

	/**
	 * Check if the current stream is closed and if so, then throw IO exception.
	 * @throws IOException When the current stream is closed.
	 */
	private void checkClosed() throws IOException {
		if (closed) {
			throw new IOException(ERROR_CLOSED);
		}
	}

}