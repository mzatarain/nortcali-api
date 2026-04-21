package com.nortcali.api.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FolioGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private FolioGenerator() {}

    /**
     * Genera el folio de una orden con formato ORD-{restaurantId}-{yyyyMMdd}-{secuencia}.
     *
     * @param restaurantId ID del restaurante
     * @param date         Fecha de la orden
     * @param sequence     Número secuencial del día para ese restaurante
     * @return Folio generado, ej: ORD-1-20260414-0001
     */
    public static String generateOrderFolio(Long restaurantId, LocalDate date, long sequence) {
        return String.format("ORD-%d-%s-%04d", restaurantId, date.format(FORMATTER), sequence);
    }

    public static String folioPrefix(Long restaurantId, LocalDate date) {
        return String.format("ORD-%d-%s-", restaurantId, date.format(FORMATTER));
    }
}
