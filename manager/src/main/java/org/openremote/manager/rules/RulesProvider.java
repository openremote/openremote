package org.openremote.manager.rules;

import org.kie.api.KieServices;
import org.kie.api.io.Resource;
import org.kie.api.runtime.conf.ClockTypeOption;

import java.util.stream.Stream;

public interface RulesProvider {

    Stream<Resource> getResources(KieServices kieServices);

    ClockTypeOption getClockType();
}
