package ca.mcgill.ecse321.gameorganizer.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ca.mcgill.ecse321.gameorganizer.models.Game;
import ca.mcgill.ecse321.gameorganizer.models.GameInstance;
import ca.mcgill.ecse321.gameorganizer.models.GameOwner;

/**
 * Repository for managing GameInstance entities.
 * Provides methods to find instances by their game and owner.
 */
@Repository
public interface GameInstanceRepository extends JpaRepository<GameInstance, Integer> {

    /**
     * Find instances by their ID.
     * 
     * @param id The ID of the instance
     * @return The instance with the given ID, or null if not found
     */
    GameInstance findGameInstanceById(int id);
    
    /**
     * Find all instances of a specific game.
     * 
     * @param game The game to find instances for
     * @return List of instances for the given game
     */
    List<GameInstance> findByGame(Game game);
    
    /**
     * Find all instances owned by a specific user.
     * 
     * @param owner The owner to find instances for
     * @return List of instances owned by the given user
     */
    List<GameInstance> findByOwner(GameOwner owner);
    
    /**
     * Find all instances owned by a specific user for a specific game.
     * 
     * @param game The game to find instances for
     * @param owner The owner to find instances for
     * @return List of instances for the given game owned by the given user
     */
    List<GameInstance> findByGameAndOwner(Game game, GameOwner owner);
    
    /**
     * Find all available instances of a specific game.
     * 
     * @param game The game to find instances for
     * @param available Whether the instances should be available
     * @return List of available instances for the given game
     */
    List<GameInstance> findByGameAndAvailable(Game game, boolean available);
    
    /**
     * Find all instances owned by a specific user by their ID.
     * 
     * @param ownerId The ID of the owner to find instances for
     * @return List of instances owned by the given user ID
     */
    List<GameInstance> findByOwnerId(int ownerId);
} 