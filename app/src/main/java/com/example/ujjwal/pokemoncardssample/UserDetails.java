package com.example.ujjwal.pokemoncardssample;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.AmazonClientException;
import com.example.ujjwal.pokemoncardssample.dao.SharedPreferencesHelper;
import com.example.ujjwal.pokemoncardssample.dao.dynamodb.DDBClient;
import com.example.ujjwal.pokemoncardssample.dao.dynamodb.UserHistory;
import com.example.ujjwal.pokemoncardssample.utils.BooleanHolder;
import com.example.ujjwal.pokemoncardssample.utils.Holder;

/**
 *  This activity is the activity which displays
 *  user details.
 */
public class UserDetails extends AppCompatActivity {

    /** Reference to the only object of the
     * SharedPreferencesHelper singleton class. */
    private SharedPreferencesHelper sharedPreferencesHelper;

    /** Reference to the only object of the DDBClient singleton class. */
    private DDBClient ddbClient;

    /** Toast object for this class. */
    private Toast myToast = null;

    /** Stores the time of last toast message display. */
    private int lastToastDisplayTime;

    /** Stores the last message displayed by the Toast. */
    private String lastToastMessage;

    /** Text View for displaying username of the user. */
    private TextView userDetailsUsername;

    /** Text View for displaying number of matches played
     *  by the user. */
    private TextView userDetailsMatchesPlayed;

    /** Text View for displaying number of matches won
     *  by the user. */
    private TextView userDetailsMathcesWon;

    /** Text View for displaying number of matches lost
     *  by the user. */
    private TextView userDetailsMathcesLost;

    /**
     *  Overriding onCreate method.
     *  @param savedInstanceState Bundle savedInstanceState
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        sharedPreferencesHelper = SharedPreferencesHelper.getInstance();
        ddbClient = DDBClient.getInstance();

        lastToastDisplayTime = 0;

        lastToastMessage = null;

        userDetailsUsername = (TextView) this.findViewById(R.id.
                userDetailsUsername);
        userDetailsMatchesPlayed = (TextView) this.findViewById(R.id.
                userDetailsMatchesPlayed);
        userDetailsMathcesWon = (TextView) this.findViewById(R.id.
                userDetailsMatchesWon);
        userDetailsMathcesLost = (TextView) this.findViewById(R.id.
                userDetailsMatchesLost);

        fetchAndDisplayData();
    }

    /**
     *  This method builds up the menu.
     *  @param menu Menu object.
     *  @return Boolean
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {

        /** Inflate the menu; this adds items to the action bar if present. */
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.app_bar, menu);

        /** Hiding unnecessary buttons for User Details page. */
        MenuItem signOutButton = menu.findItem(R.id.signOut);
        signOutButton.setVisible(false);
        MenuItem deleteAccountButton = menu.findItem(R.id.deleteAccount);
        deleteAccountButton.setVisible(false);
        MenuItem searchPlayersButton = menu.findItem(R.id.searchPlayers);
        searchPlayersButton.setVisible(false);

        return true;
    }

    /**
     *  This method handles menu selections.
     *  @param item MenuItem item.
     *  @return boolean
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        /** Handle action bar item clicks here. The action bar will
         *  automatically handle clicks on the Home/Up button, so long
         *  as you specify a parent activity in AndroidManifest.xml.
         */

        switch (item.getItemId()) {

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     *  This method fetches required user data from the Dynamo DB
     *  tables and displays it.
     *  Calls itself recursively upon failure.
     */
    private void fetchAndDisplayData() {

        /* Displaying username of user. */
        userDetailsUsername.setText(sharedPreferencesHelper.getUsername());

        /** BooleanHolder object to indicate whether
         *  connection was successful or not.
         *  True means the connection was successful.
         */
        final BooleanHolder connectionSuccessful = new
                BooleanHolder(true);

        final Holder userHistoryHolder = new Holder(new UserHistory());

        Thread getUserDataThread = new Thread() {
            @Override
            public void run() {

                try {
                    userHistoryHolder.setValue(ddbClient.retrieveUserHistory(
                            sharedPreferencesHelper.getUsername()));
                } catch (AmazonClientException e) {
                    connectionSuccessful.setValue(false);
                }
            }
        };

        getUserDataThread.start();
        try {
            /** Wait for the thread to finish. */
            getUserDataThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        /** Check whether the connection was successful or not.
         *  If unsuccessful, then report connection problem
         *  and return. */
        if (!connectionSuccessful.isValue()) {
            showToast(getResources().getString(R.string.connectionProblem),
                    Toast.LENGTH_SHORT);

            /* Retry upon failure. */
            fetchAndDisplayData();
        }

        /*  Displaying details of the user (except username, which has
         *  been displayed previously). */
        UserHistory userHistory = (UserHistory) userHistoryHolder.getValue();
        userDetailsMatchesPlayed.setText(String.valueOf(userHistory.
                getGamesPlayed()));
        userDetailsMathcesWon.setText(String.valueOf(userHistory.
                getGamesWon()));
        userDetailsMathcesLost.setText(String.valueOf(userHistory.
                getGamesLost()));
    }

    /**
     *  This method can be used by other class' objects
     *  to display toasts on the Home Page.
     *  @param message  String message.
     *  @param duration int duration,
     *                  generally Toast.LENGTH_SHORT or
     *                  Toast.LENGTH_LONG .
     */
    public void showToast(final String message, final int duration) {

        int currentToastDisplayTime = (int) (System.currentTimeMillis());

        if (lastToastMessage != null) {
            if (message.equals(lastToastMessage)
                    && (currentToastDisplayTime
                    - lastToastDisplayTime <= Constants.
                    TOAST_MESSAGE_SEPARATION_TIME)) {

                return;
            }
        }

        lastToastMessage = message;
        lastToastDisplayTime = currentToastDisplayTime;

        /* Final context object to be used inside the below thread. */
        final Context context = this;

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                myToast = Toast.makeText(context, message, duration);
                myToast.show();
            }
        });
    }
}
