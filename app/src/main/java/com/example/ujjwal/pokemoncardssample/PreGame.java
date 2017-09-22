package com.example.ujjwal.pokemoncardssample;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.CountDownTimer;

import com.amazonaws.AmazonClientException;
import com.example.ujjwal.pokemoncardssample.dao.SharedPreferencesHelper;
import com.example.ujjwal.pokemoncardssample.dao.dynamodb.DDBClient;
import com.example.ujjwal.pokemoncardssample.dao.sqs.SQSClient;
import com.example.ujjwal.pokemoncardssample.dao.sqs.SQSListener;
import com.example.ujjwal.pokemoncardssample.pokemon.Pokemon;
import com.example.ujjwal.pokemoncardssample.services.ExitService;
import com.example.ujjwal.pokemoncardssample.utils.BooleanHolder;
import com.example.ujjwal.pokemoncardssample.utils.Holder;
import com.example.ujjwal.pokemoncardssample.utils.JsonKey;
import com.example.ujjwal.pokemoncardssample.utils.JsonValue;
import com.example.ujjwal.pokemoncardssample.utils.PokemonIDList;
import com.example.ujjwal.pokemoncardssample.utils.RandomSequence;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 *  This activity is the pre game activity.
 *  This activity determines the number of cards
 *  selected for the game to be held, as well as
 *  displays those cards.
 *
 *  @author ujjwal
 */
public class PreGame extends AppCompatActivity {

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

    /** ID and Name text view. */
    private TextView idAndNameTextView;

    /** Image View to display Pokemon's image. */
    private ImageView imageView;

    /** Height text view. */
    private TextView heightTextView;

    /** Weight text view. */
    private TextView weightTextView;

    /** Type text view. */
    private TextView typeTextView;

    /**
     *  Overriding onCreate method.
     *  @param savedInstanceState Bundle savedInstanceState
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pre_game);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        sharedPreferencesHelper = SharedPreferencesHelper.getInstance();
        ddbClient = DDBClient.getInstance();
        sqsClient = SQSClient.getInstance();

        controllerUser = getIntent().getExtras().getBoolean(Constants.
                CONTROLLER_USER);
        otherUsername = getIntent().getExtras().getString(Constants.
                OTHER_USERNAME_KEY);

        numberOfCards = 0;
        myPokemons = new ArrayList<>();

        idAndNameTextView = (TextView) this.findViewById(R.id.
                idAndNameTextView);
        imageView = (ImageView) this.findViewById(R.id.imageView);
        heightTextView = (TextView) this.findViewById(R.id.heightTextView);
        weightTextView = (TextView) this.findViewById(R.id.weightTextView);
        typeTextView = (TextView) this.findViewById(R.id.typeTextView);

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

        /** Hiding unnecessary buttons for Pre-Game page. */
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
     *  This method handles the game for the
     *  controller user.
     */
    private void handleControllerUser() {

        showNumberOfCardsDialog();
    }

    /**
     *  This method handles the game for the
     *  non controller user.
     */
    private void handleNonControllerUser() {

        startSqsListener();
    }

    /**
     *  This method builds up the dialog box
     *  for selecting the number of cards to
     *  be used in the game.
     */
    private void showNumberOfCardsDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.numberOfCardsDialog)
                .setSingleChoiceItems(Constants.NUMBER_OF_CARDS_OPTIONS, 0,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog,
                                                final int which) {
                                numberOfCards = Integer.parseInt(Constants.
                                        NUMBER_OF_CARDS_OPTIONS[which]);
                            }
                        })
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog,
                                        final int id) {

                        if (numberOfCards == 0) {
                            numberOfCards = Integer.parseInt(Constants.
                                    NUMBER_OF_CARDS_OPTIONS[0]);
                        }

                        setCards();
                    }
                })
                .setCancelable(false);

        builder.show();
    }

    /**
     *  This method selects the cards of the two players.
     *  A random sequence of length twice the number of
     *  cards per user is obtained. The first half of the
     *  sequence is assigned to the Controller player,
     *  while the second half is sent to the other player
     *  via a SQS message.
     */
    private void setCards() {

        ArrayList<Integer> pokemonIds = RandomSequence.
                generateRandomSequence(1, Constants.NUMBER_OF_POKEMONS,
                        2 * numberOfCards);

        for (int pokemonId : pokemonIds.subList(0, pokemonIds.size() / 2)) {

            myPokemons.add(new Pokemon(this, pokemonId));
        }

        sendPokemonCardsInitMessage(pokemonIds.subList(
                pokemonIds.size() / 2, pokemonIds.size()));

        showCards();
    }

    /**
     *  This method builds up the initial Pokemon Cards
     *  Message, sent by the Controller User to the other
     *  user. The message notifies the other user of his/her
     *  Pokemons.
     *  @param otherPokemonIds List of the Pokemon IDs of the
     *                         other user.
     *  @return JSONObject
     */
    private JSONObject buildPokemonCardsInitMessage(final List<Integer>
                                                        otherPokemonIds) {

        JSONArray otherPokemonIdsArray = new JSONArray(otherPokemonIds);

        JSONObject pokemonCardsMessage = new JSONObject();

        try {
            pokemonCardsMessage.put(JsonKey.MESSAGE_TYPE.getKey(),
                    JsonValue.POKEMON_CARDS_INIT_MESSAGE.getValue());
            pokemonCardsMessage.put(JsonKey.USERNAME.getKey(),
                    sharedPreferencesHelper.getUsername());
            pokemonCardsMessage.put(JsonKey.INIT_POKEMON_LIST.getKey(),
                    otherPokemonIdsArray);

            return pokemonCardsMessage;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *  This method sends the message built by
     *  BuildPokemonInitMessage method to the SQS
     *  queue of the other user.
     *  @param otherPokemonIds List of the Pokemon IDs of the other
     *                         user.
     */
    private void sendPokemonCardsInitMessage(final List<Integer>
                                                     otherPokemonIds) {

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
                            buildPokemonCardsInitMessage(otherPokemonIds));
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
            Toast.makeText(this, R.string.connectionProblem, Toast.LENGTH_SHORT)
                    .show();

            /* Connection unsuccessful. Re-try. */
            sendPokemonCardsInitMessage(otherPokemonIds);
        }
    }

    /**
     *  This method sets the Pokemons of the non controller
     *  user, after receiving their IDs from the controller
     *  user.
     *  Also, it first checks if the sender user is the one
     *  with whom the game is being played.
     *  This method is called by SQS listener.
     *  @param pokemonIds   List of Pokemon IDs.
     *  @param senderUser   String Username of the sender
     *                      user.
     */
    public void setNonControllerUserCards(final List<Integer> pokemonIds,
                                          final String senderUser) {

        if (!senderUser.equals(otherUsername)) {
            startSqsListener();
            return;
        }

        for (int pokemonId : pokemonIds) {

            myPokemons.add(new Pokemon(this, pokemonId));
        }

        numberOfCards = myPokemons.size();

        /* Destroy the SQS listener, as its job is finished. */
        sqsListener = null;

        showCards();
    }

    /**
     *  This method displays the cards of the
     *  user, one by one.
     */
    private void showCards() {

        this.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                /* Maintains index in Pokemon list. */
                final Holder pokemonIndexHolder = new Holder(0);

                new CountDownTimer(Constants.
                        CARD_DISPLAY_SLEEP_TIME * (numberOfCards + 1),
                        Constants.CARD_DISPLAY_SLEEP_TIME) {

                    @Override
                    public void onTick(final long milliSecUntilFinished) {

                        if ((int) pokemonIndexHolder.getValue()
                                < myPokemons.size()) {
                            Pokemon currentPokemon = myPokemons.get(
                                    (int) pokemonIndexHolder.getValue());
                            pokemonIndexHolder.setValue(
                                    (int) pokemonIndexHolder.getValue() + 1);

                            displayPokemon(currentPokemon);
                        }
                    }

                    @Override
                    public void onFinish() {

                        gotoGamePage();
                    }
                }.start();
            }
        });

    }

    /**
     *  This method displays a Pokemon.
     *  @param pokemon The Pokemon to be displayed.
     */
    private void displayPokemon(final Pokemon pokemon) {

        idAndNameTextView.setText(String.format(getResources().
                getString(R.string.pokemonIdAndName), String.
                valueOf(pokemon.getNumber()), pokemon.
                getName()));
        imageView.setImageDrawable(pokemon.getImage());
        heightTextView.setText(String.format(getResources().
                getString(R.string.pokemonHeight), String.
                valueOf(pokemon.getHeight()), Constants.
                POKEMON_HEIGHT_UNIT));
        weightTextView.setText(String.format(getResources().
                getString(R.string.pokemonWeight), String.
                valueOf(pokemon.getWeight()), Constants.
                POKEMON_WEIGHT_UNIT));
        typeTextView.setText(pokemon.getTypes().toString());
    }

    /**
     *  This method transfers the application to
     *  the Game Page.
     *  Basically, builds an appropriate intent
     *  and starts GamePage activity.
     */
    private void gotoGamePage() {

        /* De-activate the SQS listener. */
        sqsListener = null;

        /*
         *  Building up the intent to be passed
         *  to the Game activity.
         *  ControllerUser determines whether the
         *  current user will control the coming
         *  game or not.
         */
        Intent intent = new Intent(this,
                GamePage.class);
        intent.putExtra(Constants.
                OTHER_USERNAME_KEY, otherUsername);
        intent.putExtra(Constants.
                CONTROLLER_USER, controllerUser);
        intent.putExtra(Constants.POKEMON_ID_LIST_KEY,
                PokemonIDList.getIDList(myPokemons));
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
