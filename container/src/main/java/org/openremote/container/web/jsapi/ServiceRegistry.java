/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openremote.container.web.jsapi;

import org.jboss.resteasy.core.ResourceInvoker;
import org.jboss.resteasy.core.ResourceLocatorInvoker;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.metadata.ResourceLocator;
import org.jboss.resteasy.util.GetRestful;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * @author Stéphane Épardaud <stef@epardaud.fr>
 */
public class ServiceRegistry {

    private static final Logger LOG = Logger.getLogger(ServiceRegistry.class.getName());

    private static final long serialVersionUID = -1985015444704126795L;

    private ResourceMethodRegistry registry;

    private ResteasyProviderFactory providerFactory;

    private ServiceRegistry parent;

    private ArrayList<MethodMetaData> methods;

    private ArrayList<ServiceRegistry> locators;

    private ResourceLocatorInvoker invoker;

    private String uri;

    private String functionPrefix;

    public ServiceRegistry getParent() {
        return parent;
    }

    public ServiceRegistry(ServiceRegistry parent, ResourceMethodRegistry registry,
                           ResteasyProviderFactory providerFactory, ResourceLocatorInvoker invoker) throws Exception {
        this.parent = parent;
        this.registry = registry;
        this.providerFactory = providerFactory;
        this.invoker = invoker;
        if (invoker != null) {
            Method method = invoker.getMethod();

            ResourceLocator resourceLocator = MethodMetaData.getResourceLocator(invoker);

            String methodPathVal = resourceLocator.getPath();
            String classPathVal = resourceLocator.getResourceClass().getPath();

            this.uri = MethodMetaData.appendURIFragments(parent, classPathVal, methodPathVal);

            if (parent.isRoot())
                this.functionPrefix = method.getDeclaringClass().getSimpleName() + "." + method.getName();
            else
                this.functionPrefix = parent.getFunctionPrefix() + "." + method.getName();
        }
        scanRegistry();
    }

    private void scanRegistry() throws Exception {
        methods = new ArrayList<MethodMetaData>();
        locators = new ArrayList<ServiceRegistry>();
        for (Entry<String, List<ResourceInvoker>> entry : registry.getBounded().entrySet()) {
            List<ResourceInvoker> invokers = entry.getValue();
            for (ResourceInvoker invoker : invokers) {
                if (invoker instanceof ResourceMethodInvoker) {
                    methods.add(new MethodMetaData(this, (ResourceMethodInvoker) invoker));
                } else if (invoker instanceof ResourceLocatorInvoker) {
                    ResourceLocatorInvoker locator = (ResourceLocatorInvoker) invoker;
                    Method method = locator.getMethod();
                    Class<?> locatorType = method.getReturnType();
                    Class<?>[] locatorResourceTypes = GetRestful.getSubResourceClasses(locatorType);
                    for (Class<?> locatorResourceType : locatorResourceTypes) {
                        if (locatorResourceType == null) {
                            // FIXME: we could generate an error for the client, which would be more informative than
                            // just logging this
                            LOG.warning("Error generating JS API: " + method.getDeclaringClass().getName() + ":" + method.getName());
                            // skip this
                            continue;
                        }
                        ResourceMethodRegistry locatorRegistry = new ResourceMethodRegistry(providerFactory);
                        locatorRegistry.addResourceFactory(null, null, locatorResourceType);
                        locators.add(new ServiceRegistry(this, locatorRegistry, providerFactory, locator));
                    }
                }
            }
        }
    }


    public List<MethodMetaData> getMethodMetaData() {
        return methods;
    }

    public List<ServiceRegistry> getLocators() {
        return locators;
    }

    public String getUri() {
        return uri;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public String getFunctionPrefix() {
        return functionPrefix;
    }

    public void collectResourceMethodsUntilRoot(List<Method> methods) {
        if (isRoot())
            return;
        methods.add(invoker.getMethod());
        parent.collectResourceMethodsUntilRoot(methods);
    }

}
