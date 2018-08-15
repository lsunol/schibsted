package lsunol.schibsted.model;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class UserTest {

    User testUser = new User("testuser", "samplepassword", Arrays.asList("PAGE_1", "PAGE_2"));

    @Test
    public void userPasswordIsHashed() throws NoSuchFieldException, IllegalAccessException {
        Field field = testUser.getClass().getDeclaredField("password");
        field.setAccessible(true);
        assertNotEquals("samplepassword", field.get(testUser));
    }

    @Test
    public void passwordVerificationWorks() throws NoSuchFieldException, IllegalAccessException {
        assertTrue(testUser.isPasswordCorrect("samplepassword"));
    }
}