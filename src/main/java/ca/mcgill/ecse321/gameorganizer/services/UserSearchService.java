package ca.mcgill.ecse321.gameorganizer.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.mcgill.ecse321.gameorganizer.dto.response.UserSummaryDto;
import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.GameOwner;
import ca.mcgill.ecse321.gameorganizer.repositories.AccountRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for searching and retrieving user information.
 * Provides methods to search users by name or email.
 */
@Service
public class UserSearchService {

    private final AccountRepository accountRepository;

    @Autowired
    public UserSearchService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Search for users by email address (exact match).
     *
     * @param email The email to search for
     * @return UserSummaryDto if user is found, null otherwise
     */
    @Transactional(readOnly = true)
    public UserSummaryDto getUserByEmail(String email) {
        Optional<Account> accountOpt = accountRepository.findByEmail(email);
        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();
            return convertToUserSummaryDto(account);
        }
        return null;
    }

    /**
     * Search for users by email containing the provided string (partial match).
     *
     * @param emailPattern The email pattern to search for
     * @return List of UserSummaryDto objects for matching users
     */
    @Transactional(readOnly = true)
    public List<UserSummaryDto> searchUsersByEmail(String emailPattern) {
        List<Account> accounts = accountRepository.findByEmailContainingIgnoreCase(emailPattern);
        return accounts.stream()
                .map(this::convertToUserSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Search for users by name containing the provided string (partial match).
     *
     * @param namePattern The name pattern to search for
     * @return List of UserSummaryDto objects for matching users
     */
    @Transactional(readOnly = true)
    public List<UserSummaryDto> searchUsersByName(String namePattern) {
        List<Account> accounts = accountRepository.findByNameContainingIgnoreCase(namePattern);
        return accounts.stream()
                .map(this::convertToUserSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Search for users by either name or email containing the provided string.
     *
     * @param searchTerm The term to search for in both name and email fields
     * @param gameOwnerOnly Whether to only return game owners
     * @return List of UserSummaryDto objects for matching users
     */
    @Transactional(readOnly = true)
    public List<UserSummaryDto> searchUsers(String searchTerm, boolean gameOwnerOnly) {
        List<Account> nameMatches = accountRepository.findByNameContainingIgnoreCase(searchTerm);
        List<Account> emailMatches = accountRepository.findByEmailContainingIgnoreCase(searchTerm);
        
        // Combine results, ensuring no duplicates
        List<Account> combinedResults = new ArrayList<>(nameMatches);
        for (Account account : emailMatches) {
            if (!combinedResults.contains(account)) {
                combinedResults.add(account);
            }
        }
        
        // Filter by game owner if requested
        if (gameOwnerOnly) {
            combinedResults = combinedResults.stream()
                    .filter(account -> account instanceof GameOwner)
                    .collect(Collectors.toList());
        }
        
        return combinedResults.stream()
                .map(this::convertToUserSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert an Account entity to a UserSummaryDto.
     *
     * @param account The Account entity to convert
     * @return UserSummaryDto containing the account information
     */
    private UserSummaryDto convertToUserSummaryDto(Account account) {
        boolean isGameOwner = account instanceof GameOwner;
        return new UserSummaryDto(
                account.getId(),
                account.getName(),
                account.getEmail(),
                isGameOwner
        );
    }
} 