package ca.mcgill.ecse321.gameorganizer.models;

/**
 * Represents the possible states of a borrow request in the system.
 * Used to track the lifecycle of game borrowing requests from creation to resolution.
 * 
 * @author @rayanBaida
 */
public enum BorrowRequestStatus {
    /** Initial state when a borrow request is first created */
    PENDING,
    
    /** State indicating the owner has accepted the borrow request */
    APPROVED,
    
    /** State indicating the owner has rejected the borrow request */
    DECLINED
}
