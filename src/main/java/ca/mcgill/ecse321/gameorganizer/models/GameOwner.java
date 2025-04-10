package ca.mcgill.ecse321.gameorganizer.models;

import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

/**
 * Represents a user who owns games in the system.
 * This class extends the Account class to add game ownership functionality.
 * GameOwner accounts can lend their games to other users through borrow requests.
 * 
 * @author @dyune
 */
@Entity
@NoArgsConstructor
public class GameOwner extends Account {

    /**
     * Creates a new GameOwner account with the specified details.
     *
     * @param aName The display name of the game owner
     * @param aEmail The email address of the game owner (unique identifier)
     * @param aPassword The password for account authentication
     */
    public GameOwner(String aName, String aEmail, String aPassword) {
        super(aName, aEmail, aPassword);
    }

    /**
     * Performs cleanup operations when deleting the GameOwner account.
     * This includes handling owned games and active borrow requests.
     */
    public void delete() {
        super.delete();
    }

}