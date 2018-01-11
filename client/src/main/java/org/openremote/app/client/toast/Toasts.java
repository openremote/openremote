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
package org.openremote.app.client.toast;

import com.google.gwt.user.client.Timer;
import jsinterop.annotations.JsType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@JsType
public class Toasts extends Timer {

    final protected List<Toast> toasts = new LinkedList<>();
    final protected ToastDisplay display;

    public Toasts(ToastDisplay display) {
        this.display = display;
    }

    public List<Toast> getToasts() {
        synchronized (toasts) {
            return new ArrayList<>(toasts);
        }
    }

    @Override
    public void run() {
        synchronized (toasts) {
            boolean onlyDurableToastsLeft = true;
            Iterator<Toast> it = toasts.iterator();
            while (it.hasNext()) {
                Toast toast = it.next();
                if (toast.isExpired()) {
                    getDisplay().remove(toast);
                    it.remove();
                } else if (toast.getType() != Toast.Type.DURABLE_FAILURE) {
                    onlyDurableToastsLeft = false;
                }
            }
            if (toasts.isEmpty() || onlyDurableToastsLeft) {
                cancel();
            }
        }
    }

    public ToastDisplay getDisplay() {
        return display;
    }

    public void showToast(Toast toast) {
        if (toast == null)
            return;
        synchronized (toasts) {

            // If it's the same toast from the user's point of view, only show it once
            for (Toast existing : toasts) {
                if (existing.equalsForUser(toast))
                return;
            }

            toasts.add(toast);
            if (!isRunning())
                scheduleRepeating(100);
            getDisplay().show(toast);
        }
    }
}
