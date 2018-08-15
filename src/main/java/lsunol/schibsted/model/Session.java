package lsunol.schibsted.model;

import lsunol.schibsted.application.ApplicationConstants;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public class Session {

    private String sessionKey;
    private OffsetDateTime expiresOnTime;
    private User user;

    public Session(User user) {
        this.user = user;
        this.sessionKey = UUID.randomUUID().toString();
        refreshSessionExpiryDate();
    }

    public boolean hasExpired() {
        return getExpiresOnTime().isBefore(OffsetDateTime.now());
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public OffsetDateTime getExpiresOnTime() {
        return expiresOnTime;
    }

    public User getUser() {
        return user;
    }

    /**
     * Refreshes the session's expiry date up to {@link ApplicationConstants#SESSION_EXPIRY_MINUTES} more minutes.
     */
    public void refreshSessionExpiryDate() {
        expiresOnTime = OffsetDateTime.now(ZoneOffset.UTC).plus(Duration.ofMinutes(ApplicationConstants.SESSION_EXPIRY_MINUTES));
    }
}
