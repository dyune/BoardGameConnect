package ca.mcgill.ecse321.gameorganizer.dto.request;

/**
 * DTO for submitting new game reviews.
 * Contains all required fields for creating a review, using IDs/emails for entity references.
 *
 * @author Alexander
 */
public class ReviewSubmissionDto {
    private int rating;
    private String comment;
    private int gameId;  // ID of the specific game being reviewed
    private String reviewerId;  // Email of the person submitting the review

    /**
     * Default constructor
     */
    public ReviewSubmissionDto() {}

    /**
     * Constructs a ReviewSubmissionDto with all required fields
     *
     * @param rating The numerical rating given to the game (1-5)
     * @param comment The textual feedback about the game
     * @param gameId The ID of the game being reviewed
     * @param reviewerId The email of the account submitting the review
     */
    public ReviewSubmissionDto(int rating, String comment, int gameId, String reviewerId) {
        this.rating = rating;
        this.comment = comment;
        this.gameId = gameId;
        this.reviewerId = reviewerId;
    }

    /**
     * Gets the rating value
     *
     * @return The numerical rating (1-5)
     */
    public int getRating() {
        return rating;
    }

    /**
     * Sets the rating value
     *
     * @param rating The numerical rating (1-5)
     */
    public void setRating(int rating) {
        this.rating = rating;
    }

    /**
     * Gets the comment text
     *
     * @return The review comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the comment text
     *
     * @param comment The review comment
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Gets the ID of the game being reviewed
     *
     * @return The game ID
     */
    public int getGameId() {
        return gameId;
    }

    /**
     * Sets the ID of the game being reviewed
     *
     * @param gameId The game ID
     */
    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    /**
     * Gets the email of the reviewer
     *
     * @return The reviewer's email
     */
    public String getReviewerId() {
        return reviewerId;
    }

    /**
     * Sets the email of the reviewer
     *
     * @param reviewerId The reviewer's email
     */
    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    @Override
    public String toString() {
        return "ReviewSubmissionDto{" +
                "rating=" + rating +
                ", comment='" + comment + '\'' +
                ", gameId=" + gameId +
                ", reviewerId='" + reviewerId + '\'' +
                '}';
    }
}