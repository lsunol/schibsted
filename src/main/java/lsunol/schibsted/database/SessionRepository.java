package lsunol.schibsted.database;

import lsunol.schibsted.model.Session;
import lsunol.schibsted.model.User;

public interface SessionRepository {

    /**
     * Generates a new session for the given <code>username</code> and returns the session key. If the user
     * already had an old session, it will be simply purged after its expiry date passes on (this is, after
     * {@link lsunol.schibsted.application.ApplicationConstants#SESSION_EXPIRY_MINUTES} minutes have passed.
     * @param user that the new session will be registered to.
     * @return the key of a new session.
     */
    Session generateSessionForUser(User user);

    /**
     * Returns the session identified by <code>sessionKey</code>.
     * @param sessionKey key of the session to be retrieved.
     * @return the session identified by <code>sessionKey</code>.
     */
    Session getSession(String sessionKey);

    /**
     * Returns the name of the {@link User} that is registered to the given <code>sessionkey</code>.
     * @param sessionkey session identifier.
     * @return the name of the {@link User} that is registered to the given <code>sessionkey</code>.
     */
    String getUsernameFromSession(String sessionkey);
}
