package lsunol.schibsted.controllers;

import com.sun.net.httpserver.HttpExchange;
import lsunol.schibsted.controllers.annotations.HttpProduces;
import lsunol.schibsted.database.RepositoryManager;
import lsunol.schibsted.database.SessionRepository;
import lsunol.schibsted.database.UserRepository;
import lsunol.schibsted.model.Session;
import lsunol.schibsted.model.User;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LoginController extends ApplicationController {

    private final static Logger log = Logger.getLogger(LoginController.class.getName());

    private static final String PATH = "/login";

    private static UserRepository userRepository = RepositoryManager.getUserRepository();
    private static SessionRepository sessionRepository = RepositoryManager.getSessionRepository();

    @Override
    public String getRequestMapping() {
        return PATH;
    }

    @HttpProduces(name = "text/html")
    public String doGet() {
        return "login-form";
    }

    @HttpProduces(name = "text/html")
    public String doPost(HttpExchange httpExchange, Map<String, String> templateAttributes) throws ResponseToClientException {
        try {
            Map<String, String> postParams = getBodyAsParametersMap(httpExchange);
            String username = postParams.get("username");
            String password = postParams.get("password");
            String targetResource = postParams.get("targetResource");

            User user = userRepository.getUserByUsername(username);
            if (user != null && user.isPasswordCorrect(password)) {
                Session session = sessionRepository.generateSessionForUser(user);
                setSessionCookie(httpExchange, session);
                templateAttributes.put("username", user.getUsername());
                templateAttributes.put("roles", user.getRoles().stream().collect(Collectors.joining(", ")));
                if (targetResource == null || !targetResource.startsWith("page")) return "home";
                else return targetResource;
            } else {
                throw new ResponseToClientException(HttpURLConnection.HTTP_UNAUTHORIZED, "User credentials are not correct.", "login-form");
            }
        } catch (IOException ioe) {
            // Could not read the input form parameters (username and password)
            throw new ResponseToClientException(HttpURLConnection.HTTP_BAD_REQUEST, "Please, provide a valid username and password to log in.", "login-form");
        }
    }
}
