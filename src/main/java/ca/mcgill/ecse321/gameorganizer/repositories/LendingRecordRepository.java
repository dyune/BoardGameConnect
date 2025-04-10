package ca.mcgill.ecse321.gameorganizer.repositories;

import ca.mcgill.ecse321.gameorganizer.models.LendingRecord;
import ca.mcgill.ecse321.gameorganizer.models.LendingRecord.LendingStatus;
import ca.mcgill.ecse321.gameorganizer.models.GameOwner;
import ca.mcgill.ecse321.gameorganizer.models.Account;
import ca.mcgill.ecse321.gameorganizer.models.BorrowRequest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.Date;

/**
 * Repository interface for managing LendingRecord entities.
 * Provides methods for CRUD operations and custom queries related to lending records.
 * 
 * @author @YoussGm3o8
 */
@Repository
public interface LendingRecordRepository extends JpaRepository<LendingRecord, Integer> {
    
    /**
     * Finds a lending record by its unique identifier.
     *
     * @param id the ID of the lending record
     * @return an Optional containing the found record or empty if not found
     */
    Optional<LendingRecord> findLendingRecordById(int id);

    /**
     * Retrieves all lending records with a specific status.
     *
     * @param status the status to search for
     * @return list of lending records matching the status
     */
    List<LendingRecord> findByStatus(LendingStatus status);
    
    /**
     * Retrieves a page of lending records with a specific status.
     *
     * @param status the status to search for
     * @param pageable pagination information
     * @return page of lending records matching the status
     */
    Page<LendingRecord> findByStatus(LendingStatus status, Pageable pageable);

    /**
     * Finds all lending records associated with a specific game owner.
     *
     * @param owner the game owner to search for
     * @return list of lending records for the specified owner
     */
    List<LendingRecord> findByRecordOwner(GameOwner owner);
    
    /**
     * Finds a page of lending records associated with a specific game owner.
     *
     * @param owner the game owner to search for
     * @param pageable pagination information
     * @return page of lending records for the specified owner
     */
    Page<LendingRecord> findByRecordOwner(GameOwner owner, Pageable pageable);

    /**
     * Finds all lending records with start dates within a specified range.
     *
     * @param startDate the beginning of the date range
     * @param endDate the end of the date range
     * @return list of lending records within the date range
     */
    List<LendingRecord> findByStartDateBetween(Date startDate, Date endDate);

    /**
     * Finds all lending records that have passed their end date and have a specific status.
     *
     * @param date the date to compare against end dates
     * @param status the status to filter by
     * @return list of lending records matching the criteria
     */
    List<LendingRecord> findByEndDateBeforeAndStatus(Date date, LendingStatus status);

    /**
     * Finds all lending records associated with a specific borrower.
     * Changed from findByRequest_Borrower to findByRequest_Requester to match BorrowRequest model
     *
     * @param requester the borrower account to search for
     * @return list of lending records for the specified borrower
     */
    List<LendingRecord> findByRequest_Requester(Account requester);

    /**
    * Retrieves a LendingRecord entity associated with a specific BorrowRequest.
    *
    * @param request the BorrowRequest entity to search by
    * @return an Optional containing the LendingRecord if found, or an empty Optional if no record exists for the given request
    */
    Optional<LendingRecord> findByRequest(BorrowRequest request);
    
    /**
     * Finds a page of lending records associated with a specific borrower.
     *
     * @param requester the borrower account to search for
     * @param pageable pagination information
     * @return page of lending records for the specified borrower
     */
    Page<LendingRecord> findByRequest_Requester(Account requester, Pageable pageable);

    /**
     * Advanced filter method to find lending records based on multiple criteria.
     * Uses native query with dynamic conditions.
     * 
     * @param fromDate optional start date range
     * @param toDate optional end date range
     * @param status optional status filter
     * @param borrowerId optional borrower ID filter
     * @param gameId optional game ID filter
     * @return list of lending records matching all provided criteria
     */
    @Query("SELECT lr FROM LendingRecord lr WHERE " +
           "(:fromDate IS NULL OR lr.startDate >= :fromDate) AND " +
           "(:toDate IS NULL OR lr.endDate <= :toDate) AND " +
           "(:status IS NULL OR lr.status = :status) AND " +
           "(:borrowerId IS NULL OR lr.request.requester.id = :borrowerId) AND " +
           "(:gameId IS NULL OR lr.request.requestedGame.id = :gameId)")
    List<LendingRecord> filterLendingRecords(
            @Param("fromDate") Date fromDate,
            @Param("toDate") Date toDate,
            @Param("status") LendingStatus status,
            @Param("borrowerId") Integer borrowerId,
            @Param("gameId") Integer gameId);
    
    /**
     * Paginated version of the advanced filter method.
     * 
     * @param fromDate optional start date range
     * @param toDate optional end date range
     * @param status optional status filter
     * @param borrowerId optional borrower ID filter
     * @param gameId optional game ID filter
     * @param pageable pagination information
     * @return page of lending records matching all provided criteria
     */
    @Query("SELECT lr FROM LendingRecord lr WHERE " +
           "(:fromDate IS NULL OR lr.startDate >= :fromDate) AND " +
           "(:toDate IS NULL OR lr.endDate <= :toDate) AND " +
           "(:status IS NULL OR lr.status = :status) AND " +
           "(:borrowerId IS NULL OR lr.request.requester.id = :borrowerId) AND " +
           "(:gameId IS NULL OR lr.request.requestedGame.id = :gameId)")
    Page<LendingRecord> filterLendingRecords(
            @Param("fromDate") Date fromDate,
            @Param("toDate") Date toDate,
            @Param("status") LendingStatus status,
            @Param("borrowerId") Integer borrowerId,
            @Param("gameId") Integer gameId,
            Pageable pageable);

    List<LendingRecord> findByRequestRequesterEmail(String email);
}
