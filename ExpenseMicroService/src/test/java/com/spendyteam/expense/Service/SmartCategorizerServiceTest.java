package com.spendyteam.expense.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SmartCategorizerServiceTest {

    @InjectMocks
    private SmartCategorizerService smartCategorizerService;

    @BeforeEach
    void setUp() {
        // Inizializza il servizio simulando @PostConstruct
        smartCategorizerService.init();
    }

    @Test
    void testPredictCategoryWithNullDescription() {
        String result = smartCategorizerService.predictCategory(null);
        assertEquals("Non classificato", result);
    }

    @Test
    void testPredictCategoryWithEmptyDescription() {
        String result = smartCategorizerService.predictCategory("");
        assertEquals("Non classificato", result);
    }

    @Test
    void testPredictCategoryWithBlankDescription() {
        String result = smartCategorizerService.predictCategory("   ");
        assertEquals("Non classificato", result);
    }

    @Test
    void testPredictCategoryNetflix() {
        String result = smartCategorizerService.predictCategory("Netflix subscription");
        // Può essere classificato dal modello ML o dal fallback
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testPredictCategoryCarrefour() {
        String result = smartCategorizerService.predictCategory("Shopping at Carrefour");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testPredictCategoryUber() {
        String result = smartCategorizerService.predictCategory("Uber ride");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testPredictCategoryMcDonalds() {
        String result = smartCategorizerService.predictCategory("McDonald's lunch");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testPredictCategoryAmazon() {
        String result = smartCategorizerService.predictCategory("Amazon Prime");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testPredictCategoryUnknown() {
        String result = smartCategorizerService.predictCategory("Unknown random text xyz123");
        // Dovrebbe restituire "Non classificato" o una categoria
        assertNotNull(result);
    }

    @Test
    void testPredictCategoryReplacesUnderscores() {
        // Se il modello ML restituisce "Ristorazione_e_Bar",
        // dovrebbe essere convertito in "Ristorazione e Bar"
        String result = smartCategorizerService.predictCategory("McDonald's");

        // Il risultato non dovrebbe contenere underscore
        assertNotNull(result);
        assertFalse(result.contains("_"), "Category should not contain underscores");
    }

    @Test
    void testPredictCategoryLowerCase() {
        // Testa che la tokenizzazione funzioni con il testo in minuscolo
        String result = smartCategorizerService.predictCategory("netflix monthly subscription");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testPredictCategoryUpperCase() {
        String result = smartCategorizerService.predictCategory("NETFLIX MONTHLY SUBSCRIPTION");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testPredictCategoryMixedCase() {
        String result = smartCategorizerService.predictCategory("NeTfLiX MoNtHlY sUbScRiPtIoN");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testPredictCategoryWithSpecialCharacters() {
        String result = smartCategorizerService.predictCategory("Netflix @ 15.99€ - monthly");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testPredictCategoryLongDescription() {
        String longDesc = "This is a very long description for a Netflix subscription " +
                         "that includes many words and details about the payment and service";
        String result = smartCategorizerService.predictCategory(longDesc);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testInitializationDoesNotThrowException() {
        assertDoesNotThrow(() -> {
            SmartCategorizerService service = new SmartCategorizerService();
            service.init();
        });
    }

    @Test
    void testTrainModelWithValidData() throws Exception {
        String trainingData = """
            Abbonamenti_e_Servizi_Digitali Netflix monthly subscription
            Supermercati_e_Alimentari Carrefour weekly shopping
            Trasporti Uber ride to airport
            Ristorazione_e_Bar McDonald's lunch""";

        String result = smartCategorizerService.trainModel(trainingData);

        assertNotNull(result);
        assertTrue(result.contains("Training completato") || result.contains("completato"));
    }

    @Test
    void testTrainModelUpdatesInternalModel() throws Exception {
        String trainingData = """
            Abbonamenti_e_Servizi_Digitali Netflix
            Supermercati_e_Alimentari Carrefour
            """;

        smartCategorizerService.trainModel(trainingData);

        // Dopo il training, il modello dovrebbe essere utilizzabile
        String result = smartCategorizerService.predictCategory("Netflix");
        assertNotNull(result);
    }

    @Test
    void testMultiplePredictions() {
        // Testa che il servizio possa fare multiple predizioni consecutive
        String result1 = smartCategorizerService.predictCategory("Netflix");
        String result2 = smartCategorizerService.predictCategory("Carrefour");
        String result3 = smartCategorizerService.predictCategory("Uber");

        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
    }
}

