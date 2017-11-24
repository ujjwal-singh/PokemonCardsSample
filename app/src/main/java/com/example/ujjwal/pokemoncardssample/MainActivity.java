package com.example.ujjwal.pokemoncardssample;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View.OnClickListener;

import java.util.regex.Pattern;

import com.amazonaws.AmazonClientException;
import com.example.ujjwal.pokemoncardssample.dao.SharedPreferencesHelper;
import com.example.ujjwal.pokemoncardssample.dao.dynamodb.DDBClient;
import com.example.ujjwal.pokemoncardssample.dao.dynamodb.UserAuthentication;
import com.example.ujjwal.pokemoncardssample.dao.sqs.SQSClient;
import com.example.ujjwal.pokemoncardssample.utils.BooleanHolder;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.SignInButton;

/**
 *  The main activity class.
 *  The app starts from here.
 */
public class MainActivity extends AppCompatActivity
        implements OnConnectionFailedListener, OnClickListener {

    /** DDBClient for this class. */
    private DDBClient ddbClient;

    /** SQSClient for this class. */
    private SQSClient sqsClient;

    /** SharedPreferencesHelper object for this class. */
    private SharedPreferencesHelper sharedPreferencesHelper;

    /** Sign-Up username entry text-box. */
    private TextView signUpUsername;

    /** Sign-In button. */
    private SignInButton signInButton;

    /** Sign-Up button. */
    private SignInButton signUpButton;

    /** Toast object for this class. */
    private Toast myToast = null;

    /** Stores the time of last toast message display. */
    private int lastToastDisplayTime;

    /** Stores the last message displayed by the Toast. */
    private String lastToastMessage;

    /** Google API Client for Google sign-in. */
    private GoogleApiClient mGoogleApiClient;

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

        signUpUsername = (TextView) this.findViewById(R.id.signUpUsername);

        signInButton = (SignInButton) this.findViewById(R.id.signInButton);
        signUpButton = (SignInButton) this.findViewById(R.id.signUpButton);
        initializeGoogleButtons();

        sharedPreferencesHelper = SharedPreferencesHelper.getInstance(this);
        ddbClient = DDBClient.getInstance(this);
        sqsClient = SQSClient.getInstance(this);

        lastToastDisplayTime = 0;

        lastToastMessage = null;

        /* Configure sign-in to request the user's ID, email address, and basic
         * profile. ID and basic profile are included in DEFAULT_SIGN_IN.
         */
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        /* Build a GoogleApiClient with access to the Google Sign-In API and the
         * options specified by gso.
         */
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

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
     *  This method is used to initialize Google's
     *  sign-in and sign-up buttons.
     */
    private void initializeGoogleButtons() {

        signInButton.setOnClickListener(this);
        signUpButton.setOnClickListener(this);

        ((TextView) signInButton.getChildAt(0)).setText(R.string.
                googleSignInButtonText);
        ((TextView) signUpButton.getChildAt(0)).setText(R.string.
                googleSignUpButtonText);
    }

    /**
     *  Handles creation of new user.
     *  @param emailId String, gmail username of the user.
     */
    public void signUp(final String emailId) {

        /** username entered by the user. */
        final String username;

        /* Username validity checks.
        *  The username can have only alphanumeric characters
        *  or underscore, and its length should be between
        *  4 and 80 (both inclusive). */
        username = signUpUsername.getText().toString();
        if (!Pattern.matches(Constants.USERNAME_REGEX, username)) {
            showToast(getResources().getString(R.string.usernameInvalid),
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
        final BooleanHolder userNameExists = new BooleanHolder(false);

        /** BooleanHolder object to indicate whether
         *  email-id already exists or not.
         *  False means does not exist. */
        final BooleanHolder emailIdExists = new BooleanHolder(false);

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

                        userNameExists.setValue(true);
                    } else if (ddbClient.
                            retrieveUserByEmaiId(emailId) != null) {

                        emailIdExists.setValue(true);
                    } else {
                        sqsClient.createQueue(username);
                        ddbClient.createUser(username, emailId);
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

        if (userNameExists.isValue()) {

            /* Username clashed. */
            showToast(getResources().getString(R.string.usernameExists),
                    Toast.LENGTH_SHORT);
        } else if (emailIdExists.isValue()) {

            /* Email ID clashed. */
            showToast(getResources().getString(R.string.emailIdExists),
                    Toast.LENGTH_SHORT);
        } else {

            /* Input valid. Display user creation successful toast
             *  and move to new activity. */
            showToast(getResources().getString(R.string.userCreationSuccessful),
                    Toast.LENGTH_SHORT);
            Intent intent = new Intent(this, HomePage.class);
            startActivity(intent);
        }
    }

    /**
     *  Handles signIn request.
     *  @param emailId String, gmail username of the user.
     */
    private void signIn(final String emailId) {

        /** Input is valid. Now going for sign-in. */

        /** Disable buttons. */
        signUpButton.setEnabled(false);
        signInButton.setEnabled(false);

        /** BooleanHolder object to indicate whether username exists or not.
         *  True means exists. */
        final BooleanHolder userExists = new BooleanHolder(true);

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
                            .retrieveUserByEmaiId(emailId);

                    /** Username exists. */
                    if (userAuthentication != null) {

                        sharedPreferencesHelper.writeUsername(
                                userAuthentication.getUsername());
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
            showToast(getResources().getString(R.string.emailIdDoesNotExist),
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
     *  This method is the listener method for button clicks.
     *  @param view View view, determines which button has
     *              been clicked.
     */
    @Override
    public void onClick(final View view) {

        mGoogleApiClient.clearDefaultAccountAndReconnect();
        Intent googleAuthIntent = Auth.GoogleSignInApi.getSignInIntent(
                mGoogleApiClient);

        switch (view.getId()) {

            case R.id.signInButton:
                startActivityForResult(googleAuthIntent, Constants.
                        GOOGLE_SIGN_IN_REQUEST_CODE);
                break;

            case R.id.signUpButton:
                startActivityForResult(googleAuthIntent, Constants.
                        GOOGLE_SIGN_UP_REQUEST_CODE);
                break;

            default:
                break;
        }
    }

    /**
     *  Method which handles connection failures for
     *  Google authentication.
     *  @param result   ConnectionResult
     */
    @Override
    public void onConnectionFailed(final ConnectionResult result) {

        showToast(getResources().getString(R.string.connectionProblem),
                Toast.LENGTH_SHORT);
    }

    /**
     *  Handles result of google sign-in activity.
     *  @param requestCode  Request Code
     *  @param resultCode   Result Code
     *  @param data Intent data
     */
    @Override
    public void onActivityResult(final int requestCode, final int resultCode,
                                 final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /* Result returned from launching the Intent from
         * GoogleSignInApi.getSignInIntent(...);
         */
        GoogleSignInResult result = Auth.GoogleSignInApi.
                getSignInResultFromIntent(data);
        googleAuthResult(result, requestCode);
    }

    /**
     *  This method handles sign-in result.
     *  @param result GoogleSignInResult
     *  @param requestCode The request code,
     *                     which differentiates sign-in and sign-up.
     */
    private void googleAuthResult(final GoogleSignInResult result,
                                  final int requestCode) {

        if (result.isSuccess()) {
            GoogleSignInAccount acct = result.getSignInAccount();

            if (requestCode == Constants.GOOGLE_SIGN_IN_REQUEST_CODE) {

                signIn(acct.getEmail());
            } else if (requestCode == Constants.GOOGLE_SIGN_UP_REQUEST_CODE) {

                signUp(acct.getEmail());
            }
        }
    }

    /**
     *  This method can be used by other class' objects
     *  to display toasts on the MainActivity Page.
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

    /**
     *  Overriding onBackPressed method.
     *  App exits on pressing back button.
     */
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
