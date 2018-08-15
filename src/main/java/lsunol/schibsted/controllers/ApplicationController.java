package lsunol.schibsted.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import lsunol.schibsted.application.ApplicationConstants;
import lsunol.schibsted.controllers.annotations.HttpProduces;
import lsunol.schibsted.database.RepositoryManager;
import lsunol.schibsted.database.SessionRepository;
import lsunol.schibsted.database.UserRepository;
import lsunol.schibsted.model.Session;
import lsunol.schibsted.model.User;
import org.json.JSONObject;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static lsunol.schibsted.application.ClassManagement.getMethodParameters;

/**
 * This is the mail superclass for all the web endpoints. Every single web controller should extend this class.
 * Subclasses of this class may define methods for any HTTP methods available preceded by the "do" prefix.
 * This methods may return a String meaning that a template, named as the returned string, will be used to render that
 * request, or void meaning that the controller handles the output stream by itself.
 * This methods can receive the following parameters:
 * <ul>
 * <li>{@link HttpExchange}: contains the request stuff (headers, streams, etc.).</li>
 * <li>{@link User}: logged user if present</li>
 * <li>{@link Map<String, String>}: map used to specify attributes that will be used in template.</li>
 * As an example, if we wanted to create a simple controller that says "hello ${username}" on a GET method, we would
 * define a "doGet" method returning a String and receiving a {@link Map<String, String>} named something like <code>templateAttributes.</code>.
 * In addition, a subclass may also define one of each methods defined below:
 * <ul>
 * <li><em>preRequestFilter</em> (returning <em>void</em>): this method is expected to check some requirements prior to
 * the execution of the main "do" method. This method should throw an exception if some requirement is not accomplished
 * (i.e.: login credentials being wrong). That would prevent the "do" method to be executhed and would show a message
 * with the correct message and response status to the HTTP client.</li>
 * <li><em>getAuthenticator</em> (returning {@link com.sun.net.httpserver.Authenticator}: if defined, the Authenticator
 * will be set as the Authenticator for the context that it is registering.</li>
 * </ul>
 */
public abstract class ApplicationController implements IApplicationController {

    private final static Logger log = Logger.getLogger(ApplicationController.class.getName());
    private static SessionRepository sessionRepository = RepositoryManager.getSessionRepository();
    private static UserRepository userRepository = RepositoryManager.getUserRepository();

    /**
     * Returns the request mapping for the controller. This is the path that follows the domain and port in the URL.
     *
     * @return the request mapping for the controller. This is the path that follows the domain and port in the URL.
     */
    public abstract String getRequestMapping();

    @Override
    public final void handle(HttpExchange httpExchange) throws IOException {
        // Check for sessionid cookie to retrieve user and roles
        Session session = getSessionFromCookies(getCookiesFromRequest(httpExchange));
        User requestUser = session == null ? null : session.getUser();

        // Map to store template attributes in html based responses
        Map<String, String> templateAttributes = initializeTemplateParams(requestUser);
        try {
            try {
                // Search & invoke "preRequestFilter" method, if present
                try {
                    Method preRequestFilter = getMethodFromClass(this.getClass(), "preRequestFilter");
                    Object[] parameters = getMethodParameters(preRequestFilter, httpExchange, templateAttributes, requestUser, session);
                    preRequestFilter.invoke(this, parameters);
                } catch (NoSuchMethodException e) {
                    // Not a real problem, meaning the controller has no filters to be applied before executing.
                }

                // Search & invoke main requested method (doGet, doPost, etc.)
                String classMethodName = "do" + capitalize(httpExchange.getRequestMethod());
                Method requestedMethod = Arrays.stream(this.getClass().getMethods()).filter(innerMethod -> innerMethod.getName().equals(classMethodName)).findFirst().orElseThrow(() -> new NoSuchMethodException("Method do'" + classMethodName + "' does not exist in the implemented controller: '" + this.getClass().getName() + "'."));
                // Collect parameters found in method's definition
                Object[] parameters = getMethodParameters(requestedMethod, httpExchange, templateAttributes, requestUser, session);
                // Ensure the requested method can provide an acceptable response to the client
                String clientAccepts = getAcceptHeader(httpExchange);
                String methodProduces = getProducesAnnotation(requestedMethod);
                if (getAcceptableResponseType(methodProduces, clientAccepts) == null)
                    throw new ResponseToClientException(HttpURLConnection.HTTP_NOT_ACCEPTABLE, "The method for the resource requested cannot produce an acceptable response for your client. Please, consider accepting one of the following: '" + methodProduces + "'.", "error");
                // Analyze method's return type to distinguish template-based outputs from self-responsed ones
                Object methodsReturnValue = requestedMethod.invoke(this, parameters);
                if (methodsReturnValue instanceof String)
                    sendResponse(httpExchange, HttpURLConnection.HTTP_OK, (String) methodsReturnValue, templateAttributes);
                // else -> the controller responds directly via the httpExchange output stream.

            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof ResponseToClientException) {
                    sendResponse(httpExchange, e.getTargetException(), templateAttributes);
                } else {
                    log.log(Level.SEVERE, "Internal server error: " + e.getMessage(), e);
                    sendResponse(httpExchange, e);
                }
            } catch (NoSuchMethodException e) {
                String errorMessage = "An unexpected request method " + httpExchange.getRequestMethod() + " has been received for resource '" + this.getRequestMapping() + "'. 405 (Method not allowed) is returned.";
                log.warning(errorMessage);
                throw new ResponseToClientException(HttpURLConnection.HTTP_BAD_METHOD, errorMessage, "error");
            } catch (IllegalAccessException e) {
                String errorMessage = "Internal error occurred while trying to call " + this.getClass().getName() + "'s method: do" + capitalize(httpExchange.getRequestMethod()) + ".";
                log.warning(errorMessage);
                throw new ResponseToClientException(HttpURLConnection.HTTP_INTERNAL_ERROR, errorMessage, "error");
            } catch (ResponseToClientException e) {
                throw e;
            } catch (Exception e) {
                String errorMessage = "Internal server error: " + e.getMessage();
                log.log(Level.SEVERE, errorMessage, e);
                throw new ResponseToClientException(HttpURLConnection.HTTP_INTERNAL_ERROR, errorMessage, "error");
            }
        } catch (ResponseToClientException e) {
            sendResponse(httpExchange, e, templateAttributes);
        }
    }

    /**
     * Returns the {@link Method} with the name <code>methodName</code> present in the <code>controllerClass</code>.
     *
     * @param controllerClass {@link Class<? extends ApplicationController>} where the {@link Method} with name <code>methodName</code> is searched.
     * @param methodName      method of the name to be retrieved.
     * @return the {@link Method} with the name <code>methodName</code> present in the <code>controllerClass</code>.
     * @throws NoSuchMethodException if the method is not found in the <code>controllerClass</code>.
     */
    static Method getMethodFromClass(Class<? extends ApplicationController> controllerClass, String methodName) throws NoSuchMethodException {
        return Arrays.stream(controllerClass.getMethods()).filter(innerMethod -> innerMethod.getName().equals(methodName)).findFirst().orElseThrow(() -> new NoSuchMethodException("Method do'" + methodName + "' does not exist in the implemented controller: '" + controllerClass.getClass().getName() + "'."));
    }

    /**
     * Retrieves the value of the {@link HttpProduces} annotation from the given <code>method</code> if present.
     *
     * @param method
     * @return the value of the {@link HttpProduces} annotation from the given <code>method</code> if present.
     */
    private String getProducesAnnotation(Method method) {
        if (method == null) return null;
        Annotation annotation = method.getAnnotation(HttpProduces.class);
        if (annotation instanceof HttpProduces) return ((HttpProduces) annotation).name();
        else return null;
    }

    /**
     * Returns a new {@link HashMap<String, String>} containing the common parameters used in most of the HTML templates,
     * such as the user or roles strings.
     *
     * @param requestUser HTTP request user.
     * @return a new {@link HashMap<String, String>} containing the common parameters used in most of the HTML templates,
     * such as the user or roles strings.
     */
    static Map<String, String> initializeTemplateParams(User requestUser) {
        Map<String, String> templateAttributes = new HashMap<>();
        if (requestUser != null) {
            templateAttributes.put("username", requestUser.getUsername());
            templateAttributes.put("roles", requestUser.getRoles().stream().collect(Collectors.joining(", ")));
        }
        return templateAttributes;
    }

    /**
     * Returns the capitalized word out of <code>word</code>.
     *
     * @param word String to return capitalized.
     * @return the capitalized word out of <code>word</code>.
     */
    static String capitalize(final String word) {
        return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
    }

    /**
     * Returns a map containing the POST parameters sent in the <code>httpExchange</code> object.
     *
     * @param httpExchange
     * @return a map containing the POST parameters sent in the <code>httpExchange</code> object.
     * @throws IOException if the {@link InputStream} from {@link HttpExchange#getRequestBody()} could not be read.
     */
    static Map<String, String> getBodyAsParametersMap(HttpExchange httpExchange) throws IOException {
        Map<String, String> parameters = new HashMap<>();
        String[] keyValuePairs = getRequestBody(httpExchange).split("&");
        for (String keyValuePair : keyValuePairs) {
            String[] keyValue = keyValuePair.split("=");
            if (keyValue.length != 2) {
                continue;
            }
            parameters.put(keyValue[0], keyValue[1]);
        }
        return parameters;
    }

    /**
     * Returns the body of a POST request in String.
     *
     * @param httpExchange
     * @return the body of a POST request in String.
     * @throws IOException if any error occurs while reading the body from the <code>httpExchange</code>.
     */
    static String getRequestBody(HttpExchange httpExchange) throws IOException {
        InputStream inputStream = httpExchange.getRequestBody();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        byte[] buffer = new byte[2048];
        int read = 0;
        while ((read = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, read);
        }
        return new String(byteArrayOutputStream.toByteArray());
    }


    /**
     * Returns the html content of the template in the path <em>/resources/templates/<code>templateName</code>.html</em>.
     *
     * @param templateName path and name of the template in <em>/resources/templates</em> whose contents will be returned.
     * @return the html content of the template in the path <em>/resources/templates/<code>templateName</code>.html</em>.
     * @throws IOException
     */
    static String getTemplate(String templateName) throws IOException {
        InputStream in = ApplicationController.class.getResourceAsStream("/templates/" + templateName + ".html");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            sb.append(line);
        }
        return sb.toString();
    }

    /**
     * Replaces the attributes present in <code>htmlTemplate</code> with the values from <code>templateAttributes</code>.
     *
     * @param htmlTemplate       html String containing the raw code of a template.
     * @param templateAttributes map containing template attributes expected to be replaced.
     * @return the final HTML containing the template code with the values from <code>templateAttributes</code>.
     */
    public static String fillTemplateWithAttributes(String htmlTemplate, Map<String, String> templateAttributes) {
        String filledHtml = htmlTemplate;
        for (Map.Entry<String, String> entry : templateAttributes.entrySet()) {
            filledHtml = filledHtml.replaceAll("\\$\\{" + entry.getKey() + "\\}", entry.getValue());
        }
        String feedbackMessage = templateAttributes.get(ApplicationConstants.FEEDBACK_MESSAGE_KEY);
        if (feedbackMessage != null) {
            filledHtml = filledHtml.replaceFirst("<body>", "<body><p>" + feedbackMessage + "</p>");
        }
        if (templateAttributes.containsKey("username")) {
            filledHtml = filledHtml.replaceFirst("</body>", "<a href =\"logout\">- logout - </a></body>");
        }
        return filledHtml;
    }

    /**
     * Returns the request cookies in a {@link Map<String, String>}.
     *
     * @param httpExchange object containing http request stuff.
     * @return the request cookies in a {@link Map<String, String>}.
     */
    static Map<String, String> getCookiesFromRequest(HttpExchange httpExchange) {
        String cookiesHeader = httpExchange.getRequestHeaders().getFirst("Cookie");
        if (cookiesHeader != null) {
            List<String> cookiesList = Arrays.asList(cookiesHeader.split(";"));
            Map<String, String> cookies = new HashMap<String, String>();
            cookiesList.forEach(cookie -> {
                String[] splitted = cookie.split("=");
                if (splitted.length == 2) cookies.put(cookie.split("=")[0].trim(), cookie.split("=")[1].trim());
            });
            return cookies;
        } else return new HashMap<>();
    }

    /**
     * Sets the session cookie <code>sessionKey</code> in the <code>httpExchange</code> object. If <code>sessionKey</code>
     * is null, then the cookie is "unset", meaning that its value is set to empty string and its expiry date as 1 Jan 1970.
     *  @param httpExchange object containing http request stuff.
     * @param session   session key string to be set in http response headers.
     */
    static void setSessionCookie(HttpExchange httpExchange, Session session) {
        String cookieExpiryString = "Expires=" + (session == null ? "Thu, 01 Jan 1970 00:00:00 GMT" : DateTimeFormatter.RFC_1123_DATE_TIME.format(session.getExpiresOnTime()));
        httpExchange.getResponseHeaders().set("Set-Cookie", ApplicationConstants.SESSION_KEY_COOKIE_NAME + "=" + (session == null ? "" : session.getSessionKey()) + ";" + cookieExpiryString + ";");
    }

    /**
     * Unsets the session cookie in <code>httpExchange</code> (sets its value to empty string and sets its expiry date
     * to 1 Jan 1970).
     *
     * @param httpExchange object containing http request stuff.
     */
    static void unsetSessionCookie(HttpExchange httpExchange) {
        setSessionCookie(httpExchange, null);
    }

    /**
     * Sends out the HTTP response with the <code>statusCode</code>, using the template <code>templateName</code>,
     * replacing its parameters with the <code>templateAttributes</code>.
     *
     * @param httpExchange       object containing http request stuff.
     * @param statusCode         HTTP response status
     * @param templateName       name of the template to be rendered.
     * @param templateAttributes map of template attributes and the values they should be replaced with.
     * @throws IOException if an error occurs when reading and writing to <code>httpExchange</code>'s input and output streams.
     */
    static void sendResponse(HttpExchange httpExchange, int statusCode, String templateName, Map<String, String> templateAttributes) throws IOException {
        String responseMessage = fillTemplateWithAttributes(getTemplate(templateName), templateAttributes);
        httpExchange.getResponseHeaders().set("Content-Type", "text/html");
        httpExchange.sendResponseHeaders(statusCode, responseMessage.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(responseMessage.getBytes());
        os.close();
    }

    /**
     * Sends out the HTTP response from a {@link Throwable} in one of the accepted formats.
     *
     * @param httpExchange object containing http request stuff.
     * @param throwable    {@link Throwable} containing information about the error to be reported to the http client.
     * @throws IOException if an error occurs when reading and writing to <code>httpExchange</code>'s input and output streams.
     */
    static void sendResponse(HttpExchange httpExchange, Throwable throwable) throws IOException {
        sendResponse(httpExchange, throwable, new HashMap<>());
    }

    /**
     * Sends out the HTTP response from a {@link Throwable} in one of the accepted formats, using the <code>templateAttributes</code>
     * specified.
     *
     * @param httpExchange       object containing http request stuff.
     * @param throwable          {@link Throwable} containing information about the error to be reported to the http client.
     * @param templateAttributes map of template attributes and the values they should be replaced with.
     * @throws IOException if an error occurs when reading and writing to <code>httpExchange</code>'s input and output streams.
     */
    static void sendResponse(HttpExchange httpExchange, Throwable throwable, Map<String, String> templateAttributes) throws IOException {
        boolean isResponseToClientException = throwable instanceof ResponseToClientException;

        String clientAccepts = httpExchange.getRequestHeaders().getFirst("Accept");
        String errorMessage = isResponseToClientException ? ((ResponseToClientException) throwable).getMessageToUser() : throwable.getMessage();
        String responseString;
        int responseStatus = isResponseToClientException ? ((ResponseToClientException) throwable).getResponseStatus() : HttpURLConnection.HTTP_INTERNAL_ERROR;
        if (clientAccepts.contains("html")) {
            httpExchange.getResponseHeaders().set("Content-Type", "text/html");
            templateAttributes.put(ApplicationConstants.FEEDBACK_MESSAGE_KEY, errorMessage);
            String templateToRender = isResponseToClientException ? ((ResponseToClientException) throwable).getTemplateToRender() : "error";
            responseString = fillTemplateWithAttributes(getTemplate(templateToRender), templateAttributes);
        } else if (clientAccepts.contains("json")) {
            httpExchange.getResponseHeaders().set("Content-Type", "application/json");
            Map<String, String> responseMap = new HashMap<String, String>() {{
                put("error", Integer.toString(responseStatus));
                put("msg", errorMessage);
            }};
            ObjectMapper mapper = new ObjectMapper();
            responseString = mapper.writeValueAsString(responseMap);
        } else {
            httpExchange.getResponseHeaders().set("Content-Type", "plain/text");
            responseString = errorMessage;
        }
        httpExchange.sendResponseHeaders(responseStatus, responseString.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(responseString.getBytes());
        os.close();
    }

    /**
     * Sends the <code>responseObject</code> with the <code>responseStatusCode</code> to the requester in an acceptable
     * format for the HTTP client (for the moment, only application/json and text/plain formats are available.
     *
     * @param httpExchange       object containing http request stuff.
     * @param responseStatusCode HTTP response code that will be sent.
     * @param responseObject     object to be sent as body of the message.
     * @throws IOException               if an error occurs when reading and writing to <code>httpExchange</code>'s input and output streams.
     * @throws ResponseToClientException if no acceptable format can be found.
     */
    static void sendResponse(HttpExchange httpExchange, int responseStatusCode, Object responseObject) throws IOException, ResponseToClientException {
        String acceptableResponseType = getAcceptableResponseType("application/json;text/plain", getAcceptHeader(httpExchange));
        if (acceptableResponseType == null)
            throw new ResponseToClientException(HttpURLConnection.HTTP_NOT_ACCEPTABLE, "Cannot provide an acceptable content for your request.");
        else {
            httpExchange.getResponseHeaders().set("Content-Type", acceptableResponseType);
            if (responseObject == null) {
                httpExchange.sendResponseHeaders(responseStatusCode, 0);
            } else {
                ObjectMapper mapper = new ObjectMapper();
                String responseBody = mapper.writeValueAsString(responseObject);
                httpExchange.sendResponseHeaders(responseStatusCode, responseBody.length());
                OutputStream os = httpExchange.getResponseBody();
                os.write(responseBody.getBytes());
                os.close();
            }
        }
    }

    /**
     * Sends the responseStatusCode out to the HTTP client.
     *
     * @param httpExchange       object containing http request stuff.
     * @param responseStatusCode HTTP response code that will be sent.
     * @throws IOException               if an error occurs when reading and writing to <code>httpExchange</code>'s input and output streams.
     * @throws ResponseToClientException if no acceptable format can be found.
     */
    static void sendResponse(HttpExchange httpExchange, int responseStatusCode) throws IOException, ResponseToClientException {
        sendResponse(httpExchange, responseStatusCode, null);
    }

    /**
     * Returns the body of an http request in a {@link JSONObject}.
     *
     * @param httpExchange object containing http request stuff.
     * @return the body of an http request in a {@link JSONObject}.
     * @throws IOException if any error occurs while reading the body from the <code>httpExchange</code>.
     */
    static JSONObject getRequestBodyAsJson(HttpExchange httpExchange) throws IOException {
        String bodyRequest = getRequestBody(httpExchange);
        return new JSONObject(bodyRequest);
    }

    /**
     * Returns the session from the cookies map.
     * @param cookies map containing the http request cookies.
     * @return the session from the cookies map.
     */
    static Session getSessionFromCookies(Map<String, String> cookies) {
        String sessionKey = cookies.get(ApplicationConstants.SESSION_KEY_COOKIE_NAME);
        return sessionRepository.getSession(sessionKey);
    }

    /**
     * Returns the "Accept" header value contained in the <code>httpExchange</code>.
     *
     * @param httpExchange object containing http request stuff.
     * @return the "Accept" header value contained in the <code>httpExchange</code>.
     */
    private static String getAcceptHeader(HttpExchange httpExchange) {
        return httpExchange.getRequestHeaders().getFirst("Accept");
    }

    /**
     * Returns the acceptable format in <code>accepts</code> which is available to be produced.
     * If <code>accepts</code> is null or empty string, then the first <code>produces</code> mime type
     * is returned.
     *
     * @param produces mime type of the produced content in the format "mimetype/submimetype".
     * @param accepts list of comma separated values containing all the accepted mime types.
     * @return the acceptable format in <code>accepts</code> which is available to be produced.
     */
    static String getAcceptableResponseType(String produces, String accepts) {
        if (produces == null) throw new IllegalArgumentException("The 'produces' parameter cannot be null.");
        else if (accepts == null || "".equals(accepts)) return produces.split(";")[0].trim();
        else {
            // produces and accepts both are not null
            String[] producesArray = produces.split(";");
            for (String availableFormat : producesArray) {
                if (accepts.contains("*/*") || accepts.contains(availableFormat.split("/")[0] + "/*") || accepts.contains(availableFormat)) {
                    return availableFormat;
                }
            }
            return null;
        }
    }
}
