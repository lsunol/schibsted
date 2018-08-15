package lsunol.schibsted.database;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class UserRepositoryTest {

    private UserRepository userRepository;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    public UserRepositoryTest(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> instancesToTest() {
        Set<Object[]> implementations = new HashSet<>();
        implementations.add(new Object[] {new InMemoryUserRepository()});
        // add all implementations of UserRepository here
        return implementations;
    }

    @Before
    public void setUp() throws DuplicateKeyException {
        createOrReplaceUser("username", "password", new LinkedList<String>() {{ add("PAGE_1"); add("PAGE_2"); }});
    }

    @Test
    public void getExistingUserByUsername() {
        assertNotNull(userRepository.getUserByUsername("username"));
    }

    @Test
    public void getNonExistingUserByUsername() {
        assertNull(userRepository.getUserByUsername("non-existing-user"));
    }

    @Test
    public void checkUserCredentials() {
        assertTrue(userRepository.checkUserCredentials("username", "password"));
    }

    @Test
    public void checkWrongUserCredentials() {
        assertFalse(userRepository.checkUserCredentials("username", "wrong password"));
    }

    @Test
    public void checkNonExistingUserCredentials() {
        assertFalse(userRepository.checkUserCredentials("non-existing-user", "any-password"));
    }

    @Test
    public void addNewUser() throws DuplicateKeyException {
        assertNull(userRepository.getUserByUsername("secondTestUser"));
        userRepository.addNewUser("secondTestUser", "secondPassword", new LinkedList<>());
        assertNotNull(userRepository.getUserByUsername("secondTestUser"));
    }

    @Test
    public void addTwoNewUsersWithTheSameName() throws DuplicateKeyException {
        String newUserName = "alreadyExistingUser";
        assertNull(userRepository.getUserByUsername(newUserName));
        userRepository.addNewUser(newUserName, "nomatterpassword", new LinkedList<>());
        exception.expect(DuplicateKeyException.class);
        userRepository.addNewUser(newUserName, "nomatterpassword", new LinkedList<>());
    }

    @Test
    public void addRoleToUser() {
        assertFalse(userRepository.getUserByUsername("username").getRoles().contains("NEW_ROLE"));
        userRepository.addRoleToUser("username", Arrays.asList("NEW_ROLE"));
        assertTrue(userRepository.getUserByUsername("username").getRoles().contains("NEW_ROLE"));
    }

    @Test
    public void addRoleToNonExistingUser() {

    }

    @Test
    public void deleteExistingUser() throws DuplicateKeyException {
        userRepository.addNewUser("secondTestUser", "secondPassword", new LinkedList<>());
        userRepository.deleteUser("secondTestUser");
        assertNull(userRepository.getUserByUsername("secondTestUser"));
    }

    @Test
    public void deleteNonExistingUser() {
        int usersCount = userRepository.getAllUsers().size();
        userRepository.deleteUser("non-existing-user-to-delete");
        assertEquals(usersCount, userRepository.getAllUsers().size());
    }

    @Test
    public void getAllUsers() {
        assertNotNull(userRepository.getAllUsers());
    }

    /**
     * Creates (or replaces, if existing) the user specified by <code>username</code>.
     * @throws DuplicateKeyException if the user could not be created because it is already in the system.
     */
    private void createOrReplaceUser(String username, String password, List<String> roles) throws DuplicateKeyException {
        userRepository.deleteUser(username);
        userRepository.addNewUser(username, password, roles);
    }
}