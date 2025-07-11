/*
 * Copyright 2021, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.agent.protocol.serial;

import static org.openremote.agent.protocol.serial.JSerialCommChannelOption.*;

import java.util.Map;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;

/** A configuration class for jSerialComm device connections. */
final class DefaultJSerialCommChannelConfig extends DefaultChannelConfig
    implements JSerialCommChannelConfig {

  private volatile int baudrate = 115200;
  private volatile Stopbits stopbits = Stopbits.STOPBITS_1;
  private volatile int databits = 8;
  private volatile Paritybit paritybit = Paritybit.NONE;
  private volatile int waitTime;
  private volatile int readTimeout = 200;

  DefaultJSerialCommChannelConfig(JSerialCommChannel channel) {
    super(channel);
  }

  @Override
  public Map<ChannelOption<?>, Object> getOptions() {
    return getOptions(super.getOptions(), BAUD_RATE, STOP_BITS, DATA_BITS, PARITY_BIT, WAIT_TIME);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getOption(ChannelOption<T> option) {
    if (option == BAUD_RATE) {
      return (T) Integer.valueOf(getBaudrate());
    }
    if (option == STOP_BITS) {
      return (T) getStopbits();
    }
    if (option == DATA_BITS) {
      return (T) Integer.valueOf(getDatabits());
    }
    if (option == PARITY_BIT) {
      return (T) getParitybit();
    }
    if (option == WAIT_TIME) {
      return (T) Integer.valueOf(getWaitTimeMillis());
    }
    if (option == READ_TIMEOUT) {
      return (T) Integer.valueOf(getReadTimeout());
    }
    return super.getOption(option);
  }

  @Override
  public <T> boolean setOption(ChannelOption<T> option, T value) {
    validate(option, value);

    if (option == BAUD_RATE) {
      setBaudrate((Integer) value);
    } else if (option == STOP_BITS) {
      setStopbits((Stopbits) value);
    } else if (option == DATA_BITS) {
      setDatabits((Integer) value);
    } else if (option == PARITY_BIT) {
      setParitybit((Paritybit) value);
    } else if (option == WAIT_TIME) {
      setWaitTimeMillis((Integer) value);
    } else if (option == READ_TIMEOUT) {
      setReadTimeout((Integer) value);
    } else {
      return super.setOption(option, value);
    }
    return true;
  }

  @Override
  public JSerialCommChannelConfig setBaudrate(final int baudrate) {
    this.baudrate = baudrate;
    return this;
  }

  @Override
  public JSerialCommChannelConfig setStopbits(final Stopbits stopbits) {
    this.stopbits = stopbits;
    return this;
  }

  @Override
  public JSerialCommChannelConfig setDatabits(final int databits) {
    this.databits = databits;
    return this;
  }

  @Override
  public JSerialCommChannelConfig setParitybit(final Paritybit paritybit) {
    this.paritybit = paritybit;
    return this;
  }

  @Override
  public int getBaudrate() {
    return baudrate;
  }

  @Override
  public Stopbits getStopbits() {
    return stopbits;
  }

  @Override
  public int getDatabits() {
    return databits;
  }

  @Override
  public Paritybit getParitybit() {
    return paritybit;
  }

  @Override
  public int getWaitTimeMillis() {
    return waitTime;
  }

  @Override
  public JSerialCommChannelConfig setWaitTimeMillis(final int waitTimeMillis) {
    if (waitTimeMillis < 0) {
      throw new IllegalArgumentException("Wait time must be >= 0");
    }
    waitTime = waitTimeMillis;
    return this;
  }

  @Override
  public JSerialCommChannelConfig setReadTimeout(int readTimeout) {
    if (readTimeout < 0) {
      throw new IllegalArgumentException("readTime must be >= 0");
    }
    this.readTimeout = readTimeout;
    return this;
  }

  @Override
  public int getReadTimeout() {
    return readTimeout;
  }

  @Override
  public JSerialCommChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis) {
    super.setConnectTimeoutMillis(connectTimeoutMillis);
    return this;
  }

  @Override
  public JSerialCommChannelConfig setWriteSpinCount(int writeSpinCount) {
    super.setWriteSpinCount(writeSpinCount);
    return this;
  }

  @Override
  public JSerialCommChannelConfig setAllocator(ByteBufAllocator allocator) {
    super.setAllocator(allocator);
    return this;
  }

  @Override
  public JSerialCommChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator) {
    super.setRecvByteBufAllocator(allocator);
    return this;
  }

  @Override
  public JSerialCommChannelConfig setAutoRead(boolean autoRead) {
    super.setAutoRead(autoRead);
    return this;
  }

  @Override
  public JSerialCommChannelConfig setAutoClose(boolean autoClose) {
    super.setAutoClose(autoClose);
    return this;
  }

  @Override
  public JSerialCommChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark) {
    super.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
    return this;
  }

  @Override
  public JSerialCommChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark) {
    super.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
    return this;
  }

  @Override
  public JSerialCommChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator) {
    super.setMessageSizeEstimator(estimator);
    return this;
  }
}
