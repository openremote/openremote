package org.openremote.manager.shared.rpc;

import org.openremote.manager.shared.Function;

public class RpcService {
    private Executor executor;

    public RpcService() {}

    public RpcService(Executor executor) {
        setExecutor(executor);
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public <T,U extends RequestData> RestRequest<T, U> request(Function<U,T> fn, U params) {
        return new RestRequest<>(executor, fn, params);
    }

    public <T> RestRequest<T,RequestData> request(Function<RequestData,T> fn) {
        return new RestRequest<>(executor, fn, new RequestData());
    }
}
