package ca.mcgill.ecse321.gameorganizer.repositories;

import java.util.Date;
import java.util.List;
import java.util.Optional; // Import added

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.BorrowRequest;
import ca.mcgill.ecse321.gameorganizer.models.BorrowRequestStatus;
import ca.mcgill.ecse321.gameorganizer.models.Game;
import ca.mcgill.ecse321.gameorganizer.models.GameOwner;

/**
 * Repository interface for managing BorrowRequest entities.
 * Provides CRUD operations and custom queries for game borrowing requests.
 * Extends JpaRepository to inherit basic database operations.
 * 
 * @author @rayanBaida
 */
@Repository
public interface BorrowRequestRepository extends JpaRepository<BorrowRequest, Integer> {
    
    /**
     * Finds a borrow request by its unique identifier.
     *
     * @param id The ID of the borrow request to find
     * @return Optional containing the borrow request if found, empty Optional otherwise
     */
    Optional<BorrowRequest> findBorrowRequestById(int id);

    /**
     * Finds borrow request(s) by the user associated to it
     *
     * @param username the username of the requester
     * @return Optional containing the borrow request if found, empty Optional otherwise
     */
    List<BorrowRequest> findBorrowRequestsByRequesterName(String username);

    /**
     * Finds all approved borrow requests that overlap with a given date range for a specific game.
     * This is used to check if a game is available for a new borrow request.
     *
     * @param gameId The ID of the game to check
     * @param startDate The start date of the period to check
     * @param endDate The end date of the period to check
     * @return List of overlapping approved borrow requests
     */
    @Query("SELECT br FROM BorrowRequest br " +
           "WHERE br.requestedGame.id = :gameId " +
           "AND br.status = 'APPROVED' " +
           "AND br.startDate < :endDate " +
           "AND br.endDate > :startDate")
    List<BorrowRequest> findOverlappingApprovedRequests(@Param("gameId") int gameId, 
                                                        @Param("startDate") Date startDate, 
                                                        @Param("endDate") Date endDate);
                                                        
    /**
     * Finds all borrow requests for games owned by a specific owner and with a specific status.
     * Used by game owners to view pending requests for their games.
     *
     * @param owner The game owner whose games' requests to find
     * @param status The status of the requests to find
     * @return List of borrow requests matching the criteria
     */
    List<BorrowRequest> findByRequestedGame_OwnerAndStatus(GameOwner owner, BorrowRequestStatus status);
    
    /**
     * Finds all borrow requests made by a specific requester.
     * Used to view a user's borrow request history.
     *
     * @param requester The account that made the requests
     * @return List of borrow requests made by the specified requester
     */
    List<BorrowRequest> findByRequester(Account requester);

    /**
     * Finds all borrow requests associated with a specific game owner by their ID.
     *
     * @param ownerId The ID of the game owner
     * @return List of borrow requests associated with the specified game owner
     */
    @Query("SELECT br FROM BorrowRequest br WHERE br.requestedGame.owner.id = :ownerId")
    List<BorrowRequest> findBorrowRequestsByOwnerId(@Param("ownerId") int ownerId);

    /**
     * Finds all borrow requests associated with a specific game.
     * Used for cascade deletion when a game is deleted.
     *
     * @param game The game whose borrow requests to find
     * @return List of borrow requests associated with the specified game
     */
    List<BorrowRequest> findByRequestedGame(Game game);

    /**
     * Find borrow requests with approved status for a specific game
     * that overlap with a given period.
     * Used to check availability for new or updated requests.
     */
    @Query("SELECT br FROM BorrowRequest br " +
           "WHERE br.requestedGame.id = :gameId " +
           "AND br.status = 'APPROVED' " +
           "AND ((br.startDate <= :endDate) AND (br.endDate >= :startDate))")
    List<BorrowRequest> findOverlappingApprovedRequestsForGame(
        @Param("gameId") int gameId,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate);


    List<BorrowRequest> findBorrowRequestsByRequesterEmail(String email);

    /**
     * Finds all borrow requests associated with a specific game instance.
     *
     * @param gameInstanceId The ID of the game instance
     * @return List of borrow requests associated with the specified game instance
     */
    @Query("SELECT br FROM BorrowRequest br WHERE br.gameInstance.id = :gameInstanceId")
    List<BorrowRequest> findBorrowRequestsByGameInstanceId(@Param("gameInstanceId") int gameInstanceId);

    /**
     * Find borrow requests with approved status for a specific game instance
     * that overlap with a given period.
     * Used to check availability for new or updated requests.
     */
    @Query("SELECT br FROM BorrowRequest br " +
           "WHERE br.gameInstance.id = :gameInstanceId " +
           "AND br.status = 'APPROVED' " +
           "AND ((br.startDate <= :endDate) AND (br.endDate >= :startDate))")
    List<BorrowRequest> findOverlappingApprovedRequestsForGameInstance(
        @Param("gameInstanceId") int gameInstanceId,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate);

}
