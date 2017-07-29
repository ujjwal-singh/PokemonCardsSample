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

import com.example.ujjwal.pokemoncardssample.dao.SharedPreferencesHelper;
import com.example.ujjwal.pokemoncardssample.dao.dynamodb.DDBClient;
import com.example.ujjwal.pokemoncardssample.dao.dynamodb.UserAuthentication;
import com.example.ujjwal.pokemoncardssample.utils.HashCalculator;
import com.example.ujjwal.pokemoncardssample.utils.BooleanHolder;

/**
 *  The main activity class.
 *  The app starts from here.
 */
public class MainActivity extends AppCompatActivity {

    /** DDBClient for this class. */
    private DDBClient ddbClient;
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
            Toast.makeText(this, R.string.usernameEmpty, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        password = signUpPassword.getText().toString();
        if (password.equals(Constants.EMPTY_STRING)) {
            Toast.makeText(this, R.string.passwordEmpty, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        /** Matching password and re-password. */
        rePassword = signUpRePassword.getText().toString();
        if (!(password.equals(rePassword))) {
            Toast.makeText(this, R.string.rePasswordWrong, Toast.LENGTH_SHORT)
                    .show();
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

        /** Thread to create/register new user. */
        Thread createUserThread = new Thread() {
            @Override
            public void run() {

                /** Username already exists. */
                if (ddbClient.retrieveUser(username) != null) {
                    userExists.setValue(true);
                } else {
                    ddbClient.createUser(username,
                            HashCalculator.getMD5Hash(password));
                    sharedPreferencesHelper.writeUsername(username);
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

        /** If new user creation was successful (username did not clash),
         *  goto HomePage. */
        if (!(userExists.isValue())) {
            Toast.makeText(this, R.string.userCreationSuccessful,
                    Toast.LENGTH_SHORT)
                    .show();
            Intent intent = new Intent(this, HomePage.class);
            startActivity(intent);
        } else {
            Toast.makeText(MainActivity.this, R.string.usernameExists,
                    Toast.LENGTH_SHORT)
                    .show();
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
            Toast.makeText(this, R.string.usernameEmpty, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        password = signInPassword.getText().toString();
        if (password.equals(Constants.EMPTY_STRING)) {
            Toast.makeText(this, R.string.passwordEmpty, Toast.LENGTH_SHORT)
                    .show();
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

        /** Thread to sign-in user. */
        Thread signInUserThread = new Thread() {
            @Override
            public void run() {

                UserAuthentication userAuthentication = ddbClient.retrieveUser(
                        username);

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

        /** Username does not exist. */
        if (!(userExists.isValue())) {
            Toast.makeText(this, R.string.userDoesNotExist, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        /** Incorrect password for the username provided. */
        if (!(passwordCorrect.isValue())) {
            Toast.makeText(this, R.string.wrongPassword, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        /** Successful signIn. */
        Toast.makeText(this, R.string.signInSuccessful, Toast.LENGTH_SHORT)
                .show();
        Intent intent = new Intent(this, HomePage.class);
        startActivity(intent);
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