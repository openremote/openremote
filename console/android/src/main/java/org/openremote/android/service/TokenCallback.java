package org.openremote.android.service;

interface TokenCallback {
    void onToken(String accessToken);

    void onFailure(Throwable t);
}
