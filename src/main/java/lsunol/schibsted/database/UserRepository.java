package lsunol.schibsted.database;

import lsunol.schibsted.model.User;

import java.util.List;

public interface UserRepository {
    /**
     * Returns the {@link User} with the name <code>username</code>, or <em>null</em> if the user does not exist.
     * @param username name of the user to be fetched into the repository.
     * @return the {@link User} with the name <code>username</code>, or <em>null</em> if the user does not exist.
     */
    User getUserByUsername(String username);

    /**
     * True whether the user exists and its password is correct. False otherwise.
     * @param username name of the user to be autenticated.
     * @param password password of the user to be autenticated.
     * @return true if the user exists and its password is correct. False otherwise.
     */
    boolean checkUserCredentials(String username, String password);

    /**
     * Adds a new {@link User} with the given <code>username</code>, <code>password</code> and <code>roles</code>.
     * @param username name of the new user.
     * @param password password of the new user.
     * @param roles list of roles (String) the user has.
     */
    User addNewUser(String username, String password, List<String> roles) throws DuplicateKeyException;

    void addRoleToUser(String username, List<String> newRoles);

    /**
     * Deletes the user with id <code>username</code>.
     * @param username identifier of the user to be deleted.
     */
    void deleteUser(String username);

    /**
     * Returns the list of all the {@link User} in the system.
     * @return the list of all the {@link User} in the system.
     */
    List<User> getAllUsers();
}
