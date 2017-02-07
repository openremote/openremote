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

/**
 * Base interface for a resettable buffer.
 *
 * @author Bauke Scholtz
 */
public interface ResettableBuffer {

	/**
	 * Perform a buffer reset.
	 */
	void reset();

	/**
	 * Returns true if buffer can be reset.
	 * @return <code>true</code> if buffer can be reset, otherwise <code>false</code>.
	 */
	boolean isResettable();

}