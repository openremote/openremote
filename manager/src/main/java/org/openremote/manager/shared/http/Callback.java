package org.openremote.manager.shared.http;

public interface Callback<T> {
    void call(int responseCode, Object entity);
}
