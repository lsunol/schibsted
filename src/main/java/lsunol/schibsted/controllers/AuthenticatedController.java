package lsunol.schibsted.controllers;

import com.sun.net.httpserver.HttpExchange;
import lsunol.schibsted.application.ApplicationConstants;
import lsunol.schibsted.model.Session;
import lsunol.schibsted.model.User;

import java.net.HttpURLConnection;
import java.util.logging.Logger;

/**
 * This abstract controller adds authentication functionality to any controller extending it.
 */
public abstract class AuthenticatedController extends ApplicationController {

    private final static Logger log = Logger.getLogger(AuthenticatedController.class.getName());

    /**
     * Returns the necessary role the requesting user must have in order to see its contents.
     */
    abstract String getRequiredRole();

    public void preRequestFilter(HttpExchange httpExchange, Session session, User requestUser) throws ResponseToClientException {

        if (requestUser == null) {
            // Remove the sessionid cookie
            unsetSessionCookie(httpExchange);
            // and show him the login page
            throw new ResponseToClientException(HttpURLConnection.HTTP_UNAUTHORIZED, "You must first authenticate.", "login-form");
        } else if ((getRequiredRole() != null && !requestUser.getRoles().contains(getRequiredRole())) && !requestUser.getRoles().contains(ApplicationConstants.ADMIN_ROLENAME)) {
            // The user is correctly logged in, but has no access to the resource
            throw new ResponseToClientException(HttpURLConnection.HTTP_FORBIDDEN, "You have no access to this resource.", "access-denied");
        } else {
            session.refreshSessionExpiryDate();
            setSessionCookie(httpExchange, session);
        }
    }
}
