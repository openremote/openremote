/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.app.client.admin.syslog;

import org.openremote.app.client.Environment;
import org.openremote.app.client.admin.AbstractAdminActivity;
import org.openremote.app.client.admin.AdminView;
import org.openremote.app.client.admin.navigation.AdminNavigation;
import org.openremote.app.client.event.SharedEventArrayMapper;
import org.openremote.app.client.event.ShowSuccessEvent;
import org.openremote.app.client.mvp.AcceptsView;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.syslog.SyslogConfig;
import org.openremote.model.syslog.SyslogEvent;
import org.openremote.model.syslog.SyslogLevel;
import org.openremote.model.syslog.SyslogResource;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AdminSyslogActivity
    extends AbstractAdminActivity<AdminSyslogPlace, AdminSyslog>
    implements AdminSyslog.Presenter {

    final Environment environment;
    final SyslogResource syslogResource;
    final SyslogConfigMapper syslogConfigMapper;
    final SharedEventArrayMapper sharedEventArrayMapper;

    protected SyslogConfig config;
    protected SyslogLevel filterLevel;
    protected int filterLimit;

    @Inject
    public AdminSyslogActivity(Environment environment,
                               AdminView adminView,
                               AdminNavigation.Presenter adminNavigationPresenter,
                               AdminSyslog view,
                               SyslogResource syslogResource,
                               SyslogConfigMapper syslogConfigMapper,
                               SharedEventArrayMapper sharedEventArrayMapper) {
        super(adminView, adminNavigationPresenter, view);
        this.environment = environment;
        this.syslogResource = syslogResource;
        this.syslogConfigMapper = syslogConfigMapper;
        this.sharedEventArrayMapper = sharedEventArrayMapper;
    }

    @Override
    protected String[] getRequiredRoles() {
        return new String[]{"read:admin"};
    }

    @Override
    public void start(AcceptsView container, EventBus eventBus, Collection<EventRegistration> registrations) {
        super.start(container, eventBus, registrations);
        adminContent.setPresenter(this);

        registrations.add(
            eventBus.register(SyslogEvent.class, adminContent::showEvents)
        );

        filterLevel = adminContent.getFilterLevel();
        filterLimit = adminContent.getFilterLimit();
        fetchEvents();
        loadConfig();
    }

    @Override
    public void onStop() {
        super.onStop();
        adminContent.setPresenter(null);
        environment.getEventService().unsubscribe(SyslogEvent.class);
    }

    @Override
    public void onClearLog() {
        adminContent.clearEvents();
    }

    @Override
    public void onContinueLog() {
        environment.getEventService().subscribe(
            SyslogEvent.class,
            new SyslogEvent.LevelCategoryFilter(filterLevel)
        );
    }

    @Override
    public void onPauseLog() {
        environment.getEventService().unsubscribe(SyslogEvent.class);
    }

    @Override
    public void onFilterLimitChanged(int limit) {
        adminContent.clearEvents();
        this.filterLimit = limit;
        fetchEvents();
    }

    @Override
    public void onFilterLevelChanged(SyslogLevel level) {
        adminContent.clearEvents();
        this.filterLevel = level;
        fetchEvents();
    }

    @Override
    public void saveSettings() {
        // TODO Client-side validation error handling?
        adminContent.setFormBusy(true);
        readFromView();
        environment.getApp().getRequestService().sendWith(
            syslogConfigMapper,
            requestParams -> {
                syslogResource.updateConfig(requestParams, config);
            },
            204,
            () -> {
                environment.getEventBus().dispatch(new ShowSuccessEvent(
                    environment.getMessages().settingsSaved()
                ));
                adminContent.setFormBusy(false);
            }
        );
    }

    @Override
    public void removeAll() {
        adminContent.setFormBusy(true);
        environment.getApp().getRequestService().send(
            syslogResource::clearEvents,
            204,
            () -> {
                adminContent.clearEvents();
                adminContent.setFormBusy(false);
                environment.getEventBus().dispatch(new ShowSuccessEvent(
                    environment.getMessages().eventsRemoved()
                ));
            }
        );
    }

    protected void fetchEvents() {
        adminContent.setFormBusy(true);
        environment.getApp().getRequestService().sendAndReturn(
            sharedEventArrayMapper,
            requestParams -> syslogResource.getEvents(requestParams, filterLevel, filterLimit),
            200,
            events -> {
                List<SyslogEvent> syslogEvents = new ArrayList<>();
                for (SharedEvent event : events) {
                    if (event instanceof SyslogEvent) {
                        SyslogEvent syslogEvent = (SyslogEvent) event;
                        syslogEvents.add(syslogEvent);
                    }
                }
                adminContent.showEvents(syslogEvents.toArray(new SyslogEvent[syslogEvents.size()]));
                adminContent.setFormBusy(false);

                environment.getEventService().subscribe(
                    SyslogEvent.class,
                    new SyslogEvent.LevelCategoryFilter(filterLevel)
                );
            }
        );
    }

    protected void loadConfig() {
        adminContent.setFormBusy(true);
        environment.getApp().getRequestService().sendAndReturn(
            syslogConfigMapper,
            syslogResource::getConfig,
            200,
            config -> {
                this.config = config;
                adminContent.setStoredLevel(config.getStoredLevel());
                adminContent.setStoredMinutes(config.getStoredMaxAgeMinutes());
                adminContent.setFormBusy(false);
            }
        );
    }

    protected void readFromView() {
        config.setStoredLevel(adminContent.geStoredLevel());
        config.setStoredMaxAgeMinutes(adminContent.getStoredMinutes());
    }
}
