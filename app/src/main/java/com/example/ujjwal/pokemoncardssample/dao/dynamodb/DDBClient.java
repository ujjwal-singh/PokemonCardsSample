package com.example.ujjwal.pokemoncardssample.dao.dynamodb;

import android.content.Context;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.example.ujjwal.pokemoncardssample.Constants;

/**
 * Created by ujjwal on 19/7/17.
 * This is a Singleton class to manage DDB calls.
 */

public final class DDBClient {

    /** Context passed by calling activity. */
    private static Context context;

    /** Amazon AWS Cognito credentials. */
    private static CognitoCachingCredentialsProvider credentialsProvider;

    /** DynamoDB client. */
    private static AmazonDynamoDBClient ddbClient;

    /** Dynamo DB region. */
    private static Region region;

    /** DynamoDB Mapper. */
    private static DynamoDBMapper ddbMapper;

    /** The only instance of this Singleton class. */
    private static DDBClient instance = null;

    /**
     *  This method returns a static instance of the class.
     *  Uses lazy initialization.
     *  @param passedContext  Context context, passed by the calling activity.
     *  @return DDBClient, the only instance of the class.
     */
    public static DDBClient getInstance(final Context passedContext) {

        if (instance == null) {
            instance = new DDBClient(passedContext);
        }
        return instance;
    }

    /**
     *  Method overload.
     *  This method returns the current instance.
     *  Can return null also.
     *  @return DDBClient, the only instance of the class.
     */
    public static DDBClient getInstance() {
        return instance;
    }

    /**
     *  Private constructor, disables initialization from other classes.
     *  @param passedContext Context passed by the calling activity.
     */
    private DDBClient(final Context passedContext) {

        /**
         *  FindBugs will throws the folowing warning --
         *  This instance method writes to a static field.
         *  This is tricky to get correct if multiple instances
         *  are being manipulated, and generally bad practice.
         *
         *  However, this is a Singleton class.
         */
        this.context = passedContext;
        region = Region.getRegion(Regions.US_EAST_2);
        credentialsProvider = buildCredentials();
        ddbClient = new AmazonDynamoDBClient(credentialsProvider);
        ddbClient.setRegion(region);
        ddbMapper = new DynamoDBMapper(ddbClient);
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
     *  This method saves a generic object into a DynamoDB table.
     *  @param obj Object to be saved in the DynamoDBTable.
     *  @throws AmazonClientException Throws this exception in case
     *          of network problems.
     */
    public static void saveItem(final Object obj) throws AmazonClientException {

        try {
            ddbMapper.save(obj);
        } catch (AmazonClientException e) {
            throw e;
        }
    }

    /**
     *  This method accepts details of new user as input, username and password.
     *  It writes to three DDB tables : UserAuthentication
     *                                  UserAvailability
     *                                  UserHistory
     *                                  Creates a new entry in
     *                                  each of the three tables.
     *  @param username String username, username of the new user.
     *  @param password String password, MD5 hash of the password of new user.
     *  @throws AmazonClientException Throws this exception in case
     *          of network problems.
     */
    public static void createUser(final String username,
                                  final String password)
                                throws AmazonClientException {

        try {
            UserAuthentication newUser = new UserAuthentication();
            newUser.setUsername(username);
            newUser.setPassword(password);
            saveItem(newUser);

            UserAvailability newUserAvailability = new UserAvailability();
            newUserAvailability.setUsername(username);
            newUserAvailability.setOnline(true);
            newUserAvailability.setInGame(false);
            saveItem(newUserAvailability);

            UserHistory newUserHistory = new UserHistory();
            newUserHistory.setUsername(username);
            newUserHistory.setGamesPlayed(0);
            newUserHistory.setGamesWon(0);
            newUserHistory.setGamesLost(0);
            saveItem(newUserHistory);
        } catch (AmazonClientException e) {
            throw e;
        }
    }

    /**
     *  This method fetches a user from UserAuthentication table,
     *  based on username.
     *  Returns null if no such user exists.
     *  @param username String username, the username to be fetched.
     *  @return UserAuthentication, corresponding item in the table.
     *  @throws AmazonClientException Throws this exception in case
     *          of network problems.
     */
    public static UserAuthentication retrieveUser(final String username)
            throws AmazonClientException {

        try {
            return ddbMapper.load(UserAuthentication.class, username);
        } catch (AmazonClientException e) {
            throw e;
        }
    }

    /**
     *  This method fetches the availability details of a user,
     *  based on the username.
     *  @param username String username.
     *  @return UserAvailability object.
     *  @throws AmazonClientException Throws this exception in case
     *          of network problems.
     */
    public static UserAvailability retrieveUserAvailability(
            final String username) throws AmazonClientException {

        try {
            return ddbMapper.load(UserAvailability.class, username);
        } catch (AmazonClientException e) {
            throw e;
        }
    }

    /**
     *  This method fetches the history of a user, based on the username.
     *  @param username String username.
     *  @return UserHistory object.
     *  @throws AmazonClientException Throws this exception in case
     *          of network problems.
     */
    public static UserHistory retrieveUserHistory(final String username)
            throws AmazonClientException {

        try {
            return ddbMapper.load(UserHistory.class, username);
        } catch (AmazonClientException e) {
            throw e;
        }
    }

    /**
     *  This method deletes a generic object from a DDB table.
     *  @param obj Object to be deleted from the table.
     *  @throws AmazonClientException Throws this exception in case
     *          of network problems.
     */
    public static void deleteItem(final Object obj) {

        try {
            ddbMapper.delete(obj);
        } catch (AmazonClientException e) {
            throw e;
        }
    }

    /**
     *  This method deletes a user from the UserAuthentication table,
     *  UserAvailability table and UserHistory table based on the username.
     *  @param username The username of the user to be deleted.
     *  @throws AmazonClientException Throws this exception in case
     *          of network problems.
     */
    public static void deleteUser(final String username)
            throws AmazonClientException {

        try {
            deleteItem(retrieveUser(username));
            deleteItem(retrieveUserAvailability(username));
            deleteItem(retrieveUserHistory(username));
        } catch (AmazonClientException e) {
            throw e;
        }
    }

    /**
     *  This method sets user online status, as per given username and status.
     *  @param username String username, user whose status is to be set.
     *  @param isOnline boolean isOnline, online status to be set.
     *  @throws AmazonClientException Throws this exception in case
     *          of network problems.
     */
    public static void setUserOnlineAvailability(final String username,
                                                 final boolean isOnline)
                                                throws AmazonClientException {

        try {
            UserAvailability userAvailability =
                    retrieveUserAvailability(username);
            userAvailability.setOnline(isOnline);
            saveItem(userAvailability);
        } catch (AmazonClientException e) {
            throw e;
        }
    }
}
