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
import com.example.ujjwal.pokemoncardssample.services.ExitService;
import com.example.ujjwal.pokemoncardssample.utils.BooleanHolder;
import com.example.ujjwal.pokemoncardssample.utils.HashCalculator;
import com.example.ujjwal.pokemoncardssample.utils.Holder;

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
                    ddbClient.setUserOnlineAvailability(
                            sharedPreferencesHelper.getUsername(),
                            false);
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
            Toast.makeText(this, R.string.connectionProblem,
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        Toast.makeText(this, R.string.signOutSuccessful,
                Toast.LENGTH_SHORT)
                .show();
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

        String message = "Are you sure you want to delete user "
                + sharedPreferencesHelper.getUsername()
                + " ? The account will be deleted permanently "
                + "from our databases."
                + " Enter the password of user to continue.";
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
                                        sharedPreferencesHelper
                                                .removeUsername();
                                        ddbClient.deleteUser(username);
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
                            Toast.makeText(context, R.string.connectionProblem,
                                    Toast.LENGTH_SHORT)
                                    .show();
                            return;
                        }

                        /** Check if user deletion was successful.
                         * If successful, go back to MainActivity page. */
                        if (userDeletedSuccessfully.isValue()) {
                            Toast.makeText(
                                    context, R.string.userDeletionSuccessful,
                                    Toast.LENGTH_SHORT)
                                    .show();
                            Intent intent = new
                                    Intent(context, MainActivity.class);
                            startActivity(intent);
                        } else {
                            Toast.makeText(context, R.string.wrongPassword,
                                    Toast.LENGTH_SHORT).show();
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
                    onlineUserList.setValue(ddbClient.getOnlineUserList(
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
            Toast.makeText(this, R.string.connectionProblem, Toast.LENGTH_SHORT)
                    .show();
            return true;
        }

        showOnlineUsersDialog((ArrayList<String>) onlineUserList.getValue());

        return true;
    }

    /**
     *  This method builds a DialogBox showing the list of
     *  online users, and lets the user select one option (user)
     *  to start the game with.
     *  @param onlineUserList List<String>
     */
    private void showOnlineUsersDialog(final List<String> onlineUserList) {

        /** Array to store user names. */
        String[] onlineUserArray = onlineUserList.toArray(new
                String[onlineUserList.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.onlineUsers)
                /** -1 indicates that no option is selected by default. */
                .setSingleChoiceItems(onlineUserArray, -1,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog,
                                                final int which) {

                            }
                        })
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog,
                                        final int which) {

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
     *  This takes care of sign-in bypass (through initCheck in MainActivity)
     *  and manual sign-in also.
     */
    @Override
    protected void onResume() {
        super.onResume();

        new Thread() {
            @Override
            public void run() {

                ddbClient.setUserOnlineAvailability(
                        sharedPreferencesHelper.getUsername(), true);
            }
        }.start();
    }
}
