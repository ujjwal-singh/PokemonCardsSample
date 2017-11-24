package com.example.ujjwal.pokemoncardssample.utils;

import com.example.ujjwal.pokemoncardssample.Constants;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 *  This class is a util class for getting current UTC time.
 *
 *  @author ujjwal
 */
public final class UTCTime {

    /**
     *  Util classes should not be initialized.
     *  Prevents initialization.
     */
    private UTCTime() {

    }

    /**
     *  This method returns the current UTC (milli sec) time as a long.
     *
     *  @return Long UTC Time in  milli sec.
     */
    public static long getUtcTime() {

        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                Constants.DATE_FORMAT);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(Constants.
                TIME_ZONE));

        Date currentDate = new Date();
        try {
            currentDate = simpleDateFormat.parse(simpleDateFormat.
                    format(new Date()));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return (currentDate.getTime());
    }
}
