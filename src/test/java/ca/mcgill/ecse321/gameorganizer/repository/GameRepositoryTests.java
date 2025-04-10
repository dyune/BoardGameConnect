package ca.mcgill.ecse321.gameorganizer.repository;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import ca.mcgill.ecse321.gameorganizer.models.Game;
import ca.mcgill.ecse321.gameorganizer.models.GameOwner;
import ca.mcgill.ecse321.gameorganizer.repositories.GameRepository;

@DataJpaTest
public class GameRepositoryTests {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GameRepository gameRepository;

    @AfterEach
    public void clearDatabase() {
        gameRepository.deleteAll();
        entityManager.flush();
    }

    @Test
    public void testPersistAndLoadGame() {
        String name = "Dune Imperium";
        int minPlayers = 1;
        int maxPlayers = 6;
        String image = "dune.png";
        Date dateAdded = new Date();

        Game game = new Game(name, minPlayers, maxPlayers, image, dateAdded);
        game = entityManager.persistAndFlush(game);
        int id = game.getId();

        entityManager.clear();

        Game gameFromDb = gameRepository.findGameById(id);

        assertNotNull(gameFromDb);
        assertEquals(name, gameFromDb.getName());
        assertEquals(minPlayers, gameFromDb.getMinPlayers());
        assertEquals(maxPlayers, gameFromDb.getMaxPlayers());
        assertEquals(image, gameFromDb.getImage());
        assertEquals(dateAdded, gameFromDb.getDateAdded());
    }

    @Test
    public void testFindByName() {
        // Create games with different names
        Game game1 = new Game("Monopoly Classic", 2, 6, "monopoly1.jpg", new Date());
        Game game2 = new Game("Monopoly Junior", 2, 8, "monopoly2.jpg", new Date());
        entityManager.persist(game1);
        entityManager.persist(game2);
        entityManager.flush();
        entityManager.clear();

        // Find games containing "Monopoly"
        List<Game> games = gameRepository.findByNameContaining("Monopoly");

        // Assert
        assertEquals(2, games.size());
        assertTrue(games.stream().allMatch(g -> g.getName().contains("Monopoly")));
    }

    @Test
    public void testFindByNameContaining() {
        Game game1 = new Game("Monopoly Classic", 2, 6, "m1.jpg", new Date());
        Game game2 = new Game("Monopoly Junior", 2, 4, "m2.jpg", new Date());
        Game game3 = new Game("Chess", 2, 2, "c.jpg", new Date());
        entityManager.persist(game1);
        entityManager.persist(game2);
        entityManager.persist(game3);
        entityManager.flush();
        entityManager.clear();

        final String searchTerm = "Mono";
        List<Game> gamesWithMono = gameRepository.findByNameContaining(searchTerm);

        assertEquals(2, gamesWithMono.size());
        assertTrue(gamesWithMono.stream().allMatch(g -> g.getName().contains(searchTerm)));
    }

    @Test
    public void testFindByMinPlayers() {
        Game game1 = new Game("Game1", 2, 4, "g1.jpg", new Date());
        Game game2 = new Game("Game2", 3, 6, "g2.jpg", new Date());
        Game game3 = new Game("Game3", 4, 8, "g3.jpg", new Date());
        entityManager.persist(game1);
        entityManager.persist(game2);
        entityManager.persist(game3);
        entityManager.flush();
        entityManager.clear();

        final int maxMinPlayers = 3;
        List<Game> gamesFor3Players = gameRepository.findByMinPlayersLessThanEqual(maxMinPlayers);

        assertEquals(2, gamesFor3Players.size());
        assertTrue(gamesFor3Players.stream().allMatch(g -> g.getMinPlayers() <= maxMinPlayers));
    }

    @Test
    public void testFindByMaxPlayers() {
        Game game1 = new Game("Game1", 2, 4, "g1.jpg", new Date());
        Game game2 = new Game("Game2", 3, 6, "g2.jpg", new Date());
        Game game3 = new Game("Game3", 4, 8, "g3.jpg", new Date());
        entityManager.persist(game1);
        entityManager.persist(game2);
        entityManager.persist(game3);
        entityManager.flush();
        entityManager.clear();

        final int minMaxPlayers = 6;
        List<Game> gamesFor6Players = gameRepository.findByMaxPlayersGreaterThanEqual(minMaxPlayers);

        assertEquals(2, gamesFor6Players.size());
        assertTrue(gamesFor6Players.stream().allMatch(g -> g.getMaxPlayers() >= minMaxPlayers));
    }

    @Test
    public void testFindByPlayerRange() {
        Game game1 = new Game("Game1", 2, 4, "g1.jpg", new Date());
        Game game2 = new Game("Game2", 3, 6, "g2.jpg", new Date());
        Game game3 = new Game("Game3", 4, 8, "g3.jpg", new Date());
        entityManager.persist(game1);
        entityManager.persist(game2);
        entityManager.persist(game3);
        entityManager.flush();
        entityManager.clear();

        final int targetPlayers = 4;
        List<Game> gamesFor4Players = gameRepository.findByMinPlayersLessThanEqualAndMaxPlayersGreaterThanEqual(
                targetPlayers, targetPlayers);

        assertEquals(3, gamesFor4Players.size());
        assertTrue(gamesFor4Players.stream()
                .allMatch(g -> g.getMinPlayers() <= targetPlayers && g.getMaxPlayers() >= targetPlayers));
    }

    @Test
    public void testFindByDateAddedBefore() {
        final Date cutoffDate = new Date();
        try {
            Thread.sleep(100); // Small delay to ensure different timestamps
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Game oldGame = new Game("OldGame", 2, 4, "old.jpg", new Date(cutoffDate.getTime() - 86400000));
        Game newGame = new Game("NewGame", 2, 4, "new.jpg", new Date());
        entityManager.persist(oldGame);
        entityManager.persist(newGame);
        entityManager.flush();
        entityManager.clear();

        List<Game> oldGames = gameRepository.findByDateAddedBefore(cutoffDate);

        assertEquals(1, oldGames.size());
        assertEquals("OldGame", oldGames.get(0).getName());
    }

    @Test
    public void testFindByDateAddedAfter() {
        final Date cutoffDate = new Date();
        try {
            Thread.sleep(100); // Small delay to ensure different timestamps
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Game oldGame = new Game("OldGame", 2, 4, "old.jpg", new Date(cutoffDate.getTime() - 86400000));
        Game newGame = new Game("NewGame", 2, 4, "new.jpg", new Date());
        entityManager.persist(oldGame);
        entityManager.persist(newGame);
        entityManager.flush();
        entityManager.clear();

        List<Game> newGames = gameRepository.findByDateAddedAfter(cutoffDate);

        assertEquals(1, newGames.size());
        assertEquals("NewGame", newGames.get(0).getName());
    }

    @Test
    public void testFindByDateAddedBetween() {
        final Date startDate = new Date(System.currentTimeMillis() - 86400000); // Yesterday
        final Date endDate = new Date(System.currentTimeMillis() + 86400000);   // Tomorrow
        final Date outsideDate = new Date(System.currentTimeMillis() - 172800000); // 2 days ago

        Game game1 = new Game("Game1", 2, 4, "g1.jpg", new Date());
        Game game2 = new Game("Game2", 2, 4, "g2.jpg", outsideDate);
        entityManager.persist(game1);
        entityManager.persist(game2);
        entityManager.flush();
        entityManager.clear();

        List<Game> gamesInRange = gameRepository.findByDateAddedBetween(startDate, endDate);

        assertEquals(1, gamesInRange.size());
        assertEquals("Game1", gamesInRange.get(0).getName());
    }

    @Test
    public void testFindByOwner() {
        // Create owners with required fields
        final GameOwner owner1 = new GameOwner("Owner1", "owner1@test.com", "password1");
        final GameOwner owner2 = new GameOwner("Owner2", "owner2@test.com", "password2");
        entityManager.persist(owner1);
        entityManager.persist(owner2);

        Game game1 = new Game("Game1", 2, 4, "g1.jpg", new Date());
        Game game2 = new Game("Game2", 3, 6, "g2.jpg", new Date());
        Game game3 = new Game("Game3", 4, 8, "g3.jpg", new Date());

        game1.setOwner(owner1);
        game2.setOwner(owner1);
        game3.setOwner(owner2);

        entityManager.persist(game1);
        entityManager.persist(game2);
        entityManager.persist(game3);
        entityManager.flush();
        entityManager.clear();

        List<Game> owner1Games = gameRepository.findByOwner(owner1);

        assertEquals(2, owner1Games.size());
        List<Integer> gameIds = owner1Games.stream()
                .map(Game::getId)
                .collect(Collectors.toList());
        assertTrue(gameIds.contains(game1.getId()));
        assertTrue(gameIds.contains(game2.getId()));
    }

    @Test
    public void testFindByOwnerAndNameContaining() {
        // Create owner with required fields
        final GameOwner owner = new GameOwner("TestOwner", "test@owner.com", "password");
        entityManager.persist(owner);

        Game game1 = new Game("Monopoly Classic", 2, 6, "m1.jpg", new Date());
        Game game2 = new Game("Monopoly Junior", 2, 4, "m2.jpg", new Date());
        Game game3 = new Game("Chess", 2, 2, "c.jpg", new Date());

        game1.setOwner(owner);
        game2.setOwner(owner);
        game3.setOwner(owner);

        entityManager.persist(game1);
        entityManager.persist(game2);
        entityManager.persist(game3);
        entityManager.flush();
        entityManager.clear();

        final String searchTerm = "Mono";
        List<Game> ownerMonopolyGames = gameRepository.findByOwnerAndNameContaining(owner, searchTerm);

        assertEquals(2, ownerMonopolyGames.size());
        assertTrue(ownerMonopolyGames.stream()
                .allMatch(g -> g.getName().contains(searchTerm) && g.getOwner().getId() == owner.getId()));
    }
}
