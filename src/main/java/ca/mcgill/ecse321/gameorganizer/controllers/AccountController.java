package ca.mcgill.ecse321.gameorganizer.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.mcgill.ecse321.gameorganizer.dto.request.CreateAccountRequest;
import ca.mcgill.ecse321.gameorganizer.dto.request.UpdateAccountRequest;
import ca.mcgill.ecse321.gameorganizer.services.AccountService;
import jakarta.validation.Valid;

/**
 * REST Controller for managing account-related use cases.
 * <p>
 * This controller exposes thes endpoints below:
 * <ul>
 *   <li><strong>POST /account</strong>: Create a new account.</li>
 *   <li><strong>PUT /account</strong>: Update an existing account.</li>
 *   <li><strong>DELETE /account/{email}</strong>: Delete an Account by its email.</li>
 *   <li><strong>PUT /account/{email}</strong>: Upgrade an Account to a GameOwner.</li>
 *   <li><strong>GET /account/{email}</strong>: Retrieve Account information to display by email.</li>
 * </ul>
 * </p>
 *
 * <p>
 * The controller uses gameorganizer/services/AccountService.java for business logic.
 * </p>
 *
 * @see ca.mcgill.ecse321.gameorganizer.services.AccountService
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;

    /**
     * Constructs a new AccountController with the given AccountService.
     *
     * @param accountService the service used to handle account operations
     */
    @Autowired
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Creates a new account.
     * <p>
     * Endpoint: <code>POST /account</code>
     * </p>
     *
     * @param request the request body containing the account creation info:
     *                username,
     *                password,
     *                email,
     *                boolean indicating whether the account should be a GameOwner
     * @return a {@code ResponseEntity<String>} with a success message or error message if creation fails
     */
    @PostMapping("")
    public ResponseEntity<String> createAnAccount(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.createAccount(request);
    }

    /**
     * Updates an existing account's information.
     * <p>
     * Endpoint: <code>PUT /account</code>
     * </p>
     *
     * @param request the request body containing the account update information:
     *                email to identify the account,
     *                the current password for authentication,
     *                the new username,
     *                and optionally, a new password
     * @return a {@code ResponseEntity<String>} with a confirmation message upon update or an error message
     *         if the given password is wrong or if an account with that email could not be found
     */
    @PutMapping("")
    public ResponseEntity<String> updateAccount(@Valid @RequestBody UpdateAccountRequest request) {
        return accountService.updateAccount(request);
    }

    /**
     * Deletes an account by its email address.
     * <p>
     * Endpoint: <code>DELETE /account/{email}</code>
     * </p>
     *
     * @param email the email of the account to delete
     * @return a {@code ResponseEntity<String>} with a confirmation message if the account is deleted,
     *         or an error message if no account could be found with the given email
     */
    @DeleteMapping("/{email}")
    public ResponseEntity<String> deleteAccount(@PathVariable String email) {
        return accountService.deleteAccountByEmail(email);
    }

    /**
     * Upgrades an account to a GameOwner.
     * <p>
     * This action promotes an existing account to a GameOwner by transferring all related associations
     * to the new GameOwner account.
     * <br>
     * Endpoint: <code>PUT /account/{email}</code>
     * </p>
     *
     * @param email the email of the account to upgrade, provided as a path variable
     * @return a {@code ResponseEntity<String>} with a success message if the upgrade is successful
     *         or an error message if the account does not exist or is already a GameOwner
     */
    @PutMapping("/{email}")
    public ResponseEntity<String> upgradeAccountToGameOwner(@PathVariable String email) {
        return accountService.upgradeUserToGameOwner(email);
    }

    /**
     * Retrieves account information by email.
     * <p>
     * Endpoint: <code>GET /account/{email}</code>
     * </p>
     *
     * @param email the email of the account to retrieve, provided as a path variable
     * @return a {@code ResponseEntity<?>} containing an {@code AccountResponse} DTO with account details
     *         if found or an error message if no account with the provided email exists
     */
    @GetMapping("/{email}")
    public ResponseEntity<?> getAccount(@PathVariable String email) {
        return accountService.getAccountInfoByEmail(email);
    }
}
