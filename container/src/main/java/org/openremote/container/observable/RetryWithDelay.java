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