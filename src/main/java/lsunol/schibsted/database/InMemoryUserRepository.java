package lsunol.schibsted.database;

import lsunol.schibsted.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is an implementation of the {@link UserRepository} interface.
 * This implementation stores users in memory, so as soon as the application is shut down all users are lost.
 */
public class InMemoryUserRepository implements UserRepository {

    private Map<String, User> users = new ConcurrentHashMap<>();

    @Override
    public User getUserByUsername(String username) {
        return users.get(username);
    }

    @Override
    public boolean checkUserCredentials(String username, String password) {
        return (users.containsKey(username) && users.get(username).isPasswordCorrect(password));
    }

    @Override
    public void addRoleToUser(String username, List<String> newRoles) {
        User user = users.get(username);
        if (user != null) {
            user.getRoles().addAll(newRoles);
        }
    }

    @Override
    public void deleteUser(String username) {
        users.remove(username);
    }

    @Override
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    @Override
    public User addNewUser(String username, String password, List<String> roles) throws DuplicateKeyException {
        if (users.containsKey(username))
            throw new DuplicateKeyException("There is already another user with the same name: '" + username + "'.");
        User user = new User(username, password, roles);
        users.put(username, user);
        return user;
    }
}
