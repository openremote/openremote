package org.openremote.manager.web;

import jakarta.ws.rs.core.Application;

import java.util.HashSet;
import java.util.Set;

/**
 * This is a custom JAX-RS {@link Application} that allows resources to be programmatically statically
 * registered at startup.
 */
// TODO: Replace this with Jakarta servlet discovery and CDI once this is available
public class ManagerAPIApplication extends Application {
    protected static Set<Object> SINGLETONS = new HashSet<>();
    protected static Set<Class<?>> CLASSES = new HashSet<>();

    public static void addSingleton(Object singleton) {
        SINGLETONS.add(singleton);
    }

    public static void addClass(Class<?> clazz) {
        CLASSES.add(clazz);
    }

    @Override
    public Set<Object> getSingletons() {
        return SINGLETONS;
    }

    @Override
    public Set<Class<?>> getClasses() {
        return CLASSES;
    }

    public static void clear() {
        SINGLETONS.clear();
        CLASSES.clear();
    }
}
