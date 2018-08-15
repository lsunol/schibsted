package lsunol.schibsted.controllers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import lsunol.schibsted.TestUtils;
import lsunol.schibsted.application.ApplicationConstants;
import lsunol.schibsted.database.DuplicateKeyException;
import lsunol.schibsted.database.RepositoryManager;
import lsunol.schibsted.database.SessionRepository;
import lsunol.schibsted.database.UserRepository;
import lsunol.schibsted.model.Session;
import lsunol.schibsted.model.User;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class ApplicationControllerTest {

    private final static Logger log = Logger.getLogger(ApplicationControllerTest.class.getName());

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    SessionRepository sessionRepository = RepositoryManager.getSessionRepository();

    @Mock
    private HttpExchange httpExchange;

    User requestUser = new User("testUser", "testPassword", Arrays.asList("PAGE_1", "ADMIN"));
    static Headers responseHeaders;

    @BeforeClass
    public static void setUpClass() throws Exception {
        responseHeaders = new Headers();
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(httpExchange.getResponseHeaders()).thenReturn(responseHeaders);
    }

    @Test
    public void getExistingMethodFromClass() throws NoSuchMethodException {
        Page1Controller p1c = new Page1Controller();
        assertNotNull(p1c.getMethodFromClass(p1c.getClass(), "doGet"));
        assertNotNull(p1c.getMethodFromClass(p1c.getClass(), "preRequestFilter"));
    }

    @Test
    public void getNonExistingMethodFromClass() throws NoSuchMethodException {
        LoginController lc = new LoginController();
        exception.expect(NoSuchMethodException.class);
        lc.getMethodFromClass(lc.getClass(), "preRequestFilter");
    }

    @Test
    public void initializeTemplateParams() {
        Map<String, String> templateParams = ApplicationController.initializeTemplateParams(requestUser);
        assertEquals("testUser", templateParams.get("username"));
        assertEquals("PAGE_1, ADMIN", templateParams.get("roles"));
    }

    @Test
    public void capitalize() {
        assertEquals("Capitalized", ApplicationController.capitalize("capitalized"));
    }

    @Test
    public void getTemplate() throws IOException {
        String html = ApplicationController.getTemplate("login-form");
        assertTrue(html.contains("<form id=\"loginForm\" action=\"/login\" method=\"post\">"));
    }

    @Test
    public void fillTemplateWithAttributes() throws IOException {
        String html = ApplicationController.fillTemplateWithAttributes(ApplicationController.getTemplate("page1"), ApplicationController.initializeTemplateParams(requestUser));
        assertTrue(html.contains("<p>Hello, testUser, your roles in the platform are:"));
        assertTrue(html.contains("PAGE_1"));
        assertTrue(html.contains("ADMIN"));
    }

    @Test
    public void getCookiesFromRequest() {
        Headers requestHeaders = new Headers();
        requestHeaders.set("Cookie", "yummy_cookie=choco; tasty_cookie=strayberry");
        when(httpExchange.getRequestHeaders()).thenReturn(requestHeaders);
        Map<String, String> cookies = ApplicationController.getCookiesFromRequest(httpExchange);
        assertEquals("choco", cookies.get("yummy_cookie"));
        assertEquals("strayberry", cookies.get("tasty_cookie"));
    }


    @Test
    public void setSessionCookie() {
        Session session = sessionRepository.generateSessionForUser(requestUser);
        ApplicationController.setSessionCookie(httpExchange, session);
        List<String> cookieList = httpExchange.getResponseHeaders().get("Set-Cookie");
        String cookieValue = cookieList.get(0);
        assertTrue(cookieValue.contains(ApplicationConstants.SESSION_KEY_COOKIE_NAME + "=" + session.getSessionKey() + ";Expires="));
    }

    @Test
    public void unsetSessionCookie() {
        ApplicationController.unsetSessionCookie(httpExchange);
        List<String> cookieList = httpExchange.getResponseHeaders().get("Set-Cookie");
        String cookieValue = cookieList.get(0);
        assertTrue(cookieValue.contains(ApplicationConstants.SESSION_KEY_COOKIE_NAME + "=;Expires="));
    }

    @Test
    public void getSessionFromCookies() throws DuplicateKeyException {
        User user = TestUtils.createOrReplaceUser("testUserSessionName", "userPassword", Arrays.asList("PAGE_1"));
        Session session = sessionRepository.generateSessionForUser(user);
        Session sessionFromCookies = ApplicationController.getSessionFromCookies(new HashMap<String, String>() {{
            put(ApplicationConstants.SESSION_KEY_COOKIE_NAME, session.getSessionKey());
        }});
        assertNotNull(sessionFromCookies);
        assertEquals(session.getSessionKey(), sessionFromCookies.getSessionKey());
    }

    @Test
    public void getUserFromRequest() throws DuplicateKeyException {
        User user = TestUtils.createOrReplaceUser("testUserSessionName", "userPassword", Arrays.asList("PAGE_1"));
        Session session = sessionRepository.generateSessionForUser(user);
        Session sessionFromCookies = ApplicationController.getSessionFromCookies(new HashMap<String, String>() {{
            put(ApplicationConstants.SESSION_KEY_COOKIE_NAME, session.getSessionKey());
        }});
        assertEquals(user.getUsername(), sessionFromCookies.getUser().getUsername());
        assertEquals("PAGE_1", sessionFromCookies.getUser().getRoles().get(0));
    }

    @Test
    public void acceptsAny() {
        assertEquals("application/json", ApplicationController.getAcceptableResponseType("application/json;text/plain", "*/*"));
    }

    @Test
    public void accepts2ndOption() {
        assertEquals("text/plain", ApplicationController.getAcceptableResponseType("application/json;text/plain", "text/plain"));
    }

    @Test
    public void acceptsMimeSubtype() {
        assertEquals("text/html", ApplicationController.getAcceptableResponseType("applitacion/json;text/html", "text/*"));
    }

    @Test
    public void unacceptableFormatFound() {
        assertNull(ApplicationController.getAcceptableResponseType("application/json", "text/html"));
    }
}