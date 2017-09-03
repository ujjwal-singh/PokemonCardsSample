package com.example.ujjwal.pokemoncardssample.dao.sqs;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.sqs.model.Message;
import com.example.ujjwal.pokemoncardssample.Constants;
import com.example.ujjwal.pokemoncardssample.HomePage;
import com.example.ujjwal.pokemoncardssample.utils.JsonKey;
import com.example.ujjwal.pokemoncardssample.utils.JsonValue;

import org.json.JSONObject;
import org.json.JSONException;

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
                            processMessage(receivedMessage.getBody());
                            sqsClient.deleteMessage(queueUrl,
                                    receivedMessage.getReceiptHandle());
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

                            String message = "Connection Problem";
                            ((HomePage) context).showToast(message,
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
                                toastMessage = "User : "
                                        + otherUsername + " accepted "
                                        + "your request !! Lets Play !!";
                            } else {
                                toastMessage = "User : "
                                        + otherUsername + " declined "
                                        + "your request.";
                            }

                            ((HomePage) context).showToast(toastMessage,
                                    Toast.LENGTH_SHORT);

                            if (!response) {
                                ((HomePage) context).startSqsListener();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
