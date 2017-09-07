package com.example.ujjwal.pokemoncardssample.dao.sqs;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.sqs.model.Message;
import com.example.ujjwal.pokemoncardssample.Constants;
import com.example.ujjwal.pokemoncardssample.HomePage;
import com.example.ujjwal.pokemoncardssample.PreGame;
import com.example.ujjwal.pokemoncardssample.R;
import com.example.ujjwal.pokemoncardssample.utils.JsonKey;
import com.example.ujjwal.pokemoncardssample.utils.JsonValue;

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

    /** SQSClient object. */
    private SQSClient sqsClient;

    /** Name of the queue for which the listener is set up. */
    private String queueName;

    /** URL of the queue for which the listener is set up. */
    private String queueUrl;

    /**
     *  Constructor for the class.
     *  @param passedContext    Context passed by the calling activity.
     *  @param passedSqsClient  SQSClient object.
     *  @param passedQueueName  Queue name for which the listener is set up.
     */
    public SQSListener(final Context passedContext,
                       final SQSClient passedSqsClient,
                       final String passedQueueName) {

        this.context = passedContext;
        this.sqsClient = passedSqsClient;
        this.queueName = passedQueueName;

        Thread getUrlThread = new Thread() {
            @Override
            public void run() {
                queueUrl = sqsClient.getQueueUrl(queueName);
            }
        };
        getUrlThread.start();
        try {
            getUrlThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
                    int previousNumberOfMessages = (sqsClient.
                            getMessages(queueUrl)).size();

                /* Listen for messages on the queue. */
                    while (true) {

                        List<Message> messages = sqsClient.
                                getMessages(queueUrl);

                    /* Message is received.
                    *  Process it and then delete it. */
                        if (messages.size() > previousNumberOfMessages) {

                            Message receivedMessage = messages.get(
                                    messages.size() - 1);
                            String messageBody = receivedMessage.getBody();
                            sqsClient.deleteMessage(queueUrl,
                                    receivedMessage.getReceiptHandle());
                            processMessage(messageBody);

                            break;
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

                            ((HomePage) context).showToast(context.
                                    getResources().
                                    getString(R.string.connectionProblem),
                                    Toast.LENGTH_SHORT);
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
            } else if (messageType.equals(JsonValue.
                    GAME_START_RESPONSE.getValue())
                    && context instanceof HomePage) {

                /*
                 *  Message is for Game Start Response.
                 */
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
            } else if (messageType.equals(JsonValue.
                    POKEMON_CARDS_INIT_MESSAGE.getValue())
                    && context instanceof PreGame) {

                /*
                 *  Message is for telling about the initial
                 *  Pokemon IDs for the non-controller user.
                 */

                JSONArray pokemonIdArray = msg.getJSONArray(JsonKey.
                        INIT_POKEMON_LIST.getKey());

                ArrayList<Integer> pokemonIds = new ArrayList<>();

                for (int index = 0; index < pokemonIdArray.length(); index++) {
                    pokemonIds.add(pokemonIdArray.getInt(index));
                }

                ((PreGame) context).setNonControllerUserCards(pokemonIds);
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
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
