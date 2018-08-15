package lsunol.schibsted;

import lsunol.schibsted.database.DuplicateKeyException;
import lsunol.schibsted.database.RepositoryManager;
import lsunol.schibsted.database.UserRepository;
import lsunol.schibsted.model.User;

import java.util.List;

public class TestUtils {

    private static UserRepository userRepository = RepositoryManager.getUserRepository();

    /**
     * Creates (or replaces, if existing) the user specified by <code>username</code>.
     * @throws DuplicateKeyException if the user could not be created because it is already in the system.
     */
    public static User createOrReplaceUser(String username, String password, List<String> roles) throws DuplicateKeyException {
        userRepository.deleteUser(username);
        return userRepository.addNewUser(username, password, roles);
    }
}
