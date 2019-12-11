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

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;
import org.openremote.app.client.Environment;
import org.openremote.app.client.admin.AbstractAdminActivity;
import org.openremote.app.client.admin.AdminView;
import org.openremote.app.client.admin.navigation.AdminNavigation;
import org.openremote.app.client.event.SharedEventArrayMapper;
import org.openremote.app.client.event.ShowSuccessEvent;
import org.openremote.app.client.mvp.AcceptsView;
import org.openremote.model.Constants;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.http.RequestParams;
import org.openremote.model.syslog.*;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AdminSyslogActivity
    extends AbstractAdminActivity<AdminSyslogPlace, AdminSyslog>
    implements AdminSyslog.Presenter {

    // Crappy hack to keep GWT working
    @JsType(isNative = true)
    public interface GwtSyslogResource extends SyslogResource {
        @JsMethod(name = "getEvents")
        List<SyslogEvent> getEventsGwt(@BeanParam RequestParams requestParams, @QueryParam("level") SyslogLevel level, @QueryParam("per_page") Integer perPage, @QueryParam("page") Integer page, @QueryParam("from") Long from, @QueryParam("to") Long to, @QueryParam("category") List<SyslogCategory> categories, @QueryParam("subCategory") List<String> subCategories);
    }

    final Environment environment;
    final GwtSyslogResource syslogResource;
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
                               GwtSyslogResource syslogResource,
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
        return new String[]{Constants.READ_ADMIN_ROLE};
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
        environment.getApp().getRequests().sendWith(
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
        environment.getApp().getRequests().send(
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
        environment.getApp().getRequests().sendAndReturn(
            sharedEventArrayMapper,
            requestParams -> syslogResource.getEventsGwt(requestParams, filterLevel, filterLimit, null, null, null, null, null),
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
        environment.getApp().getRequests().sendAndReturn(
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
