package com.spendyteam.expense.Utility;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExpenseClassifierTest {

    @Test
    void testClassifyNetflix() {
        String result = ExpenseClassifier.classify("Netflix monthly subscription");
        assertEquals("Abbonamenti e Servizi Digitali", result);
    }

    @Test
    void testClassifyCarrefour() {
        String result = ExpenseClassifier.classify("Shopping at Carrefour");
        assertEquals("Supermercati e Alimentari", result);
    }

    @Test
    void testClassifyUber() {
        String result = ExpenseClassifier.classify("Uber ride to airport");
        assertEquals("Trasporti", result);
    }

    @Test
    void testClassifyMcDonalds() {
        String result = ExpenseClassifier.classify("McDonald's lunch");
        assertEquals("Ristorazione e Bar", result);
    }

    @Test
    void testClassifyNullDescription() {
        String result = ExpenseClassifier.classify(null);
        assertEquals("Non classificato", result);
    }

    @Test
    void testClassifyEmptyDescription() {
        String result = ExpenseClassifier.classify("");
        assertEquals("Non classificato", result);
    }

    @Test
    void testClassifyUnknownDescription() {
        String result = ExpenseClassifier.classify("Random unknown expense");
        assertEquals("Non classificato", result);
    }
}

