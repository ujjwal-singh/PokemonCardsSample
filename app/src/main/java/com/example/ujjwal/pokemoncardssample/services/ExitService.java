package com.example.ujjwal.pokemoncardssample.services;

import android.app.ActivityManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;

import com.example.ujjwal.pokemoncardssample.Constants;
import com.example.ujjwal.pokemoncardssample.MainActivity;
import com.example.ujjwal.pokemoncardssample.dao.SharedPreferencesHelper;
import com.example.ujjwal.pokemoncardssample.dao.dynamodb.DDBClient;

import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;

import java.util.Random;

/**
 *  Created by ujjwal on 22/7/17.
 *  This class starts a service for carrying out
 *  back-end exit functions when the app exits, or
 *  goes to background.
 */

public class ExitService extends IntentService {

    /** Reference to the only object of DDBClient Singleton class. */
    private DDBClient ddbClient;

    /** Reference to the only object of SharedPreferencesHelper class. */
    private SharedPreferencesHelper sharedPreferencesHelper;

    /**
     * A constructor is required, and must call the super IntentService(String)
     * constructor with a name for the worker thread.
     */
    public ExitService() {

        super(Constants.EXIT_SERVICE_WORKER_THREAD_NAME);

        ddbClient = DDBClient.getInstance();
        sharedPreferencesHelper = SharedPreferencesHelper.getInstance();
    }

    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns,
     * IntentService stops the service, as appropriate.
     */
    @Override
    protected final void onHandleIntent(final Intent intent) {

        /*
         * Wait for some time so that app's background status is clear.
         */
        try {
            Thread.sleep(Constants.ON_PAUSE_THREAD_WAIT_MILLI_SEC);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        /** Check if app is in background or not.
         * Prevents clashing DDB calls in case of
         * signOut and deleteAccount requests. */
        ActivityManager.RunningAppProcessInfo appProcessInfo = new
                ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(appProcessInfo);
        if (!(appProcessInfo.importance == IMPORTANCE_FOREGROUND
                || appProcessInfo.importance == IMPORTANCE_VISIBLE)) {

            ddbClient.setUserOnlineAvailability(
                    sharedPreferencesHelper.getUsername(), false);
        }

        stopForeground(true);
    }

    /**
     *  Overriding onCreate method.
     *  This method starts the service as a foreground service so that it
     *  isn't killed immediately on app exit.
     */
    @Override
    public final void onCreate() {
        super.onCreate();

        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentIntent(pendingIntent).build();

        int notificationId = (new Random()).nextInt(1000);

        startForeground(notificationId, notification);
    }
}
