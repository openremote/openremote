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
package org.openremote.app.client.admin.users.edit;

import org.openremote.app.client.Environment;
import org.openremote.app.client.admin.*;
import org.openremote.app.client.admin.navigation.AdminNavigation;
import org.openremote.app.client.admin.users.AdminUsersPlace;
import org.openremote.app.client.event.ShowSuccessEvent;
import org.openremote.app.client.mvp.AcceptsView;
import org.openremote.app.client.mvp.AppActivity;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;
import org.openremote.model.http.ConstraintViolation;
import org.openremote.model.interop.Consumer;
import org.openremote.model.notification.NotificationResource;
import org.openremote.model.security.Credential;
import org.openremote.model.security.Role;
import org.openremote.model.security.User;
import org.openremote.model.security.UserResource;
import org.openremote.model.interop.Runnable;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class AdminUserEditActivity
    extends AbstractAdminActivity<AdminUserEditPlace, AdminUserEdit>
    implements AdminUserEdit.Presenter {

    final protected Environment environment;
    final protected UserResource userResource;
    final protected UserMapper userMapper;
    final protected CredentialMapper credentialMapper;
    final protected RoleArrayMapper roleArrayMapper;
    final protected NotificationResource notificationResource;
    final protected Consumer<ConstraintViolation[]> validationErrorHandler;

    protected AdminUserEditPlace place;
    protected String realm;
    protected String userId;
    protected User user;
    protected Role[] roles = new Role[0];

    @Inject
    public AdminUserEditActivity(Environment environment,
                                 AdminView adminView,
                                 AdminNavigation.Presenter adminNavigationPresenter,
                                 AdminUserEdit view,
                                 UserResource userResource,
                                 UserMapper userMapper,
                                 CredentialMapper credentialMapper,
                                 RoleArrayMapper roleArrayMapper,
                                 NotificationResource notificationResource) {
        super(adminView, adminNavigationPresenter, view);
        this.environment = environment;
        this.userResource = userResource;
        this.userMapper = userMapper;
        this.credentialMapper = credentialMapper;
        this.roleArrayMapper = roleArrayMapper;
        this.notificationResource = notificationResource;
        this.validationErrorHandler = violations -> {
            for (ConstraintViolation violation : violations) {
                if (violation.getConstraintType() == ConstraintViolation.Type.CONFLICT) {
                    adminContent.addFormMessageError(environment.getMessages().conflictRequest());
                    adminContent.setUsernameError(true);
                } else if (violation.getPath() != null) {
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
                    adminContent.addFormMessageError(violation.getMessage());
                }
            }
            adminContent.setFormBusy(false);
        };
    }

    @Override
    protected String[] getRequiredRoles() {
        return new String[]{"read:admin", "write:admin"};
    }

    @Override
    protected AppActivity<AdminUserEditPlace> init(AdminUserEditPlace place) {
        realm = place.getRealm();
        userId = place.getUserId();
        return super.init(place);
    }

    @Override
    public void start(AcceptsView container, EventBus eventBus, Collection<EventRegistration> registrations) {
        super.start(container, eventBus, registrations);

        adminContent.setPresenter(this);

        adminContent.clearRoles();
        adminContent.clearFormMessages();
        clearViewFieldErrors();
        adminContent.enableCreate(false);
        adminContent.enableUpdate(false);
        adminContent.enableDelete(false);
        adminContent.enableResetPassword(false);
        adminContent.enableRoles(false);
        adminContent.setEditMode(false);

        if (userId != null) {
            adminContent.setEditMode(true);
            loadUser();
        } else {
            user = new User();
            user.setRealm(realm);
            writeToView();
            adminContent.enableCreate(true);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        adminContent.setPresenter(null);
        adminContent.clearRoles();
        adminContent.clearFormMessages();
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
        adminContent.clearFormMessages();
        clearViewFieldErrors();
        readFromView();
        environment.getApp().getRequests().sendWith(
            userMapper,
            requestParams -> userResource.create(requestParams, realm, user),
            204,
            () -> {
                adminContent.setFormBusy(false);
                environment.getEventBus().dispatch(new ShowSuccessEvent(
                    environment.getMessages().userCreated(user.getUsername())
                ));
                environment.getPlaceController().goTo(new AdminUsersPlace(realm));
            },
            validationErrorHandler
        );
    }

    @Override
    public void update() {
        adminContent.setFormBusy(true);
        adminContent.clearFormMessages();
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
            ConstraintViolation violation = new ConstraintViolation();
            violation.setConstraintType(ConstraintViolation.Type.FIELD);
            violation.setPath("password");
            violation.setMessage(environment.getMessages().passwordsMustMatch());
            validationErrorHandler.accept(new ConstraintViolation[]{violation});
            return;
        }
        Credential credential = new Credential(password, false);
        environment.getApp().getRequests().sendWith(
            credentialMapper,
            requestParams -> userResource.resetPassword(requestParams, realm, userId, credential),
            204,
            () -> adminContent.addFormMessageSuccess(environment.getMessages().passwordUpdated())
        );
    }

    protected void updateUser() {
        environment.getApp().getRequests().sendWith(
            userMapper,
            requestParams -> userResource.update(requestParams, realm, userId, user),
            204,
            () -> updateRoles(() -> {
                adminContent.setFormBusy(false);
                adminContent.addFormMessageSuccess(environment.getMessages().userUpdated(user.getUsername()));
            }),
            validationErrorHandler
        );
    }

    protected void updateRoles(Runnable onComplete) {
        environment.getApp().getRequests().sendWith(
            roleArrayMapper,
            requestParams -> userResource.updateRoles(requestParams, realm, userId, roles),
            204,
            onComplete
        );
    }

    @Override
    public void delete() {
        adminContent.showConfirmation(
            environment.getMessages().confirmation(),
            environment.getMessages().confirmationDelete(user.getUsername()),
            () -> {
                adminContent.setFormBusy(true);
                adminContent.clearFormMessages();
                clearViewFieldErrors();
                environment.getApp().getRequests().send(
                    requestParams -> userResource.delete(requestParams, realm, userId),
                    204,
                    () -> {
                        adminContent.setFormBusy(false);
                        environment.getEventBus().dispatch(new ShowSuccessEvent(
                            environment.getMessages().userDeleted(user.getUsername())
                        ));
                        environment.getPlaceController().goTo(new AdminUsersPlace(realm));
                    }
                );
            }
        );
    }

    protected void loadUser() {
        adminContent.setFormBusy(true);
        environment.getApp().getRequests().sendAndReturn(
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
                });
            }
        );
    }

    protected void loadRoles(Runnable onComplete) {
        environment.getApp().getRequests().sendAndReturn(
            roleArrayMapper,
            requestParams -> userResource.getRoles(requestParams, realm, userId),
            200,
            roles -> {
                List<Role> roleList = new ArrayList<>();
                roleList.addAll(Arrays.asList(roles));
                roleList.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
                this.roles = roleList.toArray(new Role[roleList.size()]);
                adminContent.enableRoles(true);
                onComplete.run();
            }
        );
    }

    protected void writeToView() {
        adminContent.setTenantName(realm);
        adminContent.setUsername(user.getUsername());
        adminContent.setFirstName(user.getFirstName());
        adminContent.setLastName(user.getLastName());
        adminContent.setEmail(user.getEmail());
        adminContent.setUserEnabled(user.getEnabled());
        for (Role role : roles) {
            adminContent.addRole(
                role.getId(),
                environment.getMessages().roleLabel(role.getName().replaceAll(":", "-")),
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
