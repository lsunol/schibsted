package lsunol.schibsted.controllers;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpExchange;
import lsunol.schibsted.controllers.annotations.HttpProduces;
import lsunol.schibsted.database.DuplicateKeyException;
import lsunol.schibsted.database.RepositoryManager;
import lsunol.schibsted.database.UserRepository;
import lsunol.schibsted.model.User;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public class UserRestApiController extends ApplicationController {
    private final static Logger log = Logger.getLogger(UserRestApiController.class.getName());

    private final static String REQUEST_PATH = "/api/users";

    UserRepository userRepository = RepositoryManager.getUserRepository();

    @Override
    public String getRequestMapping() {
        return REQUEST_PATH;
    }

    /**
     * Returns a {@link BasicAuthenticator} for the controller, which accepts a "admin" login only.
     * @return a {@link BasicAuthenticator} for the controller, which accepts a "admin" login only.
     */
    public Authenticator getAuthenticator() {
        return new BasicAuthenticator("schibstedRealm") {
            @Override
            public boolean checkCredentials(String user, String password) {
                return user.equals("admin") && userRepository.checkUserCredentials(user, password);
            }
        };
    }

    /**
     * Handler for the GET REST API method.
     * Returns the queried user (if present) to the http client in an acceptable response format.
     * @param httpExchange
     */
    @HttpProduces(name = "application/json;text/plain")
    public final void doGet(HttpExchange httpExchange) throws ResponseToClientException, IOException {
        String targetUserName = getUserPathParam(httpExchange);
        User targetUser = userRepository.getUserByUsername(targetUserName);
        if (targetUserName == null || targetUserName.isEmpty()) sendResponse(httpExchange, HttpURLConnection.HTTP_OK, userRepository.getAllUsers());
        else if (targetUser != null) sendResponse(httpExchange, HttpURLConnection.HTTP_OK, targetUser);
        else sendResponse(httpExchange, HttpURLConnection.HTTP_NOT_FOUND, new HashMap<String, String>() {{ put("op", "get"); put("msg", "User '" + targetUserName + "' not found."); }});
    }

    /**
     * Handler for the POST REST API method.
     * Creates a new user in the system.
     * @param httpExchange
     */
    @HttpProduces(name = "application/json;text/plain")
    public final void doPost(HttpExchange httpExchange) throws ResponseToClientException, IOException {
        String targetUserPathParam = getUserPathParam(httpExchange);
        try {
            JSONObject newUserValues = getRequestBodyAsJson(httpExchange);
            try {
                String newPassword = newUserValues.getString("password");
                List<String> newUserRoles = getListFromJsonArray(newUserValues.getJSONArray("roles"));
                userRepository.addNewUser(targetUserPathParam, newPassword, newUserRoles);
                sendResponse(httpExchange, HttpURLConnection.HTTP_CREATED, "User '" + targetUserPathParam + "' created successfully.");
            } catch (DuplicateKeyException e) {
                throw new ResponseToClientException(HttpURLConnection.HTTP_CONFLICT, e.getMessage());
            } catch (JSONException e) {
                // This exception may occur if any of the parameters is missing
                throw new ResponseToClientException(422, "Mandatory parameter missing in the JSON: " + e.getMessage());
            }
        } catch (JSONException e) {
            String errorMessage = "The request body provided is not a valid JSON.";
            log.info(errorMessage);
            throw new ResponseToClientException(HttpURLConnection.HTTP_BAD_REQUEST, errorMessage);
        }
    }

    /**
     * Handler for the PUT REST API method.
     * Modifies a user in the system.
     * @param httpExchange
     * @throws ResponseToClientException if the JSON provided lacks any mandatory parameter
     * @throws IOException when an error occurs while reading from the input stream or writing to the output stream.
     */
    @HttpProduces(name = "application/json")
    public final void doPut(HttpExchange httpExchange) throws ResponseToClientException, IOException {
        try {
            JSONObject newUserValues = getRequestBodyAsJson(httpExchange);
            String targetUserPathParam = getUserPathParam(httpExchange);
            User targetUser = userRepository.getUserByUsername(targetUserPathParam);
            if (targetUser == null) throw new ResponseToClientException(HttpURLConnection.HTTP_NOT_FOUND, "User '" + targetUserPathParam + "' not found.");
            if (newUserValues.keySet().contains("password")) targetUser.setPassword(newUserValues.optString("password"));
            if (newUserValues.keySet().contains("roles")) targetUser.setRoles(getListFromJsonArray(newUserValues.optJSONArray("roles")));
            sendResponse(httpExchange, HttpURLConnection.HTTP_NO_CONTENT);
        } catch (JSONException e) {
            String errorMessage = "The request body provided is not a valid JSON.";
            log.info(errorMessage);
            throw new ResponseToClientException(HttpURLConnection.HTTP_BAD_REQUEST, errorMessage);
        }
    }

    /**
     * Handler for the DELETE REST API method.
     * Deletes a user from the system.
     *
     * @param
     * httpExchange
     */
    @HttpProduces(name = "application/json;text/plain")
    public final void doDelete(HttpExchange httpExchange) throws ResponseToClientException, IOException {
        String userPathParam = getUserPathParam(httpExchange);
        if (userRepository.getUserByUsername(userPathParam) == null) throw new ResponseToClientException(HttpURLConnection.HTTP_NOT_FOUND, "Could not find user '" + userPathParam + "'.");
        else {
            userRepository.deleteUser(userPathParam);
            sendResponse(httpExchange, HttpURLConnection.HTTP_NO_CONTENT);
        }
    }

    /**
     * Returns the path param representing the queried user in a GET operation.
     * @param httpExchange
     * @return
     */
    private static String getUserPathParam(HttpExchange httpExchange) {
        String queriedUsername = httpExchange.getRequestURI().getPath().replaceFirst(REQUEST_PATH + "/", "");
        return queriedUsername.contains("/") ? queriedUsername.substring(0, queriedUsername.indexOf("/")) : queriedUsername;
    }

    /**
     * Returns a {@link List<String>} from a {@link JSONArray} containing Strings
     * @param jsonArray
     * @return
     */
    private static List<String> getListFromJsonArray(JSONArray jsonArray) {
        List<String> newRoles = new LinkedList<>();
        for (int i = 0; i < jsonArray.length(); i++) newRoles.add(jsonArray.getString(i));
        return newRoles;
    }
}
