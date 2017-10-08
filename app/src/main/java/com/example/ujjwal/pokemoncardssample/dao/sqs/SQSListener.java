package com.example.ujjwal.pokemoncardssample.dao.sqs;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.sqs.model.Message;
import com.example.ujjwal.pokemoncardssample.Constants;
import com.example.ujjwal.pokemoncardssample.GamePage;
import com.example.ujjwal.pokemoncardssample.HomePage;
import com.example.ujjwal.pokemoncardssample.PreGame;
import com.example.ujjwal.pokemoncardssample.R;
import com.example.ujjwal.pokemoncardssample.dao.SharedPreferencesHelper;
import com.example.ujjwal.pokemoncardssample.dao.dynamodb.DDBClient;
import com.example.ujjwal.pokemoncardssample.utils.JsonKey;
import com.example.ujjwal.pokemoncardssample.utils.JsonValue;
import com.example.ujjwal.pokemoncardssample.utils.UTCTime;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 *  This class provides a listener object for
 *  SQS queue.
 *  @author ujjwal
 */
public class SQSListener {

    /** Context, passed by the calling activity. */
    private Context context;

    /** Reference to the only object of the DDBClient singleton class. */
    private DDBClient ddbClient;

    /** SQSClient object. */
    private SQSClient sqsClient;

    /** Name of the queue for which the listener is set up. */
    private String queueName;

    /** URL of the queue for which the listener is set up. */
    private String queueUrl = null;

    /** Reference to the only object of the
     * SharedPreferencesHelper singleton class. */
    private SharedPreferencesHelper sharedPreferencesHelper;

    /** Time of the previous last seen update. */
    private static long previousLastSeenUpdateTime = 0L;

    /** Boolean variable to indicate whether further message polling
     *  is required or not. */
    private boolean pollFurther;

    /**
     *  Constructor for the class.
     *  @param passedContext    Context passed by the calling activity.
     *  @param passedSqsClient  SQSClient object.
     *  @param passedQueueName  Queue name for which the listener is set up.
     *  @param passedDdbClient  DDB Client object.
     */
    public SQSListener(final Context passedContext,
                       final SQSClient passedSqsClient,
                       final String passedQueueName,
                       final DDBClient passedDdbClient) {

        this.context = passedContext;
        this.sqsClient = passedSqsClient;
        this.queueName = passedQueueName;
        this.ddbClient = passedDdbClient;
        this.sharedPreferencesHelper = SharedPreferencesHelper.getInstance();
        this.pollFurther = true;
    }

    /**
     *  This method starts a thread which listens for messages
     *  on the SQS queue.
     */
    public void run() {

        new Thread() {
            @Override
            public void run() {

                /* Getting previous number of messages in the queue. */
                try {

                    if (queueUrl == null) {

                        queueUrl = sqsClient.getQueueUrl(queueName);
                    }

                /* Listen for messages on the queue. */
                    while (pollFurther) {

                        List<Message> messages = sqsClient.
                                getMessages(queueUrl);

                        long sentTimeStamp = 0L;
                        long receivedTimeStamp = 0L;

                        /* Update last seen of the current user,
                         * if user is at the Home Page. */
                        if (context instanceof HomePage) {

                            updateLastSeen();
                        }

                    /* Message is received.
                    *  Process it and then delete it. */
                        if (messages.size() > 0) {

                            Message receivedMessage = messages.get(0);

                            sentTimeStamp = Long.parseLong(receivedMessage.
                                    getAttributes().get(Constants.
                                    SQS_SENT_TIMESTAMP_KEY));
                            receivedTimeStamp = Long.parseLong(receivedMessage.
                                    getAttributes().get(Constants.
                                    SQS_FIRST_RECEIVED_TIMESTAMP_KEY));

                            sqsClient.deleteMessage(queueUrl,
                                    receivedMessage.getReceiptHandle());

                            /* Checking freshness of message. */
                            if (receivedTimeStamp - sentTimeStamp < Constants.
                                    SQS_FRESHNESS_INTERVAL) {
                                String messageBody = receivedMessage.getBody();
                                processMessage(messageBody);

                                break;
                            }
                        } else {
                            try {
                                Thread.sleep(Constants.
                                        LISTENER_THREAD_SLEEP_MILLI_SEC);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (AmazonClientException e) {

                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (context instanceof HomePage) {

                                ((HomePage) context).showToast(context.
                                                getResources().
                                                getString(R.string.
                                                        connectionProblem),
                                        Toast.LENGTH_SHORT);
                            } else if (context instanceof PreGame) {

                                ((PreGame) context).showToast(context.
                                                getResources().
                                                getString(R.string.
                                                        connectionProblem),
                                        Toast.LENGTH_SHORT);
                            } else if (context instanceof GamePage) {

                                ((GamePage) context).showToast(context.
                                                getResources().
                                                getString(R.string.
                                                        connectionProblem),
                                        Toast.LENGTH_SHORT);
                            }
                        }
                    });

                    /* Re-call run() as some Network
                     * Problem was encountered before. */
                    run();
                }
            }
        }.start();
    }

    /**
     *  This method processes the received message.
     *  @param message  String message, in JSON format.
     */
    private void processMessage(final String message) {

        try {
            final JSONObject msg = new JSONObject(message);

            String messageType = msg.getString(JsonKey.MESSAGE_TYPE.getKey());

            /*
             *  Message is for Game Start Request.
             */
            if (messageType.equals(JsonValue.GAME_START_REQUEST.getValue())
                    && context instanceof HomePage) {

                handleGameStartRequest(msg);

            } else if (messageType.equals(JsonValue.
                    GAME_START_RESPONSE.getValue())
                    && context instanceof HomePage) {

                /*
                 *  Message is for Game Start Response.
                 */

                handleGameStartResponse(msg);

            } else if (messageType.equals(JsonValue.
                    POKEMON_CARDS_INIT_MESSAGE.getValue())
                    && context instanceof PreGame) {

                /*
                 *  Message is for telling about the initial
                 *  Pokemon IDs for the non-controller user.
                 */

                handlePokemonCardsInitMessage(msg);

            } else if (messageType.equals(JsonValue.
                    TOSS_DECISION_MESSAGE.getValue())
                    && context instanceof GamePage) {

                /*
                 *  Message is for conveying the toss decision
                 *  to the non controller user.
                 */

                handleTossDecisionMessage(msg);

            } else if (messageType.equals(JsonValue.
                    POKEMON_MOVE_MESSAGE.getValue())
                    && context instanceof GamePage) {

                /*
                 *  Message is for conveying the move played by the user
                 *  in-charge to the other user.
                 */

                handlePokemonMoveMessage(msg);

            } else if (messageType.equals(JsonValue.
                    POKEMON_MOVE_RESPONSE_MESSAGE.getValue())
                    && context instanceof GamePage) {

                /*
                 *  Message is for conveying the response of the
                 *  other user (who is not in-charge of the current turn)
                 *  to the user in-charge of the current turn.
                 */

                handlePokemonMoveResponseMessage(msg);

            } else {
                /*
                 *  The received message is not appropriate
                 *  for the current activity. Restart the SQS
                 *  listener.
                 */

                if (context instanceof HomePage) {
                    ((HomePage) context).startSqsListener();
                } else if (context instanceof PreGame) {
                    ((PreGame) context).startSqsListener();
                } else if (context instanceof GamePage) {
                    ((GamePage) context).startSqsListener();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    /**
     *  This method handles game start request.
     *  @param msg  JSON object containing details about
     *              the request.
     */
    private void handleGameStartRequest(final JSONObject msg) {

        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {

                try {
                    ((HomePage) context).showInvitationDialog(msg.
                            getString(JsonKey.USERNAME.getKey()));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     *  This method handles game start response.
     *  @param msg  JSON object containing details about
     *              the response.
     */
    private void handleGameStartResponse(final JSONObject msg) {

        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {

                try {

                    String otherUsername = msg.getString(JsonKey.
                            USERNAME.getKey());
                    boolean response = msg.getBoolean(JsonKey.
                            RESPONSE.getKey());

                    String toastMessage = "";
                    if (response) {
                        toastMessage = String.format(context.
                                        getResources().getString(R.
                                        string.userAcceptedText),
                                otherUsername);
                    } else {
                        toastMessage = String.format(context.
                                        getResources().getString(R.
                                        string.userDeclinedText),
                                otherUsername);
                    }

                    ((HomePage) context).showToast(toastMessage,
                            Toast.LENGTH_SHORT);

                    if (!response) {
                        ((HomePage) context).startSqsListener();
                    } else {
                        ((HomePage) context).gotoPreGame(otherUsername);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     *  This method handles Pokemon Cards Init message,
     *  i.e., when the controller user sends the cards to
     *  the non controller user.
     *  @param msg  JSON object containing details about
     *              the message.
     *  @throws JSONException Throws JSON exception.
     */
    private void handlePokemonCardsInitMessage(final JSONObject msg)
            throws JSONException {

        JSONArray pokemonIdArray = msg.getJSONArray(JsonKey.
                INIT_POKEMON_LIST.getKey());

        ArrayList<Integer> pokemonIds = new ArrayList<>();

        for (int index = 0; index < pokemonIdArray.length(); index++) {
            pokemonIds.add(pokemonIdArray.getInt(index));
        }

        String senderUser = msg.getString(JsonKey.USERNAME.getKey());

        ((PreGame) context).setNonControllerUserCards(pokemonIds,
                senderUser);
    }

    /**
     *  This method handles toss decision message sent by
     *  the controller user to the non controller user.
     *  @param msg  JSON object containing details about
     *              the message.
     *  @throws JSONException Throws JSON exception.
     */
    private void handleTossDecisionMessage(final JSONObject msg)
            throws JSONException {

        String senderUser = msg.getString(JsonKey.USERNAME.getKey());
        boolean myTurn = msg.getBoolean(JsonKey.TOSS_DECISION.getKey());

        ((GamePage) context).showNonControllerUserTossDecision(
                senderUser, myTurn);
    }

    /**
     *  This method handles the Pokemon move message sent by the
     *  in-charge user to the other user.
     *  @param msg  JSON object containing details about
     *              the message.
     *  @throws JSONException Throws JSON exception.
     */
    private void handlePokemonMoveMessage(final JSONObject msg)
            throws JSONException {

        String senderUser = msg.getString(JsonKey.USERNAME.getKey());
        int pokemonId = msg.getInt(JsonKey.POKEMON_NUMBER.getKey());
        String pokemonAttribute = msg.getString(JsonKey.
                POKEMON_ATTRIBUTE.getKey());

        ((GamePage) context).handleOpponentMove(senderUser, pokemonId,
                pokemonAttribute);
    }

    /**
     *  This method handles the Pokemon move response message sent
     *  by the non in-charge user to the in-charge user.
     *  @param msg  JSON object containing details about
     *              the message.
     *  @throws JSONException Throws JSON exception.
     */
    private void handlePokemonMoveResponseMessage(final JSONObject msg)
            throws JSONException {

        String senderUser = msg.getString(JsonKey.USERNAME.getKey());
        int pokemonId = msg.getInt(JsonKey.POKEMON_NUMBER.getKey());
        String pokemonAttribute = msg.getString(JsonKey.
                POKEMON_ATTRIBUTE.getKey());

        ((GamePage) context).handleMoveResponse(senderUser, pokemonId,
                pokemonAttribute);
    }

    /**
     *  This method updates the last seen time of the current user.
     */
    private void updateLastSeen() {

        long currentTime = System.currentTimeMillis();

        if (currentTime - previousLastSeenUpdateTime <= Constants.
                LAST_SEEN_UPDATE_INTERVAL) {

            return;
        }

        ddbClient.setUserLastSeen(sharedPreferencesHelper.getUsername(),
                UTCTime.getUtcTime());

        previousLastSeenUpdateTime = System.currentTimeMillis();
    }

    /**
     *  This method stops the message poll.
     */
    public void stop() {

        pollFurther = false;
    }
}
