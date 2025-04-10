package ca.mcgill.ecse321.gameorganizer.repository;

import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.Game;
import ca.mcgill.ecse321.gameorganizer.models.GameOwner;
import ca.mcgill.ecse321.gameorganizer.models.Review;
import ca.mcgill.ecse321.gameorganizer.repositories.ReviewRepository;

@DataJpaTest
public class ReviewRepositoryTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ReviewRepository reviewRepository;

    @AfterEach
    public void clearDatabase() {
        reviewRepository.deleteAll();
        entityManager.flush();
    }

    @Test
    public void testReadReviewById() {
        Review review = new Review(5, "Test review", new Date());
        review = entityManager.persistAndFlush(review);
        entityManager.clear();

        assertTrue(reviewRepository.findReviewById(review.getId()).isPresent());
        assertEquals("Test review", reviewRepository.findReviewById(review.getId()).get().getComment());
    }

    @Test
    public void testReadReviewAttributes() {
        Date currentDate = new Date();
        Review review = new Review(4, "Read attributes test", currentDate);
        review = entityManager.persistAndFlush(review);
        entityManager.clear();

        Review found = reviewRepository.findReviewById(review.getId()).get();
        assertEquals(4, found.getRating());
        assertEquals("Read attributes test", found.getComment());
        assertEquals(currentDate, found.getDateSubmitted());
    }

    @Test
    public void testReadReviewGameReference() {
        // Create game with owner
        GameOwner owner = new GameOwner("owner", "owner@test.com", "password");
        owner = entityManager.persist(owner);

        Game game = new Game("Test Game", 2, 4, "test.jpg", new Date());
        game.setOwner(owner);
        game = entityManager.persist(game);

        // Create and link review
        Review review = new Review(5, "Game reference test", new Date());
        review.setGameReviewed(game);
        review = entityManager.persistAndFlush(review);
        entityManager.clear();

        Review found = reviewRepository.findReviewById(review.getId()).get();
        assertNotNull(found.getGameReviewed());
        assertEquals("Test Game", found.getGameReviewed().getName());
    }

    @Test
    public void testReadReviewReviewerReference() {
        Account reviewer = new Account();
        reviewer.setName("reviewer");
        reviewer.setEmail("reviewer@test.com");
        reviewer.setPassword("password");
        reviewer = entityManager.persist(reviewer);

        Review review = new Review(5, "Reviewer reference test", new Date());
        review.setReviewer(reviewer);
        review = entityManager.persistAndFlush(review);
        entityManager.clear();

        Review found = reviewRepository.findReviewById(review.getId()).get();
        assertNotNull(found.getReviewer());
        assertEquals("reviewer", found.getReviewer().getName());
    }

    @Test
    public void testWriteReviewAttributes() {
        Review review = new Review(3, "Initial comment", new Date());
        review = entityManager.persistAndFlush(review);

        // Modify attributes
        review.setRating(5);
        review.setComment("Updated comment");
        Date newDate = new Date();
        review.setDateSubmitted(newDate);
        entityManager.persistAndFlush(review);
        entityManager.clear();

        Review found = reviewRepository.findReviewById(review.getId()).get();
        assertEquals(5, found.getRating());
        assertEquals("Updated comment", found.getComment());
        assertEquals(newDate, found.getDateSubmitted());
    }

    @Test
    public void testWriteReviewGameReference() {
        // Create initial game
        GameOwner owner1 = new GameOwner("owner1", "owner1@test.com", "password");
        owner1 = entityManager.persist(owner1);
        Game game1 = new Game("Game 1", 2, 4, "game1.jpg", new Date());
        game1.setOwner(owner1);
        game1 = entityManager.persist(game1);

        // Create second game
        GameOwner owner2 = new GameOwner("owner2", "owner2@test.com", "password");
        owner2 = entityManager.persist(owner2);
        Game game2 = new Game("Game 2", 3, 6, "game2.jpg", new Date());
        game2.setOwner(owner2);
        game2 = entityManager.persist(game2);

        // Create review with first game
        Review review = new Review(4, "Game reference write test", new Date());
        review.setGameReviewed(game1);
        review = entityManager.persistAndFlush(review);

        // Update to second game
        review.setGameReviewed(game2);
        entityManager.persistAndFlush(review);
        entityManager.clear();

        Review found = reviewRepository.findReviewById(review.getId()).get();
        assertEquals("Game 2", found.getGameReviewed().getName());
    }

    @Test
    public void testWriteReviewReviewerReference() {
        // Create initial reviewer
        Account reviewer1 = new Account();
        reviewer1.setName("reviewer1");
        reviewer1.setEmail("reviewer1@test.com");
        reviewer1.setPassword("password");
        reviewer1 = entityManager.persist(reviewer1);

        // Create second reviewer
        Account reviewer2 = new Account();
        reviewer2.setName("reviewer2");
        reviewer2.setEmail("reviewer2@test.com");
        reviewer2.setPassword("password");
        reviewer2 = entityManager.persist(reviewer2);

        // Create review with first reviewer
        Review review = new Review(4, "Reviewer reference write test", new Date());
        review.setReviewer(reviewer1);
        review = entityManager.persistAndFlush(review);

        // Update to second reviewer
        review.setReviewer(reviewer2);
        entityManager.persistAndFlush(review);
        entityManager.clear();

        Review found = reviewRepository.findReviewById(review.getId()).get();
        assertEquals("reviewer2", found.getReviewer().getName());
    }

    @Test
    public void testPersistAndLoadCompleteReview() {
        // Create all required objects
        GameOwner owner = new GameOwner("owner", "owner@test.com", "password");
        owner = entityManager.persist(owner);

        Game game = new Game("Complete Game", 2, 6, "complete.jpg", new Date());
        game.setOwner(owner);
        game = entityManager.persist(game);

        Account reviewer = new Account();
        reviewer.setName("reviewer");
        reviewer.setEmail("reviewer@test.com");
        reviewer.setPassword("password");
        reviewer = entityManager.persist(reviewer);

        // Create complete review
        Date currentDate = new Date();
        Review review = new Review(5, "Complete persistence test", currentDate);
        review.setGameReviewed(game);
        review.setReviewer(reviewer);
        review = entityManager.persistAndFlush(review);
        entityManager.clear();

        // Verify complete object persistence
        Review found = reviewRepository.findReviewById(review.getId()).get();
        assertEquals(5, found.getRating());
        assertEquals("Complete persistence test", found.getComment());
        assertEquals(currentDate, found.getDateSubmitted());
        assertEquals("Complete Game", found.getGameReviewed().getName());
        assertEquals("reviewer", found.getReviewer().getName());
    }

    @Test
    public void testCascadingDelete() {
        // Create review with game and reviewer
        GameOwner owner = new GameOwner("owner", "owner@test.com", "password");
        owner = entityManager.persist(owner);

        Game game = new Game("Delete Test Game", 2, 4, "delete.jpg", new Date());
        game.setOwner(owner);
        game = entityManager.persist(game);

        Account reviewer = new Account();
        reviewer.setName("reviewer");
        reviewer.setEmail("reviewer@test.com");
        reviewer.setPassword("password");
        reviewer = entityManager.persist(reviewer);

        Review review = new Review(4, "Cascade delete test", new Date());
        review.setGameReviewed(game);
        review.setReviewer(reviewer);
        review = entityManager.persistAndFlush(review);

        // Delete game
        entityManager.remove(game);
        entityManager.flush();
        entityManager.clear();

        // Verify review still exists but game reference is null
        Review found = reviewRepository.findReviewById(review.getId()).get();
        assertNotNull(found);
        assertNull(found.getGameReviewed());
        assertNotNull(found.getReviewer());
    }
}
