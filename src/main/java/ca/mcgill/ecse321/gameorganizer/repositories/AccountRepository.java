package ca.mcgill.ecse321.gameorganizer.repositories;

import ca.mcgill.ecse321.gameorganizer.models.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Account entities.
 * Provides CRUD operations and custom queries for user accounts.
 * 
 * @author @dyune
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {

    /**
     * Finds an account by its email address.
     * Uses an EntityGraph to eagerly fetch associated roles.
     *
     * @param email The email address to search for
     * @return Optional containing the account if found, empty otherwise
     */
    Optional<Account> findByEmail(String email);
    Optional<Account> findByName(String name);

    /**
     * Finds accounts by name containing the given pattern (case insensitive).
     *
     * @param namePattern The pattern to search for in account names
     * @return List of accounts matching the pattern
     */
    @Query("SELECT a FROM Account a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :namePattern, '%'))")
    List<Account> findByNameContainingIgnoreCase(@Param("namePattern") String namePattern);

    /**
     * Finds accounts by email containing the given pattern (case insensitive).
     *
     * @param emailPattern The pattern to search for in account emails
     * @return List of accounts matching the pattern
     */
    @Query("SELECT a FROM Account a WHERE LOWER(a.email) LIKE LOWER(CONCAT('%', :emailPattern, '%'))")
    List<Account> findByEmailContainingIgnoreCase(@Param("emailPattern") String emailPattern);

    /**
     * Finds an account by its password reset token.
     *
     * @param token The password reset token to search for
     * @return Optional containing the account if found, empty otherwise
     */
    Optional<Account> findByResetPasswordToken(String token);
}
