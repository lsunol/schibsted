package lsunol.schibsted.application;

import com.sun.net.httpserver.HttpExchange;
import lsunol.schibsted.controllers.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ClassManagementTest {

    @Mock
    private HttpExchange httpExchange;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void isSubclassOf() {
        assertTrue(ClassManagement.isSubclassOf(Page1Controller.class, AuthenticatedController.class));
        assertTrue(ClassManagement.isSubclassOf(Page1Controller.class, ApplicationController.class));
        assertFalse((ClassManagement.isSubclassOf(Page1Controller.class, Page2Controller.class)));
    }

    @Test
    public void isImplementation() {
        assertTrue(ClassManagement.isImplementation(ApplicationController.class, IApplicationController.class));
        assertTrue(ClassManagement.isImplementation(LoginController.class, IApplicationController.class));
    }

    @Test
    public void isNotImplementation() {
        assertFalse(ClassManagement.isImplementation(ApplicationConstants.class, IApplicationController.class));
        assertFalse(ClassManagement.isImplementation(LoginController.class, ApplicationController.class));
    }

    @Test
    public void isSubclassOrImplements() {
        assertTrue(ClassManagement.isSubclassOrImplements(ApplicationController.class, IApplicationController.class));
        assertTrue(ClassManagement.isSubclassOrImplements(AuthenticatedController.class, ApplicationController.class));
        assertTrue(ClassManagement.isSubclassOrImplements(Page1Controller.class, AuthenticatedController.class));
        assertTrue(ClassManagement.isSubclassOrImplements(Page1Controller.class, IApplicationController.class));
    }

    @Test
    public void isNotSubclassNorImplements() {
        assertFalse(ClassManagement.isSubclassOrImplements(ApplicationConstants.class, IApplicationController.class));
        assertFalse(ClassManagement.isSubclassOrImplements(LoginController.class, AuthenticatedController.class));
    }

    @Test
    public void getMethodParameters() throws NoSuchMethodException {
        Method testMethod = Arrays.stream(ClassManagementTest.class.getMethods()).filter(method -> method.getName().equals("methodDeclaredOnlyForTestingPurposes"))
                .findFirst().orElseThrow(() -> new NoSuchMethodException(this.getClass().getName() + " class should contain a method named 'methodDeclaredOnlyForTestingPurpose' with 3 parameters: int, String and Map<String, String>."));
        Object[] parameters = ClassManagement.getMethodParameters(testMethod, "stringParam", 3, new HashMap<String, String>() {{ put("hello", "world");}} );
        assertEquals(3, parameters.length);
        assertEquals(3, parameters[0]);
        assertEquals("stringParam", parameters[1]);
        assertEquals(1, ((Map) parameters[2]).size());
        assertEquals("world", ((Map) parameters[2]).get("hello"));
    }

    public static final void methodDeclaredOnlyForTestingPurposes(int param1, String param2, Map<String, String> param3) {
    }

}