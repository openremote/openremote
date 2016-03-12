package org.openremote.test

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget
import org.openremote.manager.shared.http.RequestParams
import org.openremote.manager.shared.http.SuccessStatusCode
import org.spockframework.mock.IMockMethod

import java.lang.reflect.Method

trait ClientTrait {

    def void callResourceProxy(def mockInvocation) {
        // If the first parameter of the method we want to call is RequestParams
        List<Object> args = mockInvocation.getArguments();
        IMockMethod mockMethod = mockInvocation.getMethod();
        if (args[0] instanceof RequestParams) {
            RequestParams requestParams = (RequestParams) args[0];

            // Get a Resteasy client proxy for the resource
            Class mockedResourceType = mockInvocation.getMockObject().getType();
            Method mockedResourceMethod = mockedResourceType.getDeclaredMethod(
                    mockMethod.name,
                    mockMethod.parameterTypes.toArray(new Class[mockMethod.parameterTypes.size()])
            );
            def resourceProxy = getClientTarget().proxy(mockedResourceType);

            // Try to find out what the expected success status code is
            SuccessStatusCode successStatusCode =
                    mockedResourceMethod.getDeclaredAnnotation(SuccessStatusCode.class);
            int statusCode = successStatusCode != null ? successStatusCode.value() : 200;

            // Call the proxy
            def result = resourceProxy."$mockMethod.name"(args);

            // Pass the result to the callback, so it looks asynchronous for client code
            requestParams.callback.call(statusCode, null, result);
        }
    }

    abstract def ResteasyWebTarget getClientTarget();

}
