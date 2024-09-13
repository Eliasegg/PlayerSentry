package com.eliaseeg.playersentry.utils;

import java.util.Calendar;
import java.util.Date;

public class GeneralUtils {

    public static Date calculateExpirationDate(String time) {
        int days = 0, hours = 0, minutes = 0;
        String[] parts = time.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");

        for (int i = 0; i < parts.length; i += 2) {
            if (i + 1 >= parts.length) break;
            int value = Integer.parseInt(parts[i]);
            char unit = parts[i + 1].charAt(0);
            switch (unit) {
                case 'd': days += value; break;
                case 'h': hours += value; break;
                case 'm': minutes += value; break;
                default: return null; // Invalid unit
            }
        }

        if (days == 0 && hours == 0 && minutes == 0) return null; // No valid time specified

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, days);
        calendar.add(Calendar.HOUR_OF_DAY, hours);
        calendar.add(Calendar.MINUTE, minutes);
        return calendar.getTime();
    }

}
