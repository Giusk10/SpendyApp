package com.spendyteam.expense.Training;

import opennlp.tools.doccat.*;
import opennlp.tools.util.*;
import opennlp.tools.tokenize.SimpleTokenizer;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ExpenseModelTrainer {

    private static final String MODEL_FILE = "ExpenseMicroService/src/main/resources/expense-model.bin";
    private static final String TRAINING_FILE = "ExpenseMicroService/src/main/java/com/spendyteam/expense/Training/dataset.txt";

    public static void main(String[] args) {
        System.out.println("🚀 Avvio Training Modello Spese da FILE...");

        try {
            File trainingFile = new File(TRAINING_FILE);

            if (!trainingFile.exists()) {
                System.err.println("❌ ERRORE: Il file '" + TRAINING_FILE + "' non esiste!");
                return;
            }

            System.out.println("📂 Lettura dati da: " + trainingFile.getAbsolutePath());

            InputStreamFactory dataIn = new MarkableFileInputStreamFactory(trainingFile);

            // Crea lo stream di righe grezze
            try (ObjectStream<String> lineStream = new PlainTextByLineStream(dataIn, StandardCharsets.UTF_8);

                 // --- FILTRO MAGICO: Ignora le righe vuote ---
                 ObjectStream<String> cleanLineStream = new ObjectStream<>() {
                     public String read() throws IOException {
                         String line;
                         while ((line = lineStream.read()) != null) {
                             if (!line.trim().isEmpty()) { // Se la riga non è vuota, usala
                                 return line;
                             }
                         }
                         return null; // Fine del file
                     }
                     public void reset() throws IOException, UnsupportedOperationException {
                         lineStream.reset();
                     }
                     public void close() throws IOException {
                         lineStream.close();
                     }
                 };

                 // Passa lo stream "pulito" al parser
                 ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(cleanLineStream)) {

                TrainingParameters params = TrainingParameters.defaultParams();
                params.put(TrainingParameters.ITERATIONS_PARAM, 100);
                params.put(TrainingParameters.CUTOFF_PARAM, 0);

                DoccatModel model = DocumentCategorizerME.train("it", sampleStream, params, new DoccatFactory());
                System.out.println("✅ Training completato!");

                try (OutputStream modelOut = new BufferedOutputStream(new FileOutputStream(MODEL_FILE))) {
                    model.serialize(modelOut);
                    System.out.println("💾 Modello salvato in: " + new File(MODEL_FILE).getAbsolutePath());
                }

                // Test
                System.out.println("\n--- 🧪 TEST DEL MODELLO ---");
                testModel(model, "PAGAMENTO POS MC DONALDS");
                testModel(model, "ADDEBITO NETFLIX COM");
                testModel(model, "BENZINAIO IP MATTEI");
                testModel(model, "ZALANDO PAGAMENTO");
                testModel(model, "GAS");
                testModel(model, "Amazon Kindle");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void testModel(DoccatModel model, String descrizione) {
        DocumentCategorizerME myCategorizer = new DocumentCategorizerME(model);
        SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
        String[] tokens = tokenizer.tokenize(descrizione.toLowerCase());
        double[] outcomes = myCategorizer.categorize(tokens);
        String category = myCategorizer.getBestCategory(outcomes);
        int index = myCategorizer.getIndex(category);
        double confidence = outcomes[index];

        System.out.printf("Input: '%-25s' -> Categoria: %-30s (Sicurezza: %.2f%%)\n",
                descrizione, category.replace("_", " "), confidence * 100);
    }
}