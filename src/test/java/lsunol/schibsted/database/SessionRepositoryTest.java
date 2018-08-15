package lsunol.schibsted.database;

import lsunol.schibsted.model.Session;
import lsunol.schibsted.model.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

@RunWith(Parameterized.class)
public class SessionRepositoryTest {

    private static final String USERNAME = "testUser";
    private Session session;

    private SessionRepository sessionRepository;
    private User requestUser = new User(USERNAME, "aPassword", new LinkedList<>());

    public SessionRepositoryTest(SessionRepository sessionRepository) {
        this.sessionRepository  = sessionRepository;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> instancesToTest() {
        Set<Object[]> implementations = new HashSet<>();
        implementations.add(new Object[] {new InMemorySessionRepository()});
        // add all implementations of SessionRepository here
        return implementations;
    }

    @Before
    public void setUp() {
        session = sessionRepository.generateSessionForUser(requestUser);
    }

    @Test
    public void getExistingUsernameFromSession() {
        assertEquals(USERNAME, sessionRepository.getUsernameFromSession(session.getSessionKey()));
    }

    @Test
    public void getNonExistingUsernameFromSession() {
        assertNull(sessionRepository.getUsernameFromSession("non-existing-session-id"));
    }
}