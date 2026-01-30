package com.spendyteam.expense.Service;

import com.spendyteam.expense.Utility.ExpenseClassifier;
import opennlp.tools.doccat.*;
import opennlp.tools.util.*;
import opennlp.tools.tokenize.SimpleTokenizer;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Service
public class SmartCategorizerService {

    private DoccatModel model;
    // Nome del file previsto nelle risorse (dentro la cartella resources)
    private static final String MODEL_FILENAME = "/expense-model.bin";
    // Nome del file nel file system locale (per quando lo crei o sei in dev)
    private static final String MODEL_FILE_LOCAL = "ExpenseMicroService/src/main/resorces/expense-model.bin";

    @PostConstruct
    public void init() {
        try {
            // 1. PRIMA SCELTA: Carica dalle Risorse (Per quando è pacchettizzato in JAR/Docker)
            InputStream in = getClass().getResourceAsStream(MODEL_FILENAME);

            if (in != null) {
                this.model = new DoccatModel(in);
                in.close();
                System.out.println("✅ Modello ML caricato dalle Risorse (Production Mode)!");
            } else {
                // 2. SECONDA SCELTA: Carica dal File System (Per quando sei in sviluppo locale)
                File modelFile = new File(MODEL_FILE_LOCAL);
                if (modelFile.exists()) {
                    try (InputStream fileIn = new FileInputStream(modelFile)) {
                        this.model = new DoccatModel(fileIn);
                        System.out.println("⚠️ Modello ML caricato da File System locale (Dev Mode).");
                    }
                } else {
                    System.out.println("❌ Nessun modello trovato. Il sistema userà la classificazione manuale.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String predictCategory(String description) {
        // 1. Se il modello ML non è pronto o la descrizione è nulla, usa il fallback
        if (model == null || description == null || description.trim().isEmpty()) {
            return ExpenseClassifier.classify(description);
        }

        try {
            DocumentCategorizerME myCategorizer = new DocumentCategorizerME(model);
            SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;

            // 2. Tokenizza la descrizione (in minuscolo per aumentare i match)
            String[] tokens = tokenizer.tokenize(description.toLowerCase());

            // 3. Ottieni la categoria
            double[] outcomes = myCategorizer.categorize(tokens);
            String category = myCategorizer.getBestCategory(outcomes);

            // 4. IMPORTANTE: Sostituisci gli underscore con spazi
            // "Ristorazione_e_Bar" -> "Ristorazione e Bar"
            return category.replace("_", " ");

        } catch (Exception e) {
            // In caso di errore imprevisto, fallback sul vecchio sistema
            return ExpenseClassifier.classify(description);
        }
    }

    // Metodo per addestrare il modello via API (Salva sempre nel file system locale)
    public String trainModel(String trainingData) throws IOException {

        // Factory dei dati
        InputStreamFactory dataIn = () -> new ByteArrayInputStream(trainingData.getBytes(StandardCharsets.UTF_8));

        try (ObjectStream<String> lineStream = new PlainTextByLineStream(dataIn, StandardCharsets.UTF_8);
             ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(lineStream)) {

            TrainingParameters params = TrainingParameters.defaultParams();
            params.put(TrainingParameters.ITERATIONS_PARAM, 100);
            params.put(TrainingParameters.CUTOFF_PARAM, 0); // 0 = impara anche parole che appaiono una volta sola

            DoccatModel trainedModel = DocumentCategorizerME.train("it", sampleStream, params, new DoccatFactory());

            // Salva su file system locale (così puoi prenderlo e spostarlo in resources dopo)
            try (OutputStream modelOut = new BufferedOutputStream(new FileOutputStream(MODEL_FILE_LOCAL))) {
                trainedModel.serialize(modelOut);
            }

            this.model = trainedModel; // Aggiorna il modello in memoria
            return "Training completato! File salvato in root come '" + MODEL_FILE_LOCAL + "'.";
        }
    }
}