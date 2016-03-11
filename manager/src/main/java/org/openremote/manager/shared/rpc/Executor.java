package org.openremote.manager.shared.rpc;
/*
The Executor is responsible for processing the RPC Request and calling the success or error callbacks
 */
@FunctionalInterface
public interface Executor<T> {
    <U extends RequestData> void execute(RestRequest<T, U> request) throws Failure;
}
