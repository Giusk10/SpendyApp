package com.spendyteam.expense.Service;

import org.springframework.web.reactive.function.client.WebClient;

/**
 * Legacy service name retained for backwards compatibility.
 * All operations and business coordination are implemented in {@link ExpenseService}.
 *
 * @deprecated Use {@link ExpenseService} instead.
 */
@Deprecated
public class ExpenseImportService extends ExpenseService {

    public ExpenseImportService() {
        super();
    }

    public ExpenseImportService(WebClient webClient) {
        super(webClient);
    }
}
