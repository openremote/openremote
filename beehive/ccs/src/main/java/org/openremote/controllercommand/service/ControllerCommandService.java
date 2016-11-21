/* OpenRemote, the Home of the Digital Home.
 * Copyright 2008-2011, OpenRemote Inc.
 *
 * See the contributors.txt file in the distribution for a
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
package org.openremote.controllercommand.service;

import org.openremote.controllercommand.GenericDAO;
import org.openremote.controllercommand.domain.Account;
import org.openremote.controllercommand.domain.Controller;
import org.openremote.controllercommand.domain.ControllerCommand;
import org.openremote.controllercommand.domain.ControllerCommand.State;
import org.openremote.controllercommand.domain.ControllerCommandDTO;
import org.openremote.controllercommand.domain.InitiateProxyControllerCommand;
import org.openremote.controllercommand.domain.User;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ControllerCommand service implementation.
 *
 * @author Stef Epardaud
 */
public class ControllerCommandService {

    protected GenericDAO genericDAO;

    public void setGenericDAO(GenericDAO genericDAO) {
        this.genericDAO = genericDAO;
    }

    public void save(EntityManager entityManager, ControllerCommand c) {
        entityManager.persist(c);
    }

    public List<ControllerCommandDTO> queryByControllerOidForUser(EntityManager entityManager, Long oid, String username) {
        User u = genericDAO.getByNonIdField(entityManager, User.class, "username", username);


        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Controller> controllerQuery = criteriaBuilder.createQuery(Controller.class);
        Root<Controller> controllerRoot = controllerQuery.from(Controller.class);
        Fetch<Controller, Account> accountJoin = controllerRoot.fetch("account");
        accountJoin.fetch("users");
        controllerQuery.select(controllerRoot);
        controllerQuery.where(criteriaBuilder.equal(controllerRoot.get("oid"), oid));
        Controller controller;
        try {
            controller = entityManager.createQuery(controllerQuery).getSingleResult();
        } catch (NoResultException e) {
            return Collections.emptyList();
        }

        // Only return commands if the user making the request has access to the account linked to the controller
        if (!controller.getAccount().getUsers().contains(u)) {
            return Collections.emptyList();
        }

        CriteriaQuery<ControllerCommand> controllerCommandQuery = criteriaBuilder.createQuery(ControllerCommand.class);
        Root<ControllerCommand> controllerCommandRoot = controllerCommandQuery.from(ControllerCommand.class);
        controllerCommandQuery.select(controllerCommandRoot);
        Predicate criteria = criteriaBuilder.equal(controllerCommandRoot.get("account"), controller.getAccount());
        criteria = criteriaBuilder.and(criteria, criteriaBuilder.equal(controllerCommandRoot.get("state"), State.OPEN));
        controllerCommandQuery.where(criteria);
        controllerCommandQuery.orderBy(criteriaBuilder.asc(controllerCommandRoot.get("creationDate")));

        List<ControllerCommand> list = entityManager.createQuery(controllerCommandQuery).getResultList();
        List<ControllerCommandDTO> result = new ArrayList<ControllerCommandDTO>(list.size());
        for (ControllerCommand cmd : list) {
            ControllerCommandDTO dto = new ControllerCommandDTO();
            dto.setOid(cmd.getOid());
            dto.setCommandType(cmd.getType().getLabel());
            if (cmd instanceof InitiateProxyControllerCommand) {
                dto.addParameter("token", ((InitiateProxyControllerCommand) cmd).getToken());
                dto.addParameter("url", ((InitiateProxyControllerCommand) cmd).getUrl());
            }
            result.add(dto);
        }
        return result;
    }

    public InitiateProxyControllerCommand saveProxyControllerCommand(EntityManager entityManager, User user, String url) {
        InitiateProxyControllerCommand command = new InitiateProxyControllerCommand(user.getAccount(), ControllerCommandDTO.Type.INITIATE_PROXY, url);
        save(entityManager, command);
        return command;
    }

    public void closeControllerCommand(ControllerCommand controllerCommand) {
        controllerCommand.setState(State.DONE);
    }

    public ControllerCommand findControllerCommandById(EntityManager entityManager, Long id) {
        return entityManager.find(ControllerCommand.class, id);
    }

    public void update(EntityManager entityManager, ControllerCommand controllerCommand) {
        entityManager.merge(controllerCommand);
    }

}
