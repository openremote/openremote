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
package org.openremote.container.observable;

import rx.Observable;
import rx.functions.Func1;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class RetryWithDelay implements Func1<Observable<? extends Throwable>, Observable<?>> {

    private static final Logger LOG = Logger.getLogger(RetryWithDelay.class.getName());

    protected final String retryMessage;
    protected final int maxRetries;
    protected final int retryDelayMillis;
    protected int retryCount;

    public RetryWithDelay(String retryMessage, int maxRetries, int retryDelayMillis) {
        this.retryMessage = retryMessage;
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
        this.retryCount = 0;
    }

    @Override
    public Observable<?> call(Observable<? extends Throwable> attempts) {
        return attempts
            .flatMap(new Func1<Throwable, Observable<?>>() {
                @Override
                public Observable<?> call(Throwable throwable) {
                    if (++retryCount < maxRetries) {
                        LOG.info("Retry attempt " + retryCount + " of " + maxRetries + ": " + retryMessage);
                        return Observable.timer(retryDelayMillis, TimeUnit.MILLISECONDS);
                    }

                    return Observable.error(throwable);
                }
            });
    }
}