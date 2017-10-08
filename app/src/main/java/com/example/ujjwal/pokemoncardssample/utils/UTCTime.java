package com.example.ujjwal.pokemoncardssample.utils;

import com.example.ujjwal.pokemoncardssample.Constants;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

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
     *  Returns 0L in case of exception.
     *
     *  @return Long UTC Time in  milli sec.
     */
    public static long getUtcTime() {

        try {

            NTPUDPClient timeClient = new NTPUDPClient();

            /* List of servers available at :
             * http://tf.nist.gov/tf-cgi/servers.cgi# */
            InetAddress inetAddress = InetAddress.getByName(Constants.
                    TIME_SERVER_ADDRESS);
            TimeInfo timeInfo = timeClient.getTime(inetAddress);
            return (timeInfo.getReturnTime());
        } catch (UnknownHostException e) {

            e.printStackTrace();
            return 0L;
        } catch (IOException e) {

            e.printStackTrace();
            return 0L;
        }
    }
}
