/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.client.widget;

import com.google.gwt.text.shared.Renderer;
import com.google.gwt.user.client.ui.ValueListBox;
import com.google.gwt.view.client.ProvidesKey;

public class FormValueListBox<T> extends ValueListBox<T> {

    public FormValueListBox(Renderer<? super T> renderer) {
        super(renderer);
        setStyleName("or-FormControl or-FormValueListBox");
    }

    public FormValueListBox(Renderer<? super T> renderer, ProvidesKey<T> keyProvider) {
        super(renderer, keyProvider);
        setStyleName("or-FormControl or-FormValueListBox");
    }

    public FormValueListBox() {
        setStyleName("or-FormControl or-FormValueListBox");
    }
}
