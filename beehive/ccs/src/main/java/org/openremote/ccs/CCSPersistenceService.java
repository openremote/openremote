package org.openremote.ccs;

import org.apache.commons.codec.binary.Base64;
import org.openremote.ccs.model.*;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.controllercommand.domain.ControllerCommandDTO;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CCSPersistenceService extends PersistenceService {

    public static final String HTTP_AUTH_HEADER_NAME = "Authorization";
    public static final String HTTP_BASIC_AUTH_HEADER_VALUE_PREFIX = "Basic ";

    public User loadByUsername(EntityManager entityManager, String username) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = criteriaBuilder.createQuery(User.class);
        Root<User> root = query.from(User.class);
        query.select(root);
        query.where(criteriaBuilder.equal(root.get("username"), username));
        return entityManager.createQuery(query).getSingleResult();
    }

    public User loadByHTTPBasicCredentials(EntityManager entityManager, String credentials) {
        if (credentials.startsWith(HTTP_BASIC_AUTH_HEADER_VALUE_PREFIX)) {
            credentials = credentials.replaceAll(HTTP_BASIC_AUTH_HEADER_VALUE_PREFIX, "");
            credentials = new String(Base64.decodeBase64(credentials.getBytes()));
            String[] arr = credentials.split(":");
            if (arr.length == 2) {
                String username = arr[0];
                String password = arr[1];
                User user = loadByUsername(entityManager, username);
                String encodedPassword = new Md5PasswordEncoder().encodePassword(password, username);
                if (user != null && user.getPassword().equals(encodedPassword)) {
                    return user;
                }
            }
        }
        // let's be lax and not throw a BAD_REQUEST to allow the user to retry
        return null;
    }

    public void save(EntityManager entityManager, ControllerCommand c) {
        entityManager.persist(c);
    }

    public List<ControllerCommandDTO> queryByControllerOidForUser(EntityManager entityManager, Long oid, String username) {

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = criteriaBuilder.createQuery(User.class);
        Root<User> root = query.from(User.class);
        query.select(root);
        query.where(criteriaBuilder.equal(root.get("username"), username));
        User u = entityManager.createQuery(query).getSingleResult();

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
        criteria = criteriaBuilder.and(criteria, criteriaBuilder.equal(controllerCommandRoot.get("state"), ControllerCommand.State.OPEN));
        controllerCommandQuery.where(criteria);
        controllerCommandQuery.orderBy(criteriaBuilder.asc(controllerCommandRoot.get("creationDate")));

        List<ControllerCommand> list = entityManager.createQuery(controllerCommandQuery).getResultList();
        List<ControllerCommandDTO> result = new ArrayList<>(list.size());
        for (ControllerCommand cmd : list) {
            ControllerCommandDTO dto = new ControllerCommandDTO();
            dto.setOid(cmd.getId());
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
}
