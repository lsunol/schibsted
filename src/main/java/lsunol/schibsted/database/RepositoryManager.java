package lsunol.schibsted.database;

public class RepositoryManager {

    private static SessionRepository sessionRepository = null;

    private static UserRepository userRepository = null;

    private RepositoryManager() { }

    public static SessionRepository getSessionRepository() {
        if (sessionRepository == null) sessionRepository = new InMemorySessionRepository();
        return sessionRepository;
    }

    public static UserRepository getUserRepository() {
        if (userRepository == null) userRepository = new InMemoryUserRepository();
        return userRepository;
    }
}
