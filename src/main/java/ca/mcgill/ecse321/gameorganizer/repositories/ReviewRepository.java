package ca.mcgill.ecse321.gameorganizer.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.Game;
import ca.mcgill.ecse321.gameorganizer.models.Review;

/**
 * Repository interface for managing Review entities.
 * Provides CRUD operations and custom queries for reviews.
 *
 * @author @jiwoong0815
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    /**
     * Finds a review by its unique identifier.
     *
     * @param id the ID of the review to find
     * @return Optional containing the review if found, empty otherwise
     */
    Optional<Review> findReviewById(int id);

    /**
     * Finds all reviews for a specific game instance.
     *
     * @param game the game instance to get reviews for
     * @return List of reviews for the specified game
     */
    List<Review> findByGameReviewed(Game game);

    /**
     * Finds all reviews for all games with a specific name.
     * This allows retrieving reviews across different owners' copies of the same game.
     *
     * @param gameName the name of the game to find reviews for
     * @return List of reviews for all games with the specified name
     */
    @Query("SELECT r FROM Review r WHERE r.gameReviewed.name LIKE %:gameName%")
    List<Review> findByGameName(@Param("gameName") String gameName);

    /**
     * Finds all reviews written by a specific account.
     *
     * @param reviewer the account that wrote the reviews
     * @return List of reviews by the specified reviewer
     */
    List<Review> findByReviewer(Account reviewer);

    /**
     * Finds all reviews for games in a specific category.
     *
     * @param category the category to find reviews for
     * @return List of reviews for games in the specified category
     */
    @Query("SELECT r FROM Review r WHERE r.gameReviewed.category = :category")
    List<Review> findByGameCategory(@Param("category") String category);

    /**
     * Finds reviews with a specific rating.
     *
     * @param rating the rating to find (1-5)
     * @return List of reviews with the specified rating
     */
    List<Review> findByRating(int rating);

    /**
     * Finds reviews with a rating at or above the specified minimum.
     *
     * @param minRating the minimum rating to find
     * @return List of reviews with ratings at or above the minimum
     */
    List<Review> findByRatingGreaterThanEqual(int minRating);

    /**
     * Finds review(s) by the name of its reviewer
     *
     * @param username the name of the reviewer
     * @return Optional containing the review if found, empty otherwise
     */
    List<Review> findReviewsByReviewerName(String username);

    List<Review> findReviewsByReviewerEmail(String email);
}