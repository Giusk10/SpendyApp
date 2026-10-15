package com.spendyteam.expense.Repository;

import com.spendyteam.expense.DTO.MonthlyExpenseTotal;
import com.spendyteam.expense.Data.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface IExpenseRepository extends JpaRepository<Expense, String> {

    boolean existsByStartedDateAndCompletedDate(LocalDateTime startedDate, LocalDateTime completedDate);

    boolean existsByDescriptionAndAmount(String description, BigDecimal amount);

    boolean existsByUsernameAndStartedDateAndCompletedDate(String username, LocalDateTime startedDate, LocalDateTime completedDate);

    boolean existsByUsernameAndDescriptionAndAmount(String username, String description, BigDecimal amount);

    List<Expense> findByUsername(String username, Sort sort);

    List<Expense> findByUsername(String username);

    Page<Expense> findByUsername(String username, Pageable pageable);

    @Modifying
    @Transactional
    long deleteByUsername(String username);

    /**
     * Retrieves expenses for a specific user within a date range, ordered deterministically by startedDate.
     * Enforces complete interval containment and non-null constraints in the database.
     */
    @Query("SELECT e FROM Expense e WHERE e.username = :username " +
           "AND e.startedDate IS NOT NULL AND e.completedDate IS NOT NULL " +
           "AND e.startedDate >= :start AND e.completedDate <= :end " +
           "AND e.startedDate < :end AND e.completedDate > :start " +
           "AND e.amount IS NOT NULL " +
           "ORDER BY e.startedDate ASC")
    List<Expense> findExpensesByDateRange(
        @Param("username") String username,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    /**
     * Calculates monthly expense totals directly in MySQL using database-side grouping and aggregation.
     * Uses MySQL's DATE_FORMAT function to group by calendar month key (e.g. '2024-01').
     * Filters for negative/zero amounts (amount <= 0) to exclude positive income/credit transactions
     * in accordance with the reporting business rules.
     */
    @Query(value = "SELECT DATE_FORMAT(e.started_date, '%Y-%m') AS monthKey, " +
                   "SUM(e.amount) AS totalAmount " +
                   "FROM expenses e " +
                   "WHERE e.username = :username " +
                   "AND e.started_date IS NOT NULL AND e.completed_date IS NOT NULL " +
                   "AND e.started_date >= :start AND e.completed_date <= :end " +
                   "AND e.started_date < :end AND e.completed_date > :start " +
                   "AND e.amount IS NOT NULL " +
                   "AND e.amount <= 0 " +
                   "GROUP BY DATE_FORMAT(e.started_date, '%Y-%m') " +
                   "ORDER BY monthKey ASC",
           nativeQuery = true)
    List<MonthlyExpenseTotal> aggregateMonthlyExpenses(
        @Param("username") String username,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
}
