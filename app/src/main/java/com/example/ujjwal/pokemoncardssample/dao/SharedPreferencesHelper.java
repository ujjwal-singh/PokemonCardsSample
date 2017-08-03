package com.example.ujjwal.pokemoncardssample.dao;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.ujjwal.pokemoncardssample.Constants;

/**
 *  Created by ujjwal on 19/7/17.
 *  This is the SharedPreferences manager class.
 *  A Singleton class.
 */

public final class SharedPreferencesHelper {

    /** The default username returned when no username is currently in store. */
    private static final String DEFAULT_USERNAME = "";

    /** SharedPreferences object to handle tasks. */
    private static SharedPreferences sharedPreferences;

    /** The only instance of the Singleton class. */
    private static SharedPreferencesHelper instance = null;

    /**
     *  This method returns an instance of the class,
     *  which is the one and only instance
     *  of the Singleton class.
     *  Uses lazy initialization.
     *  @param context  Context context, passed by the calling activity.
     *  @return SharedPreferencesHelper object, the only object of the class.
     */
    public static SharedPreferencesHelper getInstance(final Context context) {

        if (instance == null) {
            instance = new SharedPreferencesHelper(context);
        }
        return instance;
    }

    /**
     *  Method overload.
     *  This method returns the current instance.
     *  Can return null.
     *  @return SharedPreferencesHelper object, the only object of the class.
     */
    public static SharedPreferencesHelper getInstance() {
        return instance;
    }

    /**
     *  Private constructor, disables initialization from other classes.
     *  @param context  Context object passed by the calling activity.
     */
    private SharedPreferencesHelper(final Context context) {

        /**
         *  FindBugs will throws the following warning --
         *  This instance method writes to a static field.
         *  This is tricky to get correct if multiple instances
         *  are being manipulated, and generally bad practice.
         *
         *  However, this is a Singleton class.
         */

        sharedPreferences = context.getSharedPreferences(
                Constants.PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
    }

    /**
     *  This method checks if a username is available.
     *  @return Boolean, true if username is available, false otherwise.
     */
    public static boolean checkUsernamePresence() {

        String username = sharedPreferences.getString(
                Constants.PREFERENCE_USERNAME_KEY, DEFAULT_USERNAME);

        return (!(username.equals(DEFAULT_USERNAME)));
    }

    /**
     *  This method fetches the name of the current signed-in user.
     *  Returns empty string if no username found.
     *  @return String, username if present else empty string.
     */
    public static String getUsername() {

        return (sharedPreferences.getString(
                Constants.PREFERENCE_USERNAME_KEY, DEFAULT_USERNAME));
    }

    /**
     *  This method writes a username to the SharedPreferences file.
     *  @param username String username, the name to be written.
     */
    public static void writeUsername(final String username) {

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.PREFERENCE_USERNAME_KEY, username);
        editor.commit();
    }

    /**
     *  This method deletes the username.
     */
    public static void removeUsername() {

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(Constants.PREFERENCE_USERNAME_KEY);
        editor.commit();
    }
}
