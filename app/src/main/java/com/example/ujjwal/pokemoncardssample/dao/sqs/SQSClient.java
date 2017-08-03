package com.example.ujjwal.pokemoncardssample.dao.sqs;

import android.content.Context;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.example.ujjwal.pokemoncardssample.Constants;

/**
 * Created by ujjwal on 2/8/17.
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
     *  @param queueName String the name of the queue to be deleted.
     *  @throws AmazonClientException Throws this exception in case
     *          of network problems.
     */
    public static void deleteQueue(final String queueName)
            throws AmazonClientException {

        sqsClient.deleteQueue(new
                DeleteQueueRequest(getQueueUrl(queueName)));
    }
}
