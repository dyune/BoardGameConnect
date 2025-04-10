package ca.mcgill.ecse321.gameorganizer.repository;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.GameOwner;
import ca.mcgill.ecse321.gameorganizer.repositories.AccountRepository;

@DataJpaTest
public class AccountRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    @AfterEach
    public void clearDatabase() {
        accountRepository.deleteAll();
        entityManager.flush();
    }

    @Test
    public void testPersistAndLoadAccount() {
        // Create a new Account instance
        Account account = new Account("John Doe", "john.doe@example.com", "secret123");

        // Save the account using entity manager to ensure it's in the persistence context
        account = entityManager.persistAndFlush(account);

        // Clear the persistence context to force a database read
        entityManager.clear();

        // Retrieve the account by its id
        Account fetchedById = accountRepository.findById(account.getId()).orElse(null);
        assertNotNull(fetchedById, "The account should be found");
        assertEquals("John Doe", fetchedById.getName());
        assertEquals("john.doe@example.com", fetchedById.getEmail());
        assertEquals("secret123", fetchedById.getPassword());

        // Test findByEmail
        Optional<Account> fetchedByEmail = accountRepository.findByEmail("john.doe@example.com");
        assertTrue(fetchedByEmail.isPresent(), "The account should be found by email");
        assertEquals(account.getId(), fetchedByEmail.get().getId());
    }

    @Test
    public void testPersistAndLoadGameOwner() {
        // Create a new GameOwner instance
        GameOwner gameOwner = new GameOwner("Jane Smith", "jane.smith@example.com", "password123");

        // Save the game owner using entity manager
        gameOwner = entityManager.persistAndFlush(gameOwner);

        // Clear the persistence context
        entityManager.clear();

        // Retrieve and verify
        Account fetchedById = accountRepository.findById(gameOwner.getId()).orElse(null);
        assertNotNull(fetchedById, "The game owner should be found");
        assertTrue(fetchedById instanceof GameOwner, "The fetched account should be a GameOwner");
        assertEquals("Jane Smith", fetchedById.getName());
        assertEquals("jane.smith@example.com", fetchedById.getEmail());
        assertEquals("password123", fetchedById.getPassword());
    }

    @Test
    public void testDeleteAccount() {
        // Create and save an account
        Account account = new Account("Bob", "bob@example.com", "bobpass");
        account = entityManager.persistAndFlush(account);

        // Verify it exists
        assertTrue(accountRepository.findById(account.getId()).isPresent(), "Account should exist before deletion");

        // Delete the account
        accountRepository.delete(account);
        entityManager.flush();

        // Verify it's deleted
        assertFalse(accountRepository.findById(account.getId()).isPresent(), "Account should be deleted");
    }

    @Test
    public void testFindByNonExistentEmail() {
        Optional<Account> account = accountRepository.findByEmail("nonexistent@example.com");
        assertFalse(account.isPresent(), "No account should be found for non-existent email");
    }
}
