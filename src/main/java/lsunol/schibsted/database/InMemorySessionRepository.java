package lsunol.schibsted.database;

import lsunol.schibsted.application.ApplicationConstants;
import lsunol.schibsted.model.Session;
import lsunol.schibsted.model.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is an implementation of the {@link SessionRepository} interface.
 * This implementation stores sessions in memory, so as soon as the application is shut down all sessions are lost.
 * This {@link SessionRepository} implements the {@link Runnable} interface because it holds a clean up task that
 * its executed every 6 x {@link ApplicationConstants#SESSION_EXPIRY_MINUTES} minutes.
 */
public class InMemorySessionRepository implements SessionRepository, Runnable {

    private final static Logger log = Logger.getLogger(InMemorySessionRepository.class.getName());

    private Map<String, Session> sessionsBySessionKey = new ConcurrentHashMap<>();

    @Override
    public Session generateSessionForUser(User user) {
        Session newSession = new Session(user);
        sessionsBySessionKey.put(newSession.getSessionKey(), newSession);
        return newSession;
    }

    @Override
    public Session getSession(String sessionKey) {
        if (sessionKey == null) return null;
        return sessionsBySessionKey.get(sessionKey);
    }

    @Override
    public String getUsernameFromSession(String sessionKey) {
        Session queriedSession = sessionsBySessionKey.get(sessionKey);
        if (queriedSession != null && !queriedSession.hasExpired()) return queriedSession.getUser().getUsername();
        else return null;
    }

    /**
     * As sessions may grow infinitely, this batch method cleans up expired sessions.
     */
    @Override
    public void run() {
        try {
            while (true) {
                Thread.sleep(ApplicationConstants.SESSION_EXPIRY_MINUTES * 60 * 1000 * 6);
                sessionsBySessionKey.values().removeIf(session -> session.hasExpired());
                log.info("Expired sessions have been purged from session repository.");
            }
        } catch (InterruptedException e) {
            log.log(Level.SEVERE, "An error occurred in the sessions cleanup task: " + e.getMessage(), e);
        }
    }
}
