package ca.mcgill.ecse321.gameorganizer.repositories;

import ca.mcgill.ecse321.gameorganizer.models.Game;
import ca.mcgill.ecse321.gameorganizer.models.GameOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Game entities.
 * Provides CRUD operations and custom queries for games.
 *
 * @author @PlazmaMamba
 */
@Repository
public interface GameRepository extends JpaRepository<Game, Integer> {
    /**
     * Finds a game by its unique identifier.
     *
     * @param id the ID of the game to find
     * @return the game if found, null otherwise
     */
    Game findGameById(int id);

    List<Game> findByName(String name);

    List<Game> findByNameContaining(String namePart);
    List<Game> findByMinPlayersLessThanEqual(int players);
    List<Game> findByMaxPlayersGreaterThanEqual(int players);
    List<Game> findByMinPlayersLessThanEqualAndMaxPlayersGreaterThanEqual(int minPlayers, int maxPlayers);
    List<Game> findByDateAddedBefore(Date date);
    List<Game> findByDateAddedAfter(Date date);
    List<Game> findByDateAddedBetween(Date startDate, Date endDate);
    List<Game> findByOwner(GameOwner owner);
    List<Game> findByOwnerAndNameContaining(GameOwner owner, String namePart);

    /**
     * Finds all games that are available for borrowing on a given date.
     *
     * @param currentDate the date to check availability
     * @return list of available games
     */
    @Query("SELECT g FROM Game g WHERE g.id NOT IN " +
           "(SELECT br.requestedGame.id FROM BorrowRequest br " +
           "WHERE br.status = 'APPROVED' AND br.startDate <= ?1 AND br.endDate >= ?1)")
    List<Game> findAvailableGames(Date currentDate);

    /**
     * Finds all games that are unavailable (borrowed) on a given date.
     *
     * @param currentDate the date to check availability
     * @return list of unavailable games
     */
    @Query("SELECT g FROM Game g WHERE g.id IN " +
           "(SELECT br.requestedGame.id FROM BorrowRequest br " +
           "WHERE br.status = 'APPROVED' AND br.startDate <= ?1 AND br.endDate >= ?1)")
    List<Game> findUnavailableGames(Date currentDate);

    @Query("SELECT g FROM Game g WHERE " +
           "(SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.gameReviewed = g) >= ?1")
    List<Game> findByAverageRatingGreaterThanEqual(double minRating);

    List<Game> findByCategory(String category);
}
