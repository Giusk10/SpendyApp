package com.spendyteam.expense.Repository;

import com.spendyteam.expense.Data.Expense;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Date;

public interface IExpenseRepository extends MongoRepository<Expense, String> {

    boolean existsByStartedDateAndCompletedDate(LocalDateTime startedDate, LocalDateTime completedDate);

    boolean existsByDescriptionAndAmount(String description, BigDecimal amount);

}
