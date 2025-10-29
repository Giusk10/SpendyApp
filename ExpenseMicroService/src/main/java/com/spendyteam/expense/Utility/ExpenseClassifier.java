package com.spendyteam.expense.Utility;
import java.util.*;

public class ExpenseClassifier {

    private static final Map<String, List<String>> categoryKeywords = Map.ofEntries(
            Map.entry("Abbonamenti e Servizi Digitali", List.of(
                    "Disney+", "Netflix", "Google One", "Amazon", "g2a.com"
            )),
            Map.entry("Supermercati e Alimentari", List.of(
                    "Carrefour", "Lidl", "Sole 365", "Green Garden", "Pantry", "Market"
            )),
            Map.entry("Trasporti", List.of(
                    "Uber", "FREE NOW", "Trenitalia", "Taxi", "Flight", "Airport"
            )),
            Map.entry("Ristorazione e Bar", List.of(
                    "McDonald's", "Burger King", "KFC", "Il Sauro Ristorante", "S. Paolo Ristorazione",
                    "Vino E Biga", "Dorys Caffe", "Bar Big", "Cannavina Bar", "Noemy Cafe", "Big Bang Sandwich",
                    "Young Pizza", "Gruppo la Piadineria", "Mastroianni", "Mariano Balato"
            )),
            Map.entry("Pagamenti e Trasferimenti", List.of(
                    "Transfer to Revolut user", "Transfer from Revolut user", "Payment from Riccio Giuseppe",
                    "Payment from Porto Vincenzo", "Payment from Iuliani Antonio", "Payment from Mangopay",
                    "Balance migration", "SumUp"
            )),
            Map.entry("Shopping e Abbigliamento", List.of(
                    "Zalando", "Douglas", "Vinted", "Proshop"
            )),
            Map.entry("Alloggi e Viaggi", List.of(
                    "Airbnb", "Hotel", "Booking", "Vacation"
            )),
            Map.entry("Varie", List.of(
                    "Samnite", "Samnet", "Margroup Societa", "Colella Group", "Fratelli Della Minerva",
                    "Officinastu", "Studiouno Grafhic Foto", "Mne 95016279moneynet"
            )),
            Map.entry("Carburante e Auto", List.of(
                    "Gas", "Fuel", "Petrol"
            ))
    );

    public static String classify(String description) {
        if (description == null || description.isBlank()) {
            return "Non classificato";
        }

        String descLower = description.toLowerCase();

        for (var entry : categoryKeywords.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (descLower.contains(keyword.toLowerCase())) {
                    return entry.getKey();
                }
            }
        }
        return "Non classificato";
    }
}


