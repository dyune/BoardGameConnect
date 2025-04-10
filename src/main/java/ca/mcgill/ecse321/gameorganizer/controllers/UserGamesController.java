package ca.mcgill.ecse321.gameorganizer.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.GameOwner;
import ca.mcgill.ecse321.gameorganizer.models.Game;
import ca.mcgill.ecse321.gameorganizer.models.LendingRecord;
import ca.mcgill.ecse321.gameorganizer.repositories.AccountRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.GameRepository;
import ca.mcgill.ecse321.gameorganizer.repositories.LendingRecordRepository;
import ca.mcgill.ecse321.gameorganizer.services.AccountService;
import ca.mcgill.ecse321.gameorganizer.services.LendingRecordService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for user game-related endpoints.
 * Provides API endpoints for retrieving games a user has played, borrowed, or owns.
 */
@RestController
@RequestMapping("/api/users")
public class UserGamesController {

    @Autowired
    private AccountService accountService;
    
    @Autowired
    private LendingRecordService lendingRecordService;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private GameRepository gameRepository;
    
    @Autowired
    private LendingRecordRepository lendingRecordRepository;

    /**
     * Get games played by a user, derived from lending records where they were the borrower.
     * Includes all games a user has borrowed or played, regardless of status.
     *
     * @param userId The ID of the user
     * @return List of game names played by the user
     */
    @GetMapping("/{userId}/games/played")
    public ResponseEntity<List<String>> getGamesPlayedByUser(@PathVariable int userId) {
        try {
            Account account = accountService.getAccountById(userId);
            List<LendingRecord> records = lendingRecordService.getLendingRecordsByBorrower(account);
            
            // Include all games the user has borrowed (which implies they've played them)
            List<String> gamesPlayed = records.stream()
                .map(record -> record.getRequest().getRequestedGame().getName())
                .distinct()
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(gamesPlayed);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get games currently or previously borrowed by a user.
     *
     * @param userId The ID of the user
     * @return List of game names borrowed by the user
     */
    @GetMapping("/{userId}/games/borrowed")
    public ResponseEntity<List<String>> getGamesBorrowedByUser(@PathVariable int userId) {
        try {
            Account account = accountService.getAccountById(userId);
            List<LendingRecord> records = lendingRecordService.getLendingRecordsByBorrower(account);
            
            // Get any game that was borrowed (active or returned)
            List<String> gamesBorrowed = records.stream()
                .map(record -> record.getRequest().getRequestedGame().getName())
                .distinct()
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(gamesBorrowed);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get games owned by a user (only applicable to GameOwner accounts).
     *
     * @param userId The ID of the user
     * @return List of game names owned by the user
     */
    @GetMapping("/{userId}/games/owned")
    public ResponseEntity<List<String>> getGamesOwnedByUser(@PathVariable int userId) {
        try {
            Account account = accountService.getAccountById(userId);
            
            // Only GameOwner accounts can have owned games
            if (!(account instanceof GameOwner)) {
                return ResponseEntity.ok(new ArrayList<>());
            }
            
            GameOwner gameOwner = (GameOwner) account;
            List<Game> ownedGames = gameRepository.findByOwner(gameOwner);
            
            List<String> gameNames = ownedGames.stream()
                .map(Game::getName)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(gameNames);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
} 