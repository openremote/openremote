package org.openremote.manager.i18n;

import org.openremote.container.Container;
import org.openremote.container.ContainerService;

import java.util.Locale;
import java.util.ResourceBundle;

public class I18NService implements ContainerService {

    protected ResourceBundle validationMessages;

    @Override
    public int getPriority() {
        return ContainerService.HIGH_PRIORITY + 400;
    }

    @Override
    public void init(Container container) throws Exception {
        // TODO configurable per user session
        Locale.setDefault(Locale.US);
        validationMessages = ResourceBundle.getBundle("ValidationMessages", Locale.getDefault());
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    public ResourceBundle getValidationMessages() {
        return validationMessages;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}
