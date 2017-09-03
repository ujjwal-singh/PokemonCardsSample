package com.example.ujjwal.pokemoncardssample.dao.sqs;

import android.content.Context;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.example.ujjwal.pokemoncardssample.Constants;

import org.json.JSONObject;

import java.util.List;

/**
 *  This class is a Singleton class.
 *  Provides SQS Client to the app.
 *  Created by ujjwal on 2/8/17.
 *  @author ujjwal
 */
public final class SQSClient {

    /** Context passed by calling activity. */
    private static Context context;

    /** Amazon AWS Cognito credentials. */
    private static CognitoCachingCredentialsProvider credentialsProvider;

    /** AWS SQS Client. */
    private static AmazonSQSClient sqsClient;

    /** Dynamo DB region. */
    private static Region region;

    /** The only instance of this Singleton class. */
    private static SQSClient instance = null;

    /**
     *  This method returns a static instance of the class.
     *  Uses lazy initialization.
     *  @param passedContext  Context context, passed by the calling activity.
     *  @return SQSClient, the only instance of the class.
     */
    public static SQSClient getInstance(final Context passedContext) {

        if (instance == null) {
            instance = new SQSClient(passedContext);
        }
        return instance;
    }

    /**
     *  Method overload.
     *  This method returns the current instance.
     *  Can return null also.
     *  @return DDBClient, the only instance of the class.
     */
    public static SQSClient getInstance() {
        return instance;
    }

    /**
     *  Private constructor, disables initialization from other classes.
     *  @param passedContext Context passed by the calling activity.
     */
    private SQSClient(final Context passedContext) {

        /**
         *  FindBugs will throws the following warning --
         *  This instance method writes to a static field.
         *  This is tricky to get correct if multiple instances
         *  are being manipulated, and generally bad practice.
         *
         *  However, this is a Singleton class.
         */

        this.context = passedContext;
        region = Region.getRegion(Regions.US_EAST_2);
        credentialsProvider = buildCredentials();
        sqsClient = new AmazonSQSClient(credentialsProvider);
        sqsClient.setRegion(region);
    }

    /**
     *  This method returns the AWS cognito credentials.
     *  @return Cognito credentials.
     */
    private CognitoCachingCredentialsProvider buildCredentials() {

        return (new CognitoCachingCredentialsProvider(
                context,
                Constants.IDENTITY_POOL_ID,
                Regions.US_EAST_2));
    }

    /**
     *  This method returns the URL of a SQS queue,
     *  identified by its name.
     *  @param queueName The name of the queue.
     *  @throws AmazonClientException Throws this exception in case
     *          of network problems.
     *  @return String Queue URL
     */
    public static String getQueueUrl(final String queueName)
            throws AmazonClientException {

        return (sqsClient.getQueueUrl(queueName).getQueueUrl());
    }

    /**
     *  This method creates a new AWS queue with given name.
     *  @param queueName String the name of the queue to be created.
     *  @throws AmazonClientException Throws this exception in case
     *          of network problems.
     */
    public static void createQueue(final String queueName)
            throws AmazonClientException {

        sqsClient.createQueue(new CreateQueueRequest(queueName));
    }

    /**
     *  This method deletes a SQS queue, based on the queue name.
     *  (Regardless of whether the queue is empty or not.)
     *  @param queueName String the name of the queue to be deleted.
     *  @throws AmazonClientException Throws this exception in case
     *          of network problems.
     */
    public static void deleteQueue(final String queueName)
            throws AmazonClientException {

        sqsClient.deleteQueue(new
                DeleteQueueRequest(getQueueUrl(queueName)));
    }

    /**
     *  This method sends a message to a SQS queue, identified
     *  by the queue URL.
     *  @param queueUrl The URL of the queue.
     *  @param message  Message to be sent (JSON object).
     *  @throws AmazonClientException Throws this exception in case
     *          of network problems.
     */
    public static void sendMessage(final String queueUrl,
                                   final JSONObject message)
            throws AmazonClientException {

        sqsClient.sendMessage(new SendMessageRequest(queueUrl,
                message.toString()));
    }

    /**
     *  This method sends a message to a SQS queue, identified
     *  by the queue name.
     *  @param queueName    The name of the queue.
     *  @param message  Message to be sent (JSON object).
     *  @param dummy    Useless, just to support method
     *                  overloading.
     *  @throws AmazonClientException Throws this exception in case
     *          of network problems.
     */
    public static void sendMessage(final String queueName,
                                   final JSONObject message, final int dummy)
            throws AmazonClientException {

        sendMessage(getQueueUrl(queueName), message);
    }

    /**
     *  This method fetches all the messages in a SQS queue,
     *  identified by its URL.
     *  @param queueUrl    URL of the queue.
     *  @return List of Messages.
     *  @throws AmazonClientException Throws this exception in case
     *          of network problems.
     */
    public static List<Message> getMessages(final String queueUrl)
            throws AmazonClientException {

        ReceiveMessageRequest receiveMessageRequest = new
                ReceiveMessageRequest(queueUrl);

        return (sqsClient.receiveMessage(receiveMessageRequest).getMessages());
    }

    /**
     *  This method deletes a particular message from a queue,
     *  identified by the URL of the queue and the recipient
     *  handle of the message.
     *  @param queueUrl URL of the queue.
     *  @param recipientHandle  Recipient handle of the message.
     *  @throws AmazonClientException Throws this exception in case
     *          of network problems.
     */
    public static void deleteMessage(final String queueUrl,
                                     final String recipientHandle)
            throws AmazonClientException {

        sqsClient.deleteMessage(queueUrl, recipientHandle);
    }
}
