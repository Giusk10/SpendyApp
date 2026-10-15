package com.spendyteam.expense.Repository;

import com.spendyteam.expense.Data.Expense;
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

    List<Expense> findByUsername(String username, Sort sort);

    List<Expense> findByUsername(String username);

    @Modifying
    @Transactional
    long deleteByUsername(String username);

    @Query("SELECT e FROM Expense e WHERE e.username = :username " +
           "AND e.startedDate IS NOT NULL AND e.completedDate IS NOT NULL " +
           "AND e.startedDate >= :start AND e.completedDate <= :end " +
           "AND e.startedDate < :end AND e.completedDate > :start " +
           "AND e.amount IS NOT NULL")
    List<Expense> findExpensesByDateRange(
        @Param("username") String username,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
}
