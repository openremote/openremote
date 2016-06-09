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
package org.openremote.manager.client.admin.users;

import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.admin.*;
import org.openremote.manager.client.admin.navigation.AdminNavigation;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.client.service.RequestService;
import org.openremote.manager.client.service.SecurityService;
import org.openremote.manager.shared.Consumer;
import org.openremote.manager.shared.Runnable;
import org.openremote.manager.shared.event.ui.ShowInfoEvent;
import org.openremote.manager.shared.security.Credential;
import org.openremote.manager.shared.security.Role;
import org.openremote.manager.shared.security.User;
import org.openremote.manager.shared.security.UserResource;
import org.openremote.manager.shared.validation.ConstraintViolation;

import javax.inject.Inject;
import java.util.*;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class AdminUserActivity
    extends AbstractAdminActivity<AdminUserPlace, AdminUser>
    implements AdminUser.Presenter {

    final protected ManagerMessages managerMessages;
    final protected PlaceController placeController;
    final protected EventBus eventBus;
    final protected SecurityService securityService;
    final protected RequestService requestService;
    final protected UserResource userResource;
    final protected UserMapper userMapper;
    final protected CredentialMapper credentialMapper;
    final protected RoleArrayMapper roleArrayMapper;

    final protected Consumer<ConstraintViolation[]> validationErrorHandler = violations -> {
        for (ConstraintViolation violation : violations) {
            if (violation.getPath() != null) {
                if (violation.getPath().endsWith("username")) {
                    adminContent.setUsernameError(true);
                }
                if (violation.getPath().endsWith("firstName")) {
                    adminContent.setFirstNameError(true);
                }
                if (violation.getPath().endsWith("lastName")) {
                    adminContent.setLastNameError(true);
                }
                if (violation.getPath().endsWith("email")) {
                    adminContent.setEmailError(true);
                }
                if (violation.getPath().endsWith("enabled")) {
                    adminContent.setUserEnabledError(true);
                }
                if (violation.getPath().endsWith("password")) {
                    adminContent.setPasswordError(true);
                }
            }
            adminContent.addFormMessageError(violation.getMessage());
        }
        adminContent.setFormBusy(false);
    };

    protected String realm;
    protected String userId;
    protected User user;
    protected Role[] roles = new Role[0];

    @Inject
    public AdminUserActivity(AdminView adminView,
                             AdminNavigation.Presenter adminNavigationPresenter,
                             AdminUser view,
                             ManagerMessages managerMessages,
                             PlaceController placeController,
                             EventBus eventBus,
                             SecurityService securityService,
                             RequestService requestService,
                             UserResource userResource,
                             UserMapper userMapper,
                             CredentialMapper credentialMapper,
                             RoleArrayMapper roleArrayMapper) {
        super(adminView, adminNavigationPresenter, view);
        this.managerMessages = managerMessages;
        this.placeController = placeController;
        this.eventBus = eventBus;
        this.securityService = securityService;
        this.requestService = requestService;
        this.userResource = userResource;
        this.userMapper = userMapper;
        this.credentialMapper = credentialMapper;
        this.roleArrayMapper = roleArrayMapper;
    }

    @Override
    protected String[] getRequiredRoles() {
        return new String[]{"read:admin", "write:admin"};
    }

    @Override
    protected AppActivity<AdminUserPlace> init(AdminUserPlace place) {
        realm = place.getRealm();
        userId = place.getUserId();
        return super.init(place);
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        super.start(container, eventBus, registrations);

        adminContent.setPresenter(this);

        adminContent.clearRoles();
        adminContent.clearFormMessagesSuccess();
        adminContent.clearFormMessagesError();
        clearViewFieldErrors();
        adminContent.enableCreate(false);
        adminContent.enableUpdate(false);
        adminContent.enableDelete(false);
        adminContent.enableResetPassword(false);
        adminContent.enableRoles(false);
        adminContent.setUsernameEditEnabled(false);

        if (userId != null) {
            loadUser();
        } else {
            user = new User();
            user.setRealm(realm);
            writeToView();
            adminContent.enableCreate(true);
            adminContent.setUsernameEditEnabled(true);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        adminContent.setPresenter(null);
        adminContent.clearRoles();
        adminContent.clearFormMessagesSuccess();
        adminContent.clearFormMessagesError();
        clearViewFieldErrors();
    }

    @Override
    public void onRoleAssigned(String id, boolean assigned) {
        for (Role role : roles) {
            if (!role.getId().equals(id)) {
                continue;
            }
            role.setAssigned(assigned);

            // A composite role switches all other roles with same name prefix
            if (role.isComposite()) {
                for (Role otherRole : roles) {
                    if (otherRole.getName().startsWith(role.getName() + ":")) {
                        otherRole.setAssigned(assigned);
                        adminContent.toggleRoleAssigned(otherRole.getId(), assigned);
                    }
                }
                // A non-composite role disables its composite parent
            } else if (!assigned) {
                int colonIndex = role.getName().indexOf(":");
                String prefix = role.getName().substring(0, colonIndex != -1 ? colonIndex : role.getName().length() - 1);
                for (Role otherRole : roles) {
                    if (otherRole.getName().equals(prefix)) {
                        otherRole.setAssigned(false);
                        adminContent.toggleRoleAssigned(otherRole.getId(), false);
                    }
                }
            }
        }
    }

    @Override
    public void create() {
        adminContent.setFormBusy(true);
        adminContent.clearFormMessagesSuccess();
        adminContent.clearFormMessagesError();
        clearViewFieldErrors();
        readFromView();
        requestService.execute(
            userMapper,
            requestParams -> {
                userResource.create(requestParams, realm, user);
            },
            204,
            () -> {
                adminContent.setFormBusy(false);
                eventBus.dispatch(new ShowInfoEvent(
                    managerMessages.userCreated(user.getUsername())
                ));
                placeController.goTo(new AdminUsersPlace(realm));
            },
            ex -> handleRequestException(ex, eventBus, managerMessages, validationErrorHandler)
        );
    }

    @Override
    public void update() {
        adminContent.setFormBusy(true);
        adminContent.clearFormMessagesSuccess();
        adminContent.clearFormMessagesError();
        clearViewFieldErrors();
        readFromView();
        handlePasswordReset();
        updateUser();
    }

    protected void handlePasswordReset() {
        String password = adminContent.getPassword();
        String passwordControl = adminContent.getPasswordControl();
        adminContent.clearPassword();
        adminContent.clearPasswordControl();
        if (password == null)
            return;
        if (!password.equals(passwordControl)) {
            validationErrorHandler.accept(new ConstraintViolation[]{
                new ConstraintViolation(
                    ConstraintViolation.Type.FIELD, "password", managerMessages.passwordsMustMatch()
                )
            });
            return;
        }
        Credential credential = new Credential(password, false);
        requestService.execute(
            credentialMapper,
            requestParams -> {
                userResource.resetPassword(requestParams, realm, userId, credential);
            },
            204,
            () -> {
                adminContent.addFormMessageSuccess(managerMessages.passwordUpdated());
            },
            ex -> handleRequestException(ex, eventBus, managerMessages, validationErrorHandler)
        );
    }

    protected void updateUser() {
        requestService.execute(
            userMapper,
            requestParams -> {
                userResource.update(requestParams, realm, userId, user);
            },
            204,
            () -> {
                updateRoles(() -> {
                    adminContent.setFormBusy(false);
                    adminContent.addFormMessageSuccess(managerMessages.userUpdated(user.getUsername()));
                });
            },
            ex -> handleRequestException(ex, eventBus, managerMessages, validationErrorHandler)
        );
    }

    protected void updateRoles(Runnable onComplete) {
        requestService.execute(
            roleArrayMapper,
            requestParams -> {
                userResource.updateRoles(requestParams, realm, userId, roles);
            },
            204,
            onComplete::run,
            ex -> handleRequestException(ex, eventBus, managerMessages)
        );
    }

    @Override
    public void delete() {
        adminContent.setFormBusy(true);
        adminContent.clearFormMessagesSuccess();
        adminContent.clearFormMessagesError();
        clearViewFieldErrors();
        requestService.execute(
            requestParams -> {
                userResource.delete(requestParams, realm, userId);
            },
            204,
            () -> {
                adminContent.setFormBusy(false);
                eventBus.dispatch(new ShowInfoEvent(
                    managerMessages.userDeleted(user.getUsername())
                ));
                placeController.goTo(new AdminUsersPlace(realm));
            },
            ex -> handleRequestException(ex, eventBus, managerMessages, validationErrorHandler)
        );
    }

    @Override
    public void cancel() {
        placeController.goTo(new AdminUsersPlace(realm));
    }

    protected void loadUser() {
        adminContent.setFormBusy(true);
        requestService.execute(
            userMapper,
            requestParams -> userResource.get(requestParams, realm, userId),
            200,
            user -> {
                this.user = user;
                this.realm = user.getRealm();
                loadRoles(() -> {
                    writeToView();
                    adminContent.setFormBusy(false);
                    adminContent.enableCreate(false);
                    adminContent.enableUpdate(true);
                    adminContent.enableDelete(true);
                    adminContent.enableResetPassword(true);
                    adminContent.setUsernameEditEnabled(false);
                });
            },
            ex -> handleRequestException(ex, eventBus, managerMessages)
        );
    }

    protected void loadRoles(Runnable onComplete) {
        requestService.execute(
            roleArrayMapper,
            requestParams -> userResource.getRoles(requestParams, realm, userId),
            200,
            roles -> {
                List<Role> roleList = new ArrayList<>();
                roleList.addAll(Arrays.asList(roles));
                Collections.sort(roleList, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
                this.roles = roleList.toArray(new Role[roleList.size()]);
                adminContent.enableRoles(true);
                onComplete.run();
            },
            ex -> handleRequestException(ex, eventBus, managerMessages)
        );
    }

    protected void writeToView() {
        adminContent.setUsername(user.getUsername());
        adminContent.setFirstName(user.getFirstName());
        adminContent.setLastName(user.getLastName());
        adminContent.setEmail(user.getEmail());
        adminContent.setUserEnabled(user.getEnabled());
        for (Role role : roles) {
            adminContent.addRole(
                role.getId(),
                managerMessages.roleLabel(role.getName().replaceAll(":", "-")),
                role.isComposite(),
                role.isAssigned()
            );
        }
    }

    protected void readFromView() {
        user.setUsername(adminContent.getUsername());
        user.setFirstName(adminContent.getFirstName());
        user.setLastName(adminContent.getLastName());
        user.setEmail(adminContent.getEmail());
        user.setEnabled(adminContent.getUserEnabled());
    }

    protected void clearViewFieldErrors() {
        adminContent.setUsernameError(false);
        adminContent.setFirstNameError(false);
        adminContent.setLastNameError(false);
        adminContent.setEmailError(false);
        adminContent.setUserEnabledError(false);
        adminContent.setPasswordError(false);
    }

}
