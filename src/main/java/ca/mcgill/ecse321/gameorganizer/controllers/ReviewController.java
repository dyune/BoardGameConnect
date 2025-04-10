package ca.mcgill.ecse321.gameorganizer.controllers;

import ca.mcgill.ecse321.gameorganizer.dto.request.ReviewSubmissionDto;
import ca.mcgill.ecse321.gameorganizer.dto.response.ReviewResponseDto;
import ca.mcgill.ecse321.gameorganizer.exceptions.ForbiddenException;
import ca.mcgill.ecse321.gameorganizer.exceptions.ResourceNotFoundException;
import ca.mcgill.ecse321.gameorganizer.services.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller that handles API endpoints for review operations.
 * Provides endpoints for creating, retrieving, updating, and deleting reviews,
 * as well as getting reviews for specific games.
 *
 * @author Alexander
 */
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private GameService gameService;

    /**
     * Submits a new review for a game.
     *
     * @param reviewDto Data for the new review
     * @return The created review
     */
    @PostMapping
    public ResponseEntity<?> submitReview(@RequestBody ReviewSubmissionDto reviewDto) {
        try {
            ReviewResponseDto createdReview = gameService.submitReview(reviewDto);
            return new ResponseEntity<>(createdReview, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    /**
     * Retrieves a specific review by ID.
     *
     * @param id ID of the review to retrieve
     * @return The requested review
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponseDto> getReviewById(@PathVariable int id) {
        ReviewResponseDto review = gameService.getReviewById(id);
        return ResponseEntity.ok(review);
    }

    /**
     * Retrieves all reviews for a specific game.
     *
     * @param gameId ID of the game to get reviews for
     * @return List of reviews for the specified game
     */
    @GetMapping("/games/{gameId}/reviews")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsByGameId(@PathVariable int gameId) {
        List<ReviewResponseDto> reviews = gameService.getReviewsByGameId(gameId);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Retrieves all reviews for games with a specific name.
     * This allows finding reviews across different copies of the same game.
     *
     * @param gameName Name of the game to find reviews for
     * @return List of reviews for games with the specified name
     */
    @GetMapping("/game")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsByGameName(@RequestParam String gameName) {
        List<ReviewResponseDto> reviews = gameService.getReviewsByGameName(gameName);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Updates an existing review.
     *
     * @param id ID of the review to update
     * @param reviewDto Updated review data
     * @return The updated review
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateReview(
            @PathVariable int id,
            @RequestBody ReviewSubmissionDto reviewDto) {
        try {
            ReviewResponseDto updatedReview = gameService.updateReview(id, reviewDto);
            return ResponseEntity.ok(updatedReview);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Deletes a review by ID.
     *
     * @param id ID of the review to delete
     * @return Confirmation message
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReview(@PathVariable int id) {
        try {
            return gameService.deleteReview(id);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}
