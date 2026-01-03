package com.spendyteam.expense.Controller;

import com.spendyteam.expense.Service.ExpenseImportService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/rest/expense")
public class ExpenseController {

    @Autowired
    private ExpenseImportService expenseService;


    @PostMapping("/import")
    public ResponseEntity<String> importCsv(@RequestParam("file") MultipartFile file, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String token = authHeader.substring(7);
            Response res = expenseService.importExpensesFromCsv(file,token);
            if(res.getStatus()==200){
                return ResponseEntity.ok(res.getEntity().toString());
            } else {
                return ResponseEntity.status(res.getStatus()).body(res.getEntity().toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Failed to import CSV: " + e.getMessage());
        }
    }

    @GetMapping("/getExpenses")
    @Produces("application/json")
    public ResponseEntity<?> getExpenses(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String token = authHeader.substring(7);
            Response res = expenseService.getExpenses(token);
            if(res.getStatus() == 200){
                return ResponseEntity.ok().body(res.getEntity());
            } else {
                return ResponseEntity.status(res.getStatus()).body(res.getEntity());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                java.util.Collections.singletonMap("error", "Failed to retrieve expenses: " + e.getMessage())
            );
        }
    }

    @PostMapping("/getExpenseByDate")
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseEntity<?> getExpenseByDate(@RequestBody Map<String, String> body, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String token = authHeader.substring(7);
            String startedDate = body.get("startedDate");
            String completedDate = body.get("completedDate");

            Response res = expenseService.getExpenseByDate(startedDate, completedDate,token);
            if(res.getStatus() == 200){
                return ResponseEntity.ok().body(res.getEntity());
            } else {
                return ResponseEntity.status(res.getStatus()).body(res.getEntity());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                java.util.Collections.singletonMap("error", "Failed to retrieve expense: " + e.getMessage())
            );
        }
    }

    @PostMapping("/getExpenseByMonth")
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseEntity<?> getExpenseByMonth_Year(@RequestBody Map<String, String> body, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String token = authHeader.substring(7);
            String month = body.get("month");
            String year = body.get("year");
            Response res = expenseService.getExpenseByMonth_Year(month, year, token);
            if(res.getStatus() == 200){
                return ResponseEntity.ok().body(res.getEntity());
            } else {
                return ResponseEntity.status(res.getStatus()).body(res.getEntity());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                java.util.Collections.singletonMap("error", "Failed to retrieve expenses: " + e.getMessage())
            );
        }
    }

    @PostMapping("/getMonthlyAmountOfYear")
    @Consumes("application/json")
    @Produces("application/json")
    public ResponseEntity<?> getMonthly_Amount_of_Year(@RequestBody Map<String, String> body, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String token = authHeader.substring(7);
            String year = body.get("year");
            Response res = expenseService.getMonthly_Amount_of_Year(year, token);
            if(res.getStatus() == 200){
                return ResponseEntity.ok().body(res.getEntity());
            } else {
                return ResponseEntity.status(res.getStatus()).body(res.getEntity());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                java.util.Collections.singletonMap("error", "Failed to retrieve monthly amounts: " + e.getMessage())
            );
        }
    }

    @DeleteMapping("/deleteExpense")
    public ResponseEntity<String> deleteExpense(@RequestBody Map<String, String> body) {
        try {
            String expenseId = body.get("expenseId");
            Response res = expenseService.deleteExpense(expenseId);
            if(res.getStatus() == 200){
                return ResponseEntity.ok(res.getEntity().toString());
            } else {
                return ResponseEntity.status(res.getStatus()).body(res.getEntity().toString());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete expense: " + e.getMessage());
        }
    }

    @PostMapping("/addExpense")
    public ResponseEntity<String> addExpense(@RequestBody Map<String, String> body, @RequestHeader (value = "Authorization", required = false) String authHeader) {
        try {
            String token = authHeader.substring(7);
            Response res = expenseService.addExpense(body, token);
            if(res.getStatus() == 200){
                return ResponseEntity.ok(res.getEntity().toString());
            } else {
                return ResponseEntity.status(res.getStatus()).body(res.getEntity().toString());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to add expense: " + e.getMessage());
        }
    }

    @PostMapping("/test" )
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Expense Microservice is up and running!");
    }
}


