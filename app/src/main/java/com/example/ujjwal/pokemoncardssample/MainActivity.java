package com.example.ujjwal.pokemoncardssample;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.AmazonClientException;
import com.example.ujjwal.pokemoncardssample.dao.SharedPreferencesHelper;
import com.example.ujjwal.pokemoncardssample.dao.dynamodb.DDBClient;
import com.example.ujjwal.pokemoncardssample.dao.dynamodb.UserAuthentication;
import com.example.ujjwal.pokemoncardssample.dao.sqs.SQSClient;
import com.example.ujjwal.pokemoncardssample.utils.HashCalculator;
import com.example.ujjwal.pokemoncardssample.utils.BooleanHolder;

/**
 *  The main activity class.
 *  The app starts from here.
 */
public class MainActivity extends AppCompatActivity {

    /** DDBClient for this class. */
    private DDBClient ddbClient;

    /** SQSClient for this class. */
    private SQSClient sqsClient;

    /** SharedPreferencesHelper object for this class. */
    private SharedPreferencesHelper sharedPreferencesHelper;

    /** Sign-In username entry text-box. */
    private TextView signInUsername;

    /** Sign-In password entry text-box. */
    private TextView signInPassword;

    /** Sign-Up username entry text-box. */
    private TextView signUpUsername;

    /** Sign-Up password entry text-box. */
    private TextView signUpPassword;

    /** Sign-Up password repeat entry text-box. */
    private TextView signUpRePassword;

    /** Sign-In button. */
    private Button signInButton;

    /** Sign-Up button. */
    private Button signUpButton;

    /** Toast object for this class. */
    private Toast myToast = null;

    /**
     *  This is the method which is called upon activity creation.
     *  @param savedInstanceState
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        signInUsername = (TextView) this.findViewById(R.id.username);
        signInPassword = (TextView) this.findViewById(R.id.password);
        signUpUsername = (TextView) this.findViewById(R.id.signUpUsername);
        signUpPassword = (TextView) this.findViewById(R.id.signUpPassword);
        signUpRePassword = (TextView) this.findViewById(
                R.id.signUpPasswordRepeat);

        signInButton = (Button) this.findViewById(R.id.signInButton);
        signUpButton = (Button) this.findViewById(R.id.signUpButton);

        sharedPreferencesHelper = SharedPreferencesHelper.getInstance(this);
        ddbClient = DDBClient.getInstance(this);
        sqsClient = SQSClient.getInstance(this);

        initCheck();
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

        /** Hiding unnecessary buttons for MainActivity page. */
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
     *  This method checks if a user is already signed in.
     */
    private void initCheck() {

        //  If already signed in, then goto the home page.
        if (sharedPreferencesHelper.checkUsernamePresence()) {

            Intent intent = new Intent(this, HomePage.class);
            startActivity(intent);
        }
    }

    /**
     *  This method is handler method for signUp button.
     *  Handles creation of new user.
     *  @param view View view.
     */
    public void signUp(final View view) {

        /** username entered by the user. */
        final String username;

        /** password entered by the user. */
        final String password;

        /** rePassword entered by the user. */
        final String rePassword;

        /** Empty checks */
        username = signUpUsername.getText().toString();
        if (username.equals(Constants.EMPTY_STRING)) {
            showToast(getResources().getString(R.string.usernameEmpty),
                    Toast.LENGTH_SHORT);
            return;
        }

        password = signUpPassword.getText().toString();
        if (password.equals(Constants.EMPTY_STRING)) {
            showToast(getResources().getString(R.string.passwordEmpty),
                    Toast.LENGTH_SHORT);
            return;
        }

        /** Matching password and re-password. */
        rePassword = signUpRePassword.getText().toString();
        if (!(password.equals(rePassword))) {
            showToast(getResources().getString(R.string.rePasswordWrong),
                    Toast.LENGTH_SHORT);
            return;
        }

        /** Input is valid. Now going for registration. */

        /** Disable buttons. */
        signUpButton.setEnabled(false);
        signInButton.setEnabled(false);

        /** BooleanHolder object to indicate whether
         *  username already exists or not.
         *  False means does not exist. */
        final BooleanHolder userExists = new BooleanHolder(false);

        /** BooleanHolder object to indicate whether
         *  connection was successful or not.
         *  True means the connection was successful.
         */
        final BooleanHolder connectionSuccessful = new BooleanHolder(true);

        /** Thread to create/register new user. */
        Thread createUserThread = new Thread() {
            @Override
            public void run() {

                try {
                    /** Username already exists. */
                    if (ddbClient.retrieveUser(username) != null) {
                        userExists.setValue(true);
                    } else {
                        ddbClient.createUser(username,
                                HashCalculator.getMD5Hash(password));
                        sqsClient.createQueue(username);
                        sharedPreferencesHelper.writeUsername(username);
                    }
                } catch (AmazonClientException e) {
                    connectionSuccessful.setValue(false);
                }
            }
        };

        /** Start user creation thread. */
        createUserThread.start();
        try {
            /** Wait for the thread to finish. */
            createUserThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        /** Enable buttons. */
        signUpButton.setEnabled(true);
        signInButton.setEnabled(true);

        /** Check whether the connection was successful or not.
         *  If unsuccessful, then report connection problem and return. */
        if (!connectionSuccessful.isValue()) {
            showToast(getResources().getString(R.string.connectionProblem),
                    Toast.LENGTH_SHORT);
            return;
        }

        /** If new user creation was successful (username did not clash),
         *  goto HomePage. */
        if (!(userExists.isValue())) {
            showToast(getResources().getString(R.string.userCreationSuccessful),
                    Toast.LENGTH_SHORT);
            Intent intent = new Intent(this, HomePage.class);
            startActivity(intent);
        } else {
            showToast(getResources().getString(R.string.usernameExists),
                    Toast.LENGTH_SHORT);
        }
    }

    /**
     *  This method is the handler for the signIn button.
     *  Handles signIn request.
     *  @param view View view.
     */
    public void signIn(final View view) {

        /** username entered by the user. */
        final String username;

        /** password entered by the user. */
        final String password;

        /** Empty checks */
        username = signInUsername.getText().toString();
        if (username.equals(Constants.EMPTY_STRING)) {
            showToast(getResources().getString(R.string.usernameEmpty),
                    Toast.LENGTH_SHORT);
            return;
        }

        password = signInPassword.getText().toString();
        if (password.equals(Constants.EMPTY_STRING)) {
            showToast(getResources().getString(R.string.passwordEmpty),
                    Toast.LENGTH_SHORT);
            return;
        }

        /** Input is valid. Now going for sign-in. */

        /** Disable buttons. */
        signUpButton.setEnabled(false);
        signInButton.setEnabled(false);

        /** BooleanHolder object to indicate whether username exists or not.
         *  True means exists. */
        final BooleanHolder userExists = new BooleanHolder(true);

        /** BooleanHolder object to indicate whether password is correct or not.
         * True means correct. */
        final BooleanHolder passwordCorrect = new BooleanHolder(true);

        /** BooleanHolder object to indicate whether
         *  connection was successful or not.
         *  True means the connection was successful.
         */
        final BooleanHolder connectionSuccessful = new BooleanHolder(true);

        /** Thread to sign-in user. */
        Thread signInUserThread = new Thread() {
            @Override
            public void run() {

                try {
                    UserAuthentication userAuthentication = ddbClient
                            .retrieveUser(username);

                    /** Username exists. */
                    if (userAuthentication != null) {

                        /** Password is correct. */
                        if ((userAuthentication.getPassword())
                                .equals(HashCalculator.getMD5Hash(password))) {
                            sharedPreferencesHelper.writeUsername(username);
                        } else {
                            passwordCorrect.setValue(false);
                        }
                    } else {
                        userExists.setValue(false);
                    }
                } catch (AmazonClientException e) {
                    connectionSuccessful.setValue(false);
                }
            }
        };

        /** Start user sign-in thread. */
        signInUserThread.start();
        try {
            /** Wait for the thread to finish. */
            signInUserThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        /** Enable buttons. */
        signUpButton.setEnabled(true);
        signInButton.setEnabled(true);

        /** Check whether the connection was successful or not.
         *  If unsuccessful, then report connection problem and return. */
        if (!connectionSuccessful.isValue()) {
            showToast(getResources().getString(R.string.connectionProblem),
                    Toast.LENGTH_SHORT);
            return;
        }

        /** Username does not exist. */
        if (!(userExists.isValue())) {
            showToast(getResources().getString(R.string.userDoesNotExist),
                    Toast.LENGTH_SHORT);
            return;
        }

        /** Incorrect password for the username provided. */
        if (!(passwordCorrect.isValue())) {
            showToast(getResources().getString(R.string.wrongPassword),
                    Toast.LENGTH_SHORT);
            return;
        }

        /** Successful signIn. */
        showToast(getResources().getString(R.string.signInSuccessful),
                Toast.LENGTH_SHORT);
        Intent intent = new Intent(this, HomePage.class);
        startActivity(intent);
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

        if (myToast != null) {
            myToast.cancel();
        }

        myToast = Toast.makeText(this, message, duration);
        myToast.show();
    }

    /**
     *  Overriding onBackPressed method.
     *  App exits on pressing back button.
     */
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
