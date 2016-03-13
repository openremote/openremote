
package org.openremote.manager.shared.http;

import org.openremote.manager.shared.Consumer;

public abstract class AbstractCallback<T> implements Callback<T> {

    public static final int ANY_STATUS_CODE = -1;

    final protected int expectedStatusCode;
    final protected Consumer<T> onSuccess;
    final protected Consumer<Exception> onFailure;

    public AbstractCallback(Consumer<T> onSuccess, Consumer<Exception> onFailure) {
        this(ANY_STATUS_CODE, onSuccess, onFailure);
    }

    public AbstractCallback(int expectedStatusCode, Consumer<T> onSuccess, Consumer<Exception> onFailure) {
        this.expectedStatusCode = expectedStatusCode;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
    }

    @Override
    public void call(int responseCode, Object entity) {
        if (responseCode == 0) {
            onFailure.accept(new RequestParams.Failure(0, "No response"));
            return;
        }
        if (expectedStatusCode != ANY_STATUS_CODE && responseCode != expectedStatusCode) {
            onFailure.accept(new RequestParams.Failure(responseCode, "Expected status code: " + expectedStatusCode));
            return;
        }
        onSuccess.accept(readMessageBody(entity));
    }

    /**
     * The object returned by RESTEasy JavaScript API is either
     *
     * <ul>
     * <li>a JavascriptObject if the content type is JSON</li>
     * <li>a Document if the content type is XML</li>
     * <li>or a String</li>
     * </ul>
     *
     * This method converts to the desired type.
     */
    protected abstract T readMessageBody(Object entity);
}
