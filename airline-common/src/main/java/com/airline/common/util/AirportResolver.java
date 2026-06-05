package com.airline.common.util;

import java.util.HashMap;
import java.util.Map;

public class AirportResolver {
    private static final Map<String, String> CITY_TO_IATA = new HashMap<>();

    static {
        // India
        CITY_TO_IATA.put("CHENNAI", "MAA");
        CITY_TO_IATA.put("MADRAS", "MAA");
        CITY_TO_IATA.put("TAMIL NADU", "MAA");
        CITY_TO_IATA.put("TAMILNADU", "MAA");
        
        CITY_TO_IATA.put("MUMBAI", "BOM");
        CITY_TO_IATA.put("BOMBAY", "BOM");
        CITY_TO_IATA.put("MAHARASHTRA", "BOM");
        
        CITY_TO_IATA.put("DELHI", "DEL");
        CITY_TO_IATA.put("NEW DELHI", "DEL");
        CITY_TO_IATA.put("NEWDELHI", "DEL");
        
        CITY_TO_IATA.put("BANGALORE", "BLR");
        CITY_TO_IATA.put("BENGALURU", "BLR");
        CITY_TO_IATA.put("KARNATAKA", "BLR");
        
        CITY_TO_IATA.put("HYDERABAD", "HYD");
        CITY_TO_IATA.put("SECUNDERABAD", "HYD");
        CITY_TO_IATA.put("TELANGANA", "HYD");
        
        CITY_TO_IATA.put("KOLKATA", "CCU");
        CITY_TO_IATA.put("CALCUTTA", "CCU");
        CITY_TO_IATA.put("WEST BENGAL", "CCU");
        CITY_TO_IATA.put("WESTBENGAL", "CCU");

        CITY_TO_IATA.put("GOA", "GOI");
        CITY_TO_IATA.put("COCHIN", "COK");
        CITY_TO_IATA.put("KOCHI", "COK");
        CITY_TO_IATA.put("KERALA", "COK");
        
        CITY_TO_IATA.put("PUNE", "PNQ");
        CITY_TO_IATA.put("AHMEDABAD", "AMD");
        CITY_TO_IATA.put("GUJARAT", "AMD");
        CITY_TO_IATA.put("JAIPUR", "JAI");
        CITY_TO_IATA.put("RAJASTHAN", "JAI");
        CITY_TO_IATA.put("LUCKNOW", "LKO");
        CITY_TO_IATA.put("UTTAR PRADESH", "LKO");

        // US
        CITY_TO_IATA.put("NEW YORK", "JFK");
        CITY_TO_IATA.put("NEWYORK", "JFK");
        CITY_TO_IATA.put("LOS ANGELES", "LAX");
        CITY_TO_IATA.put("LOSANGELES", "LAX");
        CITY_TO_IATA.put("CALIFORNIA", "LAX");
        CITY_TO_IATA.put("DALLAS", "DFW");
        CITY_TO_IATA.put("TEXAS", "DFW");
        CITY_TO_IATA.put("MIAMI", "MIA");
        CITY_TO_IATA.put("FLORIDA", "MIA");
        CITY_TO_IATA.put("CHICAGO", "ORD");
        CITY_TO_IATA.put("SAN FRANCISCO", "SFO");
        CITY_TO_IATA.put("SEATTLE", "SEA");
        CITY_TO_IATA.put("BOSTON", "BOS");
        CITY_TO_IATA.put("DENVER", "DEN");

        // International
        CITY_TO_IATA.put("LONDON", "LHR");
        CITY_TO_IATA.put("HEATHROW", "LHR");
        CITY_TO_IATA.put("ENGLAND", "LHR");
        CITY_TO_IATA.put("UNITED KINGDOM", "LHR");
        CITY_TO_IATA.put("UK", "LHR");
        
        CITY_TO_IATA.put("PARIS", "CDG");
        CITY_TO_IATA.put("FRANCE", "CDG");
        
        CITY_TO_IATA.put("MUNICH", "MUC");
        CITY_TO_IATA.put("GERMANY", "MUC");
        CITY_TO_IATA.put("FRANKFURT", "FRA");
        
        CITY_TO_IATA.put("DUBAI", "DXB");
        CITY_TO_IATA.put("UAE", "DXB");
        
        CITY_TO_IATA.put("TOKYO", "NRT");
        CITY_TO_IATA.put("NARITA", "NRT");
        CITY_TO_IATA.put("JAPAN", "NRT");
        
        CITY_TO_IATA.put("SYDNEY", "SYD");
        CITY_TO_IATA.put("AUSTRALIA", "SYD");
        
        CITY_TO_IATA.put("TORONTO", "YYZ");
        CITY_TO_IATA.put("CANADA", "YYZ");
    }

    /**
     * Resolves a query input (city name, state, composite name, or IATA code) to an IATA code.
     * If it cannot find a mapping, it returns the cleaned original uppercase string.
     */
    public static String resolveToIata(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        
        String cleaned = input.trim().toUpperCase();
        
        // Handle parentheses format: "Tamil Nadu (MAA)"
        if (cleaned.contains("(") && cleaned.contains(")")) {
            int openIdx = cleaned.indexOf('(');
            int closeIdx = cleaned.indexOf(')');
            if (closeIdx > openIdx) {
                String parenContent = cleaned.substring(openIdx + 1, closeIdx).trim();
                if (parenContent.length() == 3) {
                    return parenContent;
                }
            }
        }
        
        // Direct map lookup
        if (CITY_TO_IATA.containsKey(cleaned)) {
            return CITY_TO_IATA.get(cleaned);
        }
        
        // Strip out descriptors and try again
        String basic = cleaned.replace(" CITY", "")
                              .replace(" STATE", "")
                              .replace(" PROVINCE", "")
                              .replace(" REGION", "")
                              .replace(" INTERNATIONAL AIRPORT", "")
                              .replace(" AIRPORT", "")
                              .trim();
                              
        if (CITY_TO_IATA.containsKey(basic)) {
            return CITY_TO_IATA.get(basic);
        }
        
        return cleaned; // Fallback
    }
}
