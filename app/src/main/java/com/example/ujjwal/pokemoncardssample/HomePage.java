package com.example.ujjwal.pokemoncardssample;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.amazonaws.AmazonClientException;
import com.example.ujjwal.pokemoncardssample.dao.SharedPreferencesHelper;
import com.example.ujjwal.pokemoncardssample.dao.dynamodb.DDBClient;
import com.example.ujjwal.pokemoncardssample.dao.dynamodb.UserAuthentication;
import com.example.ujjwal.pokemoncardssample.dao.sqs.SQSClient;
import com.example.ujjwal.pokemoncardssample.dao.sqs.SQSListener;
import com.example.ujjwal.pokemoncardssample.services.ExitService;
import com.example.ujjwal.pokemoncardssample.utils.BooleanHolder;
import com.example.ujjwal.pokemoncardssample.utils.HashCalculator;
import com.example.ujjwal.pokemoncardssample.utils.Holder;
import com.example.ujjwal.pokemoncardssample.utils.JsonKey;
import com.example.ujjwal.pokemoncardssample.utils.JsonValue;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 *  This class is the activity class for Home Page of the app.
 *  @author ujjwal
 */
public class HomePage extends AppCompatActivity {

    /** Reference to the only object of the
     * SharedPreferencesHelper singleton class. */
    private SharedPreferencesHelper sharedPreferencesHelper;

    /** Reference to the only object of the DDBClient singleton class. */
    private DDBClient ddbClient;

    /** Reference to the only object of the SQSClient singleton class. */
    private SQSClient sqsClient;

    /** SQSListener object for the user queue. */
    private SQSListener sqsListener;

    /** Toast object for this class. */
    private Toast myToast = null;

    /**
     *  Overriding onCreate method.
     *  @param savedInstanceState Bundle savedInstanceState
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        sharedPreferencesHelper = SharedPreferencesHelper.getInstance();
        ddbClient = DDBClient.getInstance();
        sqsClient = SQSClient.getInstance();

        startSqsListener();
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

            /**
             * Sign out option chosen by the user.
             */
            case R.id.signOut:

                return (this.signOut());

            /**
             *  Delete account option chosen by the user.
             */
            case R.id.deleteAccount:

                return (this.deleteAccount());

            /*
             *  Search players who are currently online.
             */
            case R.id.searchPlayers:

                return (this.searchPlayers());

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     *  This method is used to sign-out the user.
     *  @return Boolean
     */
    private boolean signOut() {

        /** BooleanHolder object to indicate whether
         *  connection was successful or not.
         *  True means the connection was successful.
         */
        final BooleanHolder connectionSuccessful = new
                BooleanHolder(true);

        Thread signOutThread = new Thread() {
            @Override
            public void run() {

                try {
                    ddbClient.setUserAvailability(
                            sharedPreferencesHelper.getUsername(),
                            false, false);
                    sharedPreferencesHelper.removeUsername();
                } catch (AmazonClientException e) {
                    connectionSuccessful.setValue(false);
                }
            }
        };

        signOutThread.start();
        try {
            /** Wait for the thread to finish. */
            signOutThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        /** Check whether the connection was successful or not.
         *  If unsuccessful, then report connection problem
         *  and return. */
        if (!connectionSuccessful.isValue()) {
            showToast(getResources().getString(R.string.connectionProblem),
                    Toast.LENGTH_SHORT);
            return true;
        }

        showToast(getResources().getString(R.string.signOutSuccessful),
                Toast.LENGTH_SHORT);
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        return true;
    }

    /**
     *  This method is used to delete user account.
     *  @return Boolean
     */
    private boolean deleteAccount() {

        /** Delete account dialog box builder. */
        AlertDialog.Builder deleteAccountDialogBuilder = new
                AlertDialog.Builder(this);
        deleteAccountDialogBuilder.setTitle(
                R.string.confirmAccountDelete);

        String message = String.format(getResources().
                getString(R.string.accountDeletionText),
                sharedPreferencesHelper.getUsername());
        deleteAccountDialogBuilder.setMessage(message);

        /** EditText set up to receive password confirmation. */
        final EditText passwordEditText = new EditText(this);
        /** Specifying type of input expected as Password. */
        passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        deleteAccountDialogBuilder.setView(passwordEditText);

        /** context will be needed to display toasts. */
        final Context context = this;

        /** Handler for positive button click. */
        deleteAccountDialogBuilder.setPositiveButton(
                R.string.deleteAccount,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog,
                                        final int which) {

                        /** BooleanHolder object to indicate whether
                         *  user deletion was successful or not.
                         *  True means user deletion was successful. */
                        final BooleanHolder userDeletedSuccessfully = new
                                BooleanHolder(true);

                        /** BooleanHolder object to indicate whether
                         *  connection was successful or not.
                         *  True means the connection was successful.
                         */
                        final BooleanHolder connectionSuccessful = new
                                BooleanHolder(true);

                        /** New thread to carry out user deletion activities. */
                        Thread deleteUserThread = new Thread() {
                            @Override
                            public void run() {

                                try {
                                    String username = sharedPreferencesHelper
                                            .getUsername();
                                    String password = passwordEditText
                                            .getText().toString();

                                    UserAuthentication userAuthentication =
                                            ddbClient.retrieveUser(username);

                                    /** Validate password. */
                                    if ((userAuthentication.getPassword()).
                                            equals(HashCalculator
                                                    .getMD5Hash(password))) {
                                        ddbClient.deleteUser(username);
                                        sqsClient.deleteQueue(username);
                                        sharedPreferencesHelper
                                                .removeUsername();
                                    } else {
                                        userDeletedSuccessfully.setValue(false);
                                    }
                                } catch (AmazonClientException e) {
                                    connectionSuccessful.setValue(false);
                                }
                            }
                        };

                        deleteUserThread.start();
                        try {
                            /** Wait for the thread to finish. */
                            deleteUserThread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        /** Check whether the connection was successful or not.
                         *  If unsuccessful, then report connection problem
                         *  and return. */
                        if (!connectionSuccessful.isValue()) {
                            showToast(getResources().getString(R.string.
                                    connectionProblem),
                                    Toast.LENGTH_SHORT);
                            return;
                        }

                        /** Check if user deletion was successful.
                         * If successful, go back to MainActivity page. */
                        if (userDeletedSuccessfully.isValue()) {
                            showToast(
                                    getResources().getString(R.string.
                                            userDeletionSuccessful),
                                    Toast.LENGTH_SHORT);
                            Intent intent = new
                                    Intent(context, MainActivity.class);
                            startActivity(intent);
                        } else {
                            showToast(getResources().getString(R.string.
                                    wrongPassword),
                                    Toast.LENGTH_SHORT);
                        }
                    }
                });

        /** Handler for negative button click. */
        deleteAccountDialogBuilder
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog,
                                                final int which) {
                                dialog.cancel();
                            }
                        });

        deleteAccountDialogBuilder.show();
        return true;
    }

    /**
     *  This method is called when the user selects
     *  SearchPlayers option from the Menu.
     *  @return Boolean.
     */
    private boolean searchPlayers() {

        /** Holder object to store ArrayList<String> of users. */
        final Holder onlineUserList = new Holder(new ArrayList<String>());

        /** BooleanHolder object to indicate whether
         *  connection was successful or not.
         *  True means the connection was successful.
         */
        final BooleanHolder connectionSuccessful = new BooleanHolder(true);

        Thread searchPlayersThread = new Thread() {
            @Override
            public void run() {

                try {
                    onlineUserList.setValue(ddbClient.getAvailableUsersList(
                            sharedPreferencesHelper.getUsername()));
                } catch (AmazonClientException e) {
                    connectionSuccessful.setValue(false);
                }
            }
        };

        searchPlayersThread.start();
        try {
            /** Wait for the thread to finish. */
            searchPlayersThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        /** Check whether the connection was successful or not.
         *  If unsuccessful, then report connection problem
         *  and return. */
        if (!connectionSuccessful.isValue()) {
            showToast(getResources().getString(R.string.connectionProblem),
                    Toast.LENGTH_SHORT);
            return true;
        }

        showAvailableUsersDialog((ArrayList<String>) onlineUserList.getValue());

        return true;
    }

    /**
     *  This method builds a DialogBox showing the list of
     *  available users, and lets the user select one option (user)
     *  to start the game with.
     *  @param onlineUserList List<String>
     */
    private void showAvailableUsersDialog(final List<String> onlineUserList) {

        /** Array to store user names. */
        final String[] onlineUserArray = onlineUserList.toArray(new
                String[onlineUserList.size()]);

        /** Integer to store index of selected user in the above array.
         *  -1 indicates that no user is selected by default. */
        final Holder whichUser = new Holder(-1);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.availableUsers)
                /** -1 indicates that no option is selected by default. */
                .setSingleChoiceItems(onlineUserArray, -1,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog,
                                                final int which) {

                                whichUser.setValue(which);
                            }
                        })
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog,
                                        final int which) {

                        if ((Integer) whichUser.getValue() != -1) {

                            sendGameStartRequest(onlineUserArray[(Integer)
                                    whichUser.getValue()]);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog,
                                        final int which) {

                    }
                });

        builder.show();
    }

    /**
     *  This method sends request for a new game to
     *  the selected user.
     *  @param username The username of the selected user.
     */
    private void sendGameStartRequest(final String username) {

        /** BooleanHolder object to indicate whether
         *  connection was successful or not.
         *  True means the connection was successful.
         */
        final BooleanHolder connectionSuccessful = new BooleanHolder(true);

        Thread sendRequestThread = new Thread() {
            @Override
            public void run() {

                try {
                    /* "1" is a dummy variable. */
                    sqsClient.sendMessage(username, buildGameStartRequest(), 1);
                } catch (AmazonClientException e) {
                    connectionSuccessful.setValue(false);
                }
            }
        };

        sendRequestThread.start();
        try {
            /** Wait for the thread to finish. */
            sendRequestThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        /** Check whether the connection was successful or not.
         *  If unsuccessful, then report connection problem
         *  and return. */
        if (!connectionSuccessful.isValue()) {
            showToast(getResources().getString(R.string.connectionProblem),
                    Toast.LENGTH_SHORT);
        }

    }

    /**
     *  This method builds a JSON object for GameStartRequest.
     *  Format of the JSON object is as follows :-
     *  {"MESSAGE_TYPE" : "GameStartRequest" , "username" : USER_NAME}
     *  @return JSON object.
     */
    private JSONObject buildGameStartRequest() {

        try {
            JSONObject gameStartRequest = new JSONObject();

            gameStartRequest.put(JsonKey.MESSAGE_TYPE.getKey(),
                    JsonValue.GAME_START_REQUEST.getValue());
            gameStartRequest.put(JsonKey.USERNAME.getKey(),
                    sharedPreferencesHelper.getUsername());

            return gameStartRequest;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *  This method shows the received invitation sent by
     *  other user, so that the current user can select whether
     *  he wants to play with him/her or not.
     *  @param otherUsername Username of the user who has sent the
     *                  invitation.
     */
    public void showInvitationDialog(final String otherUsername) {

        String msg = String.format(getResources().
                getString(R.string.invitationReceivedText),
                otherUsername);

        /* Context variable to be used inside thread. */
        final Context context = this;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.invitationHeading)
                .setMessage(msg)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog,
                                                final int which) {

                                sendRequestResponse(otherUsername, true);

                                /* De-activate the SQS listener. */
                                sqsListener = null;

                                /*
                                 *  Building up the intent to be passed
                                 *  to the PreGame activity.
                                 *  ControllerUser determines whether the
                                 *  current user will control the coming
                                 *  game or not.
                                 */
                                Intent intent = new Intent(context,
                                        PreGame.class);
                                intent.putExtra(Constants.
                                        OTHER_USERNAME_KEY, otherUsername);
                                intent.putExtra(Constants.
                                        CONTROLLER_USER, true);
                                startActivity(intent);
                            }
                        })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog,
                                                final int which) {

                                sendRequestResponse(otherUsername, false);

                                /* User has declined the game request.
                                 * Start listening again for new request. */
                                startSqsListener();
                            }
                        })
                .setCancelable(false);

        builder.show();
    }

    /**
     *  This method builds a JSON object for GameStartResponse.
     *  Format of the JSON object is as follows :-
     *  {"MESSAGE_TYPE" : "GameStartResponse" , "username" : USER_NAME}
     *  @param response boolean response,
     *                  True, if the current user accepts the request.
     *                  False, otherwise.
     *  @return JSON object.
     */
    private JSONObject buildGameStartResponse(final boolean response) {

        try {
            JSONObject gameStartResponse = new JSONObject();

            gameStartResponse.put(JsonKey.MESSAGE_TYPE.getKey(),
                    JsonValue.GAME_START_RESPONSE.getValue());
            gameStartResponse.put(JsonKey.USERNAME.getKey(),
                    sharedPreferencesHelper.getUsername());
            gameStartResponse.put(JsonKey.RESPONSE.getKey(),
                    response);

            return gameStartResponse;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *  This method sends appropriate response to the
     *  GameStartRequest, as selected by the user.
     *  @param otherUsername Username of the user who has sent the
     *                  invitation.
     *  @param response boolean response,
     *                  True, if the current user accepts the request.
     *                  False, otherwise.
     */
    private void sendRequestResponse(final String otherUsername,
                                     final boolean response) {

        /** BooleanHolder object to indicate whether
         *  connection was successful or not.
         *  True means the connection was successful.
         */
        final BooleanHolder connectionSuccessful = new BooleanHolder(true);

        Thread sendRequestResponseThread = new Thread() {
            @Override
            public void run() {

                try {
                    sqsClient.sendMessage(otherUsername,
                            buildGameStartResponse(response));
                } catch (AmazonClientException e) {
                    connectionSuccessful.setValue(false);
                }
            }
        };

        sendRequestResponseThread.start();
        try {
            /** Wait for the thread to finish. */
            sendRequestResponseThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        /** Check whether the connection was successful or not.
         *  If unsuccessful, then report connection problem
         *  and return. */
        if (!connectionSuccessful.isValue()) {
            showToast(getResources().getString(R.string.connectionProblem),
                    Toast.LENGTH_SHORT);
        }
    }

    /**
     *  This method builds up an intent to go
     *  to the PreGame page.
     *  @param otherUsername String Username of the
     *                       other user.
     */
    public void gotoPreGame(final String otherUsername) {

        /* De-activate the SQS listener. */
        sqsListener = null;

        /*
         *  Building up the intent to be passed
         *  to the PreGame activity.
         *  ControllerUser determines whether the
         *  current user will control the coming
         *  game or not.
         */
        Intent intent = new Intent(this,
                PreGame.class);
        intent.putExtra(Constants.
                OTHER_USERNAME_KEY, otherUsername);
        intent.putExtra(Constants.
                CONTROLLER_USER, false);
        startActivity(intent);
    }

    /**
     *  This method creates a new SQS listener object
     *  and starts it. The older object is automatically
     *  destroyed.
     */
    public void startSqsListener() {

        sqsListener = new SQSListener(this, sqsClient,
                sharedPreferencesHelper.getUsername());
        sqsListener.run();
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
     *  App exits on pressing back button rather than going to MainActivity.
     */
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    /**
     *  Overriding opPause method.
     *  Make user online status false before exit.
     */
    @Override
    protected void onPause() {
        super.onPause();

        Intent intent = new Intent(this, ExitService.class);
        this.startService(intent);
    }

    /**
     *  Overriding onResume method.
     *  Make user online status true on resume.
     *  Also sets inGame attribute False for the user.
     *  This takes care of sign-in bypass (through initCheck in MainActivity)
     *  and manual sign-in also.
     */
    @Override
    protected void onResume() {
        super.onResume();

        new Thread() {
            @Override
            public void run() {

                ddbClient.setUserAvailability(
                        sharedPreferencesHelper.getUsername(), true, false);
            }
        }.start();
    }
}
