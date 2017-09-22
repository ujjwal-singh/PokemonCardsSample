package com.example.ujjwal.pokemoncardssample;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.AmazonClientException;
import com.example.ujjwal.pokemoncardssample.dao.SharedPreferencesHelper;
import com.example.ujjwal.pokemoncardssample.dao.dynamodb.DDBClient;
import com.example.ujjwal.pokemoncardssample.dao.sqs.SQSClient;
import com.example.ujjwal.pokemoncardssample.dao.sqs.SQSListener;
import com.example.ujjwal.pokemoncardssample.pokemon.Pokemon;
import com.example.ujjwal.pokemoncardssample.services.ExitService;
import com.example.ujjwal.pokemoncardssample.utils.BooleanHolder;
import com.example.ujjwal.pokemoncardssample.utils.JsonKey;
import com.example.ujjwal.pokemoncardssample.utils.JsonValue;
import com.example.ujjwal.pokemoncardssample.utils.PokemonComparator;
import com.example.ujjwal.pokemoncardssample.utils.RandomSequence;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

/**
 *  This activity handles the main game page,
 *  i.e., after the pre-game page.
 *  @author ujjwal
 */
public class GamePage extends AppCompatActivity {

    /** Reference to the only object of the
     * SharedPreferencesHelper singleton class. */
    private SharedPreferencesHelper sharedPreferencesHelper;

    /** Reference to the only object of the DDBClient singleton class. */
    private DDBClient ddbClient;

    /** Reference to the only object of the SQSClient singleton class. */
    private SQSClient sqsClient;

    /** SQSListener object for the user queue. */
    private SQSListener sqsListener;

    /** Boolean variable to store whether the current user
     *  is the controller user or not. */
    private boolean controllerUser;

    /** Username of the other user. */
    private String otherUsername;

    /** Stores the number of cards distributed to either
     *  player at the start of the game. */
    private int numberOfCards;

    /** ArrayList to store the current user's Pokemons. */
    private ArrayList<Pokemon> myPokemons;

    /** Boolean to store whether its current user's turn or not. */
    private boolean myTurn;

    /** TextView object to reference to my Pokemon's name text view. */
    private TextView myPokemonNameTextView;

    /** ImageView object to reference to my Pokemon's image view. */
    private ImageView myPokemonImageView;

    /** Button object to reference to my Pokemon's number attribute
     *  button. */
    private Button myPokemonNumberButton;

    /** Button object to reference to my Pokemon's height attribute
     *  button. */
    private Button myPokemonHeightButton;

    /** Button object to reference to my Pokemon's weight attribute
     *  button. */
    private Button myPokemonWeightButton;

    /** Button object to reference to my Pokemon's type attribute
     *  button. */
    private Button myPokemonTypeButton;

    /** TextView object to reference to other Pokemon's name text view. */
    private TextView otherPokemonNameTextView;

    /** ImageView object to reference to other Pokemon's image view. */
    private ImageView otherPokemonImageView;

    /** Button object to reference to other Pokemon's number attribute
     *  button. */
    private Button otherPokemonNumberButton;

    /** Button object to reference to other Pokemon's height attribute
     *  button. */
    private Button otherPokemonHeightButton;

    /** Button object to reference to other Pokemon's weight attribute
     *  button. */
    private Button otherPokemonWeightButton;

    /** Button object to reference to other Pokemon's type attribute
     *  button. */
    private Button otherPokemonTypeButton;

    /** TextView to display the number of remaining cards. */
    private TextView cardsRemainingTextView;

    /**
     *  Overriding onCreate method.
     *  @param savedInstanceState Bundle savedInstanceState
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_page);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        sharedPreferencesHelper = SharedPreferencesHelper.getInstance();
        ddbClient = DDBClient.getInstance();
        sqsClient = SQSClient.getInstance();

        controllerUser = getIntent().getExtras().getBoolean(Constants.
                CONTROLLER_USER);
        otherUsername = getIntent().getExtras().getString(Constants.
                OTHER_USERNAME_KEY);
        ArrayList<Integer> pokemonIds = getIntent().getExtras().
                getIntegerArrayList(Constants.POKEMON_ID_LIST_KEY);

        numberOfCards = pokemonIds.size();

        myPokemonNameTextView = (TextView) this.findViewById(
                R.id.myPokemonNameTextView);
        myPokemonImageView = (ImageView) this.findViewById(
                R.id.myPokemonImageView);
        myPokemonNumberButton = (Button) this.findViewById(
                R.id.myPokemonNumberButton);
        myPokemonHeightButton = (Button) this.findViewById(
                R.id.myPokemonHeightButton);
        myPokemonWeightButton = (Button) this.findViewById(
                R.id.myPokemonWeightButton);
        myPokemonTypeButton = (Button) this.findViewById(
                R.id.myPokemonTypeButton);

        otherPokemonNameTextView = (TextView) this.findViewById(
                R.id.otherPokemonNameTextView);
        otherPokemonImageView = (ImageView) this.findViewById(
                R.id.otherPokemonImageView);
        otherPokemonNumberButton = (Button) this.findViewById(
                R.id.otherPokemonNumberButton);
        otherPokemonHeightButton = (Button) this.findViewById(
                R.id.otherPokemonHeightButton);
        otherPokemonWeightButton = (Button) this.findViewById(
                R.id.otherPokemonWeightButton);
        otherPokemonTypeButton = (Button) this.findViewById(
                R.id.otherPokemonTypeButton);

        cardsRemainingTextView = (TextView) this.findViewById(
                R.id.cardsRemainingTextView);

        /* Shuffling the cards. */
        Collections.shuffle(pokemonIds);

        buildMyPokemons(pokemonIds);

        if (controllerUser) {
            handleControllerUser();
        } else {
            handleNonControllerUser();
        }
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

        /** Hiding unnecessary buttons for Game page. */
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
     *  This method handles the game for the Controller
     *  user.
     */
    private void handleControllerUser() {

        sendTossMessage();
    }

    /**
     *  This method handles the game for the non controller
     *  user.
     */
    private void handleNonControllerUser() {

        startSqsListener();
    }

    /**
     *  This method builds up a JSON message for
     *  sending toss info to the non controller user
     *  by the controller user.
     *  @return JSON object.
     */
    private JSONObject buildTossMessage() {

        JSONObject tossMessage = new JSONObject();

        int tossDecision = RandomSequence.
                generateRandomSequence(1, 2, 1).get(0);

        if (tossDecision == 1) {
            myTurn = true;
        } else {
            myTurn = false;
        }

        try {
            tossMessage.put(JsonKey.MESSAGE_TYPE.getKey(),
                    JsonValue.TOSS_DECISION_MESSAGE.getValue());
            tossMessage.put(JsonKey.USERNAME.getKey(),
                    sharedPreferencesHelper.getUsername());
            tossMessage.put(JsonKey.TOSS_DECISION.getKey(),
                    !myTurn);

            return tossMessage;
        } catch (JSONException e) {

            e.printStackTrace();
            return null;
        }
    }

    /**
     *  This method sends the toss message to the
     *  non controller user.
     */
    private void sendTossMessage() {

        /** BooleanHolder object to indicate whether
         *  connection was successful or not.
         *  True means the connection was successful.
         */
        final BooleanHolder connectionSuccessful = new BooleanHolder(true);

        Thread sendTossMessageThread = new Thread() {
            @Override
            public void run() {

                try {
                    sqsClient.sendMessage(otherUsername,
                            buildTossMessage());
                } catch (AmazonClientException e) {
                    connectionSuccessful.setValue(false);
                }
            }
        };

        sendTossMessageThread.start();
        try {
            /** Wait for the thread to finish. */
            sendTossMessageThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        /** Check whether the connection was successful or not.
         *  If unsuccessful, then report connection problem
         *  and return. */
        if (!connectionSuccessful.isValue()) {
            Toast.makeText(this, R.string.connectionProblem, Toast.LENGTH_SHORT)
                    .show();

            /* Connection unsuccessful. Re-try. */
            sendTossMessage();
        }

        showMyPokemon(myPokemons.get(0));
        removeOtherPokemon();
        displayNumberOfCardsRemaining(numberOfCards);

        if (myTurn) {
            Toast.makeText(this, R.string.tossWon,
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.tossLost,
                    Toast.LENGTH_SHORT).show();
            handleOpponentTurn();
        }
    }

    /**
     *  This method builds a JSON message, corresponding
     *  to the move played by the current user in-charge.
     *  It returns the message as a JsonObject.
     *  @param pokemonId    The ID of the pokemon whose
     *                      card has been played.
     *  @param pokemonAttribute String, the attribute of
     *                          the Pokemon which has been
     *                          chosen for play.
     *                          e.g., number
     *                                height, etc.
     *  @return JsonObject
     */
    private JSONObject buildPokemonMoveMessage(final int pokemonId,
                                               final String pokemonAttribute) {

        JSONObject pokemonMove = new JSONObject();

        try {

            pokemonMove.put(JsonKey.MESSAGE_TYPE.getKey(),
                    JsonValue.POKEMON_MOVE_MESSAGE.getValue());
            pokemonMove.put(JsonKey.USERNAME.getKey(),
                    sharedPreferencesHelper.getUsername());
            pokemonMove.put(JsonKey.POKEMON_NUMBER.getKey(),
                    pokemonId);
            pokemonMove.put(JsonKey.POKEMON_ATTRIBUTE.getKey(),
                    pokemonAttribute);

            return pokemonMove;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *  This method sends a Pokemon Move played by the
     *  user in-charge to the other user.
     *  @param pokemonId    The ID of the pokemon whose
     *                      card has been played.
     *  @param pokemonAttribute String, the attribute of
     *                          the Pokemon which has been
     *                          chosen for play.
     *                          e.g., number
     *                                height, etc.
     */
    private void sendPokemonMoveMessage(final int pokemonId,
                                        final String pokemonAttribute) {

        /** BooleanHolder object to indicate whether
         *  connection was successful or not.
         *  True means the connection was successful.
         */
        final BooleanHolder connectionSuccessful = new BooleanHolder(true);

        Thread sendPokemonMoveMessageThread = new Thread() {
            @Override
            public void run() {

                try {
                    sqsClient.sendMessage(otherUsername,
                            buildPokemonMoveMessage(pokemonId,
                                    pokemonAttribute));
                } catch (AmazonClientException e) {
                    connectionSuccessful.setValue(false);
                }
            }
        };

        sendPokemonMoveMessageThread.start();
        try {
            /** Wait for the thread to finish. */
            sendPokemonMoveMessageThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        /** Check whether the connection was successful or not.
         *  If unsuccessful, then report connection problem
         *  and return. */
        if (!connectionSuccessful.isValue()) {
            Toast.makeText(this, R.string.connectionProblem, Toast.LENGTH_SHORT)
                    .show();

            /* Connection unsuccessful. Re-try. */
            sendPokemonMoveMessage(pokemonId, pokemonAttribute);
        }
    }

    /**
     *  This method builds a JSON message, corresponding
     *  to the response of the non in-charge user to the
     *  in-charge user's move.
     *  It returns the message as a JsonObject.
     *  @param pokemonId    The ID of the pokemon
     *  @param pokemonAttribute String, the attribute of
     *                          the Pokemon which has been
     *                          chosen for play.
     *                          e.g., number
     *                                height, etc.
     *  @return JsonObject
     */
    private JSONObject buildPokemonMoveResponseMessage(final int pokemonId,
                                               final String pokemonAttribute) {

        JSONObject pokemonMove = new JSONObject();

        try {

            pokemonMove.put(JsonKey.MESSAGE_TYPE.getKey(),
                    JsonValue.POKEMON_MOVE_RESPONSE_MESSAGE.getValue());
            pokemonMove.put(JsonKey.USERNAME.getKey(),
                    sharedPreferencesHelper.getUsername());
            pokemonMove.put(JsonKey.POKEMON_NUMBER.getKey(),
                    pokemonId);
            pokemonMove.put(JsonKey.POKEMON_ATTRIBUTE.getKey(),
                    pokemonAttribute);

            return pokemonMove;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *  This method sends the response of the non in-charge
     *  user to the in-charge user.
     *  @param pokemonId    The ID of the pokemon
     *  @param pokemonAttribute String, the attribute of
     *                          the Pokemon which has been
     *                          chosen for play.
     *                          e.g., number
     *                                height, etc.
     */
    private void sendPokemonMoveResponseMessage(final int pokemonId,
                                        final String pokemonAttribute) {

        /** BooleanHolder object to indicate whether
         *  connection was successful or not.
         *  True means the connection was successful.
         */
        final BooleanHolder connectionSuccessful = new BooleanHolder(true);

        Thread sendPokemonMoveResponseMessageThread = new Thread() {
            @Override
            public void run() {

                try {
                    sqsClient.sendMessage(otherUsername,
                            buildPokemonMoveResponseMessage(pokemonId,
                                    pokemonAttribute));
                } catch (AmazonClientException e) {
                    connectionSuccessful.setValue(false);
                }
            }
        };

        sendPokemonMoveResponseMessageThread.start();
        try {
            /** Wait for the thread to finish. */
            sendPokemonMoveResponseMessageThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        /** Check whether the connection was successful or not.
         *  If unsuccessful, then report connection problem
         *  and return. */
        if (!connectionSuccessful.isValue()) {
            Toast.makeText(this, R.string.connectionProblem, Toast.LENGTH_SHORT)
                    .show();

            /* Connection unsuccessful. Re-try. */
            sendPokemonMoveResponseMessage(pokemonId, pokemonAttribute);
        }
    }

    /**
     *  This method shows the toss decision to the non
     *  controller user.
     *  @param senderUser   String username of the sender.
     *  @param decision Boolean True, if the current user
     *                  won the toss, else False.
     */
    public void showNonControllerUserTossDecision(final String senderUser,
                                                   final boolean decision) {

        if (!senderUser.equals(otherUsername)) {
            startSqsListener();
            return;
        }

        myTurn = decision;

        showMyPokemon(myPokemons.get(0));
        removeOtherPokemon();
        displayNumberOfCardsRemaining(numberOfCards);

        /* Context variable to be used inside the below thread. */
        final Context context = this;

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (myTurn) {
                    Toast.makeText(context, R.string.tossWon,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, R.string.tossLost,
                            Toast.LENGTH_SHORT).show();
                    handleOpponentTurn();
                }
            }
        });
    }

    /**
     *  This method handles the case when the current turn
     *  is of the other user.
     */
    private void handleOpponentTurn() {

        startSqsListener();
    }

    /**
     *  This method handles the case when the current
     *  turn is of the user.
     *  @param attribute  String attribute, it defines
     *                    which attribute of the current
     *                    Pokemon has been selected.
     *                    e.g., Number, Height, Weight.
     */
    private void handleMyTurn(final String attribute) {

        setMyButtons(false);

        int currentPokemonID = myPokemons.get(0).getNumber();
        sendPokemonMoveMessage(currentPokemonID, attribute);

        startSqsListener();
    }

    /**
     *  This method shows the non user in-charge's card
     *  to the user in-charge of the current turn.
     *  The current user is the user in-charge of the current
     *  turn.
     *  @param senderUser   String username of the sender user.
     *  @param otherPokemonId   Pokemon ID of the Pokemon whose
     *                          card has been played by the other
     *                          user.
     *  @param pokemonAttribute String, the attribute of
     *                          the Pokemon which has been
     *                          chosen for play.
     *                          Of course, this attribute has already
     *                          been decided by the current user
     *                          while playing his/her turn.
     *                          e.g., number
     *                                height, etc.
     */
    public void handleMoveResponse(final String senderUser,
                                 final int otherPokemonId,
                                 final String pokemonAttribute) {

        if (!senderUser.equals(otherUsername)) {

            startSqsListener();
            return;
        }

        Pokemon otherPokemon = new Pokemon(this, otherPokemonId);

        showOtherPokemon(otherPokemon);

        changeOtherPokemonButtonColorToGreen(pokemonAttribute);

        Pokemon myCurrentPokemon = myPokemons.get(0);

        try {
            Thread.sleep(Constants.POKEMON_CARD_DISPLAY_WAIT_TIME_IN_GAME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        checkAndHandleMoveResult(myCurrentPokemon, otherPokemon,
                pokemonAttribute);
    }

    /**
     *  This method shows the user in-charge's move to the
     *  other user, i.e., the other user is the user in-charge
     *  and current user is the one who will receive his/her
     *  move.
     *  It then sends the Pokemon ID and attribute of the current
     *  user to the user in-charge of the move.
     *  @param senderUser   String username of the sender user.
     *  @param otherPokemonId    int PokemonID of the pokemon whose
     *                      card has been played.
     *  @param pokemonAttribute String, the attribute of
     *                          the Pokemon which has been
     *                          chosen for play.
     *                          e.g., number
     *                                height, etc.
     */
    public void handleOpponentMove(final String senderUser,
                               final int otherPokemonId,
                               final String pokemonAttribute) {

        if (!senderUser.equals(otherUsername)) {

            startSqsListener();
            return;
        }

        Pokemon otherPokemon = new Pokemon(this, otherPokemonId);

        showOtherPokemon(otherPokemon);

        changeMyPokemonButtonColorToGreen(pokemonAttribute);
        changeOtherPokemonButtonColorToGreen(pokemonAttribute);

        Pokemon myCurrentPokemon = myPokemons.get(0);

        sendPokemonMoveResponseMessage(myCurrentPokemon.getNumber(),
                pokemonAttribute);

        try {
            Thread.sleep(Constants.POKEMON_CARD_DISPLAY_WAIT_TIME_IN_GAME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        checkAndHandleMoveResult(myCurrentPokemon, otherPokemon,
                pokemonAttribute);
    }

    /**
     *  This method checks the result of the current move,
     *  and then displays the appropriate win/lose message
     *  to the user. It also updates the Pokemon List of the
     *  user.
     *  @param myPokemon    The Pokemon of the current user.
     *  @param otherPokemon The Pokemon of the other user.
     *  @param pokemonAttribute     String, the attribute of
     *                              the Pokemons to be compared.
     *                              e.g., number
     *                              height, etc.
     */
    private void checkAndHandleMoveResult(final Pokemon myPokemon,
                                 final Pokemon otherPokemon,
                                 final String pokemonAttribute) {

        myTurn = PokemonComparator.comparePokemon(myPokemon,
                otherPokemon, pokemonAttribute, myTurn);

        if (myTurn) {

            myPokemons.add(myPokemon);
            myPokemons.add(otherPokemon);
            myPokemons.remove(0);
        } else {

            myPokemons.remove(0);
        }

        final Context context = this;

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (myTurn) {

                    Toast.makeText(context, R.string.roundWon,
                            Toast.LENGTH_SHORT).show();
                } else {

                    Toast.makeText(context, R.string.roundLost,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (myPokemons.size() == 0) {

            displayNumberOfCardsRemaining(myPokemons.size());

            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    Toast.makeText(context, R.string.gameLost,
                            Toast.LENGTH_LONG).show();
                }
            });

            updateUserHistory(false);

            Intent intent = new Intent(this, HomePage.class);
            startActivity(intent);

        } else if (myPokemons.size() == 2 * numberOfCards) {

            displayNumberOfCardsRemaining(myPokemons.size());

            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    Toast.makeText(context, R.string.gameWon,
                            Toast.LENGTH_LONG).show();
                }
            });

            updateUserHistory(true);

            Intent intent = new Intent(this, HomePage.class);
            startActivity(intent);

        } else {

            showMyPokemon(myPokemons.get(0));
            removeOtherPokemon();
            displayNumberOfCardsRemaining(myPokemons.size());

            if (!myTurn) {

                handleOpponentTurn();
            }
        }
    }

    /**
     *  This method updates the user history DDB table upon
     *  the completion of the game.
     *  @param gameResult   Boolean,    True if the current user
     *                                  won the game.
     *                                  False, otherwise.
     */
    private void updateUserHistory(final boolean gameResult) {

        /** BooleanHolder object to indicate whether
         *  connection was successful or not.
         *  True means the connection was successful.
         */
        final BooleanHolder connectionSuccessful = new BooleanHolder(true);

        Thread updateUserHistoryThread = new Thread() {
            @Override
            public void run() {

                try {

                    ddbClient.updateUserHistory(sharedPreferencesHelper.
                            getUsername(), gameResult);
                } catch (AmazonClientException e) {
                    connectionSuccessful.setValue(false);
                }
            }
        };

        updateUserHistoryThread.start();
        try {
            /** Wait for the thread to finish. */
            updateUserHistoryThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        /** Check whether the connection was successful or not.
         *  If unsuccessful, then report connection problem
         *  and return. */
        if (!connectionSuccessful.isValue()) {
            Toast.makeText(this, R.string.connectionProblem, Toast.LENGTH_SHORT)
                    .show();

            /* Connection unsuccessful. Re-try. */
            updateUserHistory(gameResult);
        }
    }

    /**
     *  This method sets the number of cards remaining
     *  text view.
     *  @param cardsRemaining   Number of cards remaining.
     */
    private void displayNumberOfCardsRemaining(final int cardsRemaining) {

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                cardsRemainingTextView.setText(String.format(getResources().
                        getString(R.string.cardsRemaining),
                        String.valueOf(cardsRemaining)));
            }
        });
    }

    /**
     *  This method changes the colour of a particular button
     *  corresponding to current user's Pokemon to green.
     *  @param pokemonAttribute String, the attribute of
     *                          the Pokemon which has been
     *                          chosen for play.
     *                          e.g., number
     *                                height, etc.
     */
    private void changeMyPokemonButtonColorToGreen(
            final String pokemonAttribute) {

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (pokemonAttribute.equals(JsonValue.POKEMON_NUMBER.
                        getValue())) {

                    myPokemonNumberButton.setBackgroundColor(getResources().
                            getColor(R.color.holoGreenLight));
                } else if (pokemonAttribute.equals(JsonValue.POKEMON_HEIGHT.
                        getValue())) {

                    myPokemonHeightButton.setBackgroundColor(getResources().
                            getColor(R.color.holoGreenLight));
                } else if (pokemonAttribute.equals(JsonValue.POKEMON_WEIGHT.
                        getValue())) {

                    myPokemonWeightButton.setBackgroundColor(getResources().
                            getColor(R.color.holoGreenLight));
                } else if (pokemonAttribute.equals(JsonValue.POKEMON_TYPE.
                        getValue())) {

                    myPokemonTypeButton.setBackgroundColor(getResources().
                            getColor(R.color.holoGreenLight));
                }
            }
        });
    }

    /**
     *  This method changes the colour of a particular button
     *  corresponding to other user's Pokemon to green.
     *  @param pokemonAttribute String, the attribute of
     *                          the Pokemon which has been
     *                          chosen for play.
     *                          e.g., number
     *                                height, etc.
     */
    private void changeOtherPokemonButtonColorToGreen(
            final String pokemonAttribute) {

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (pokemonAttribute.equals(JsonValue.POKEMON_NUMBER.
                        getValue())) {

                    otherPokemonNumberButton.setBackgroundColor(getResources().
                            getColor(R.color.holoGreenLight));
                } else if (pokemonAttribute.equals(JsonValue.POKEMON_HEIGHT.
                        getValue())) {

                    otherPokemonHeightButton.setBackgroundColor(getResources().
                            getColor(R.color.holoGreenLight));
                } else if (pokemonAttribute.equals(JsonValue.POKEMON_WEIGHT.
                        getValue())) {

                    otherPokemonWeightButton.setBackgroundColor(getResources().
                            getColor(R.color.holoGreenLight));
                } else if (pokemonAttribute.equals(JsonValue.POKEMON_TYPE.
                        getValue())) {

                    otherPokemonTypeButton.setBackgroundColor(getResources().
                            getColor(R.color.holoGreenLight));
                }
            }
        });
    }

    /**
     *  This method removes the Pokemon display of
     *  the other user.
     */
    private void removeOtherPokemon() {

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                otherPokemonNameTextView.setText(null);
                otherPokemonImageView.setImageDrawable(null);
                otherPokemonNumberButton.setText(null);
                otherPokemonHeightButton.setText(null);
                otherPokemonWeightButton.setText(null);
                otherPokemonTypeButton.setText(null);

                /* Deprecated method used to support lower API levels. */
                otherPokemonNumberButton.setBackgroundColor(getResources().
                        getColor(R.color.holoBlueLight));
                otherPokemonHeightButton.setBackgroundColor(getResources().
                        getColor(R.color.holoBlueLight));
                otherPokemonWeightButton.setBackgroundColor(getResources().
                        getColor(R.color.holoBlueLight));
                otherPokemonTypeButton.setBackgroundColor(getResources().
                        getColor(R.color.holoBlueLight));
            }
        });
    }

    /**
     *  This method sets the enabled/disabled status
     *  of the current user's Pokemon's attribute buttons.
     *
     *  @param enabled  boolean, If true, then the buttons
     *                  are enabled, else disabled.
     */
    private void setMyButtons(final boolean enabled) {

        myPokemonNumberButton.setEnabled(enabled);
        myPokemonHeightButton.setEnabled(enabled);
        myPokemonWeightButton.setEnabled(enabled);
        myPokemonTypeButton.setEnabled(enabled);
    }

    /**
     *  This method builds up the Pokemons of the player,
     *  based on their IDs.
     * @param pokemonIds    ArrayList of Pokemon IDs.
     */
    private void buildMyPokemons(final ArrayList<Integer> pokemonIds) {

        myPokemons = new ArrayList<>();

        for (int pokemonId : pokemonIds) {

            myPokemons.add(new Pokemon(this, pokemonId));
        }
    }

    /**
     *  This method displays a Pokemon of the current user.
     *  @param pokemon The Pokemon to be displayed.
     */
    private void showMyPokemon(final Pokemon pokemon) {

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                myPokemonNameTextView.setText(pokemon.getName());
                myPokemonImageView.setImageDrawable(pokemon.getImage());
                myPokemonNumberButton.setText(String.format(getResources().
                        getString(R.string.pokemonID), String.valueOf(pokemon.
                        getNumber())));
                myPokemonHeightButton.setText(String.format(getResources().
                        getString(R.string.pokemonHeight), String.valueOf(
                        pokemon.getHeight()), Constants.POKEMON_HEIGHT_UNIT));
                myPokemonWeightButton.setText(String.format(getResources().
                        getString(R.string.pokemonWeight), String.valueOf(
                        pokemon.getWeight()), Constants.POKEMON_WEIGHT_UNIT));
                myPokemonTypeButton.setText(pokemon.getTypes().toString());

                setMyButtons(true);

                /* Deprecated method used to support lower API levels. */
                myPokemonNumberButton.setBackgroundColor(getResources().
                        getColor(R.color.holoBlueLight));
                myPokemonHeightButton.setBackgroundColor(getResources().
                        getColor(R.color.holoBlueLight));
                myPokemonWeightButton.setBackgroundColor(getResources().
                        getColor(R.color.holoBlueLight));
                myPokemonTypeButton.setBackgroundColor(getResources().
                        getColor(R.color.holoBlueLight));
            }
        });
    }

    /**
     *  This method displays a Pokemon of the other user.
     *  @param pokemon The Pokemon to be displayed.
     */
    private void showOtherPokemon(final Pokemon pokemon) {

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                otherPokemonNameTextView.setText(pokemon.getName());
                otherPokemonImageView.setImageDrawable(pokemon.getImage());
                otherPokemonNumberButton.setText(String.format(getResources().
                        getString(R.string.pokemonID), String.valueOf(pokemon.
                        getNumber())));
                otherPokemonHeightButton.setText(String.format(getResources().
                        getString(R.string.pokemonHeight), String.valueOf(
                        pokemon.getHeight()), Constants.POKEMON_HEIGHT_UNIT));
                otherPokemonWeightButton.setText(String.format(getResources().
                        getString(R.string.pokemonWeight), String.valueOf(
                        pokemon.getWeight()), Constants.POKEMON_WEIGHT_UNIT));
                otherPokemonTypeButton.setText(pokemon.getTypes().toString());

                /* Deprecated method used to support lower API levels. */
                otherPokemonNumberButton.setBackgroundColor(getResources().
                        getColor(R.color.holoBlueLight));
                otherPokemonHeightButton.setBackgroundColor(getResources().
                        getColor(R.color.holoBlueLight));
                otherPokemonWeightButton.setBackgroundColor(getResources().
                        getColor(R.color.holoBlueLight));
                otherPokemonTypeButton.setBackgroundColor(getResources().
                        getColor(R.color.holoBlueLight));
            }
        });
    }

    /**
     *  This method is the handler for ID button.
     *  @param view View view.
     */
    public void handleNumber(final View view) {

        if (myTurn) {
        /* Using deprecated method to support lower API levels. */
            myPokemonNumberButton.setBackgroundColor(getResources().
                    getColor(R.color.holoGreenLight));
            handleMyTurn(JsonValue.POKEMON_NUMBER.getValue());
        }
    }

    /**
     *  This method is the handler for Height button.
     *  @param view View view.
     */
    public void handleHeight(final View view) {

        if (myTurn) {
        /* Using deprecated method to support lower API levels. */
            myPokemonHeightButton.setBackgroundColor(getResources().
                    getColor(R.color.holoGreenLight));
            handleMyTurn(JsonValue.POKEMON_HEIGHT.getValue());
        }
    }

    /**
     *  This method is the handler for Weight button.
     *  @param view View view.
     */
    public void handleWeight(final View view) {

        if (myTurn) {
        /* Using deprecated method to support lower API levels. */
            myPokemonWeightButton.setBackgroundColor(getResources().
                    getColor(R.color.holoGreenLight));
            handleMyTurn(JsonValue.POKEMON_WEIGHT.getValue());
        }
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
     *  Also sets inGame attribute True for the user.
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
                        sharedPreferencesHelper.getUsername(), true, true);
            }
        }.start();
    }
}