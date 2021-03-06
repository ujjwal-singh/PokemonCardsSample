package com.example.ujjwal.pokemoncardssample.dao.dynamodb;

import android.content.Context;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.example.ujjwal.pokemoncardssample.Constants;
import com.example.ujjwal.pokemoncardssample.utils.UTCTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  Created by ujjwal on 19/7/17.
 *  This is a Singleton class to manage DDB calls.
 *  @author ujjwal
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

        ddbMapper.save(obj);
    }

    /**
     *  This method accepts details of new user as input, username and password.
     *  It writes to three DDB tables : UserAuthentication
     *                                  UserAvailability
     *                                  UserHistory
     *                                  Creates a new entry in
     *                                  each of the three tables.
     *  @param username String username, username of the new user.
     *  @param emailId String email-id of the new user.
     *  @throws AmazonClientException Throws this exception in case
     *          of network problems.
     */
    public static void createUser(final String username,
                                  final String emailId)
                                throws AmazonClientException {

        UserAvailability newUserAvailability = new UserAvailability();
        newUserAvailability.setUsername(username);
        newUserAvailability.setLastSeen(0L);
        newUserAvailability.setInGame(false);
        saveItem(newUserAvailability);

        UserHistory newUserHistory = new UserHistory();
        newUserHistory.setUsername(username);
        newUserHistory.setGamesPlayed(0);
        newUserHistory.setGamesWon(0);
        newUserHistory.setGamesLost(0);
        saveItem(newUserHistory);

        /* User authentication table is updated last because
         * it being filled implies that the user creation was successful.
         * (This table is checked for user existence whenever required
         *  later on.) */
        UserAuthentication newUser = new UserAuthentication();
        newUser.setUsername(username);
        newUser.setEmailId(emailId);
        saveItem(newUser);
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

        return ddbMapper.load(UserAuthentication.class, username);
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

        return ddbMapper.load(UserAvailability.class, username);
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

        return ddbMapper.load(UserHistory.class, username);
    }

    /**
     *  This method deletes a generic object from a DDB table.
     *  @param obj Object to be deleted from the table.
     *  @throws AmazonClientException Throws this exception in case
     *          of network problems.
     */
    public static void deleteItem(final Object obj) {

        ddbMapper.delete(obj);
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

        deleteItem(retrieveUser(username));
        deleteItem(retrieveUserAvailability(username));
        deleteItem(retrieveUserHistory(username));
    }

    /**
     *  This method sets user status,
     *  as per given username and status.
     *  The status consists of two parts -- isSignedIn and isInGame.
     *  @param username String username, user whose status is to be set.
     *  @param isSignedIn boolean isSignedIn, whether the user is currently
     *                    signed in or not.
     *                    True means the user is signed in.
     *  @param isInGame boolean isInGame, whether the user is currently
     *                  in a game or not.
     *                  True means the user is in a Game currently.
     *  @throws AmazonClientException Throws this exception in case
     *          of network problems.
     */
    public static void setUserStatus(final String username,
                                           final boolean isSignedIn,
                                     final boolean isInGame)
            throws AmazonClientException {

        UserAvailability userAvailability =
                retrieveUserAvailability(username);
        userAvailability.setInGame(isInGame);
        userAvailability.setSignedIn(isSignedIn);
        saveItem(userAvailability);
    }

    /**
     *  This method sets last seen status of user.
     *  @param username String username, whose lastSeen is to be set.
     *  @param lastSeen Long lastSeen in UTC time.
     *  @throws AmazonClientException Throws this exception in case
     *          of network problems.
     */
    public static void setUserLastSeen(final String username,
                                       final long lastSeen)
            throws AmazonClientException {

        /* Handles the case when the user info has just been deleted
         * from Shared Preferences, but there is an attempt to
         * update his last seen. */
        if (username.equals(Constants.
                NULL_USERNAME_SHARED_PREFERENCES)) {
            return;
        }

        UserAvailability userAvailability =
                retrieveUserAvailability(username);

        /* Handles the case when the user info has just been deleted
         * from UserAuthentication table, but there is an attempt to
         * update his last seen. */
        if (userAvailability != null) {
            userAvailability.setLastSeen(lastSeen);
            saveItem(userAvailability);
        }
    }

    /**
     *  This method updates the user history after the result
     *  of a game.
     *  @param username String username of the current user.
     *  @param gameResult   Boolean,    True if the current user
     *                                  won the game.
     *                                  False, otherwise.
     *  @throws AmazonClientException Throws this exception in case
     *          of network problems.
     */
    public static void updateUserHistory(final String username,
                                         final boolean gameResult)
            throws AmazonClientException {

        UserHistory userHistory = retrieveUserHistory(username);

        int gamesPlayed = userHistory.getGamesPlayed();
        int gamesWon = userHistory.getGamesWon();
        int gamesLost = userHistory.getGamesLost();

        userHistory.setGamesPlayed(gamesPlayed + 1);

        if (gameResult) {

            userHistory.setGamesWon(gamesWon + 1);
        } else {

            userHistory.setGamesLost(gamesLost + 1);
        }

        saveItem(userHistory);
    }

    /**
     *  This method fetches the list of users available for game,
     *  and returns it as a list of strings.
     *  Availability condition is : (a) The user should be online,
     *  meaning than the interval between his previous lastSeen update
     *  and the current online users query must be less than some
     *  threshold value,
     *  and (b) The user should not be involved in a game, i.e.,
     *  inGame should be equal to 0.
     *  The current username (user who is executing the query)
     *  is excluded from the list.
     *  @param username String, determines which user is executing the query.
     *  @return List,   List of String which contains the online users.
     *  @throws AmazonClientException Throws this exception in case
     *          of network problems.
     */
    public static List<String> getAvailableUsersList(final String username)
            throws AmazonClientException {

        HashMap<String, Condition> scanFilter = new
                HashMap<String, Condition>();

        /* Filtering for signed in users. 1 indicates that the user is
         * currently signed in. */
        Condition signedInCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN("1"));

        /* Filtering for non-playing users. 0 indicates that the
         * user is not involved in any game currently. */
        Condition nonPlayingCondition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withN("0"));

        scanFilter.put(
                Constants.DDB_USER_AVAILABILITY_TABLE_ATTR_SIGNED_IN,
                signedInCondition);

        scanFilter.put(
                Constants.DDB_USER_AVAILABILITY_TABLE_ATTR_IN_GAME,
                nonPlayingCondition);

        ScanRequest scanRequest = new ScanRequest(
                Constants.DDB_USER_AVAILABILITY_TABLE_NAME)
                .withScanFilter(scanFilter);

        ScanResult scanResult = ddbClient.scan(scanRequest);
        List<Map<String, AttributeValue>> itemList = scanResult.getItems();

        List<String> onlineUsernameList = new ArrayList<String>();

        long currentUtcTime = UTCTime.getUtcTime();

        for (Map<String, AttributeValue> currentItem : itemList) {

            if (currentUtcTime - Long.parseLong(currentItem.get(Constants.
                    DDB_USER_AVAILABILITY_TABLE_ATTR_LAST_SEEN).getN())
                    <= Constants.LAST_SEEN_AND_ONLINE_GAP) {

                String onlineUsername = (currentItem.get(
                        Constants.DDB_USER_AVAILABILITY_TABLE_ATTR_USERNAME))
                        .getS();

                if (!onlineUsername.equals(username)) {
                    onlineUsernameList.add(onlineUsername);
                }
            }
        }

        return onlineUsernameList;
    }

    /**
     *  This method fetches a user from DDB UserAuth table, based
     *  on the email id of the user.
     *  Email ID is a unique attribute of the user, just like
     *  the username of the user.
     *  @param emailId  String  Email-ID of the user.
     *  @return userAuthentication object
     *          Can return null if no such user is found.
     *  @throws AmazonClientException Throws this exception in case
     *          of network problems.
     */
    public static UserAuthentication retrieveUserByEmaiId(final String emailId)
            throws AmazonClientException {

        HashMap<String, Condition> scanFilter = new HashMap<>();

        Condition emailMatchCondtion = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue().withS(emailId));

        scanFilter.put(Constants.DDB_USER_AUTH_TABLE_ATTR_EMAIL,
                emailMatchCondtion);

        ScanRequest scanRequest = new ScanRequest(Constants.
                DDB_USER_AUTH_TABLE_NAME).withScanFilter(scanFilter);

        ScanResult scanResult = ddbClient.scan(scanRequest);
        List<Map<String, AttributeValue>> itemList = scanResult.getItems();

        if (itemList.size() == 0) {

            return null;
        }

        Map<String, AttributeValue> userAuthenticationItem = itemList.get(0);

        UserAuthentication userAuthentication = new UserAuthentication();
        userAuthentication.setUsername((userAuthenticationItem.get(Constants.
                DDB_USER_AUTH_TABLE_ATTR_USERNAME)).getS());
        userAuthentication.setEmailId((userAuthenticationItem.get(Constants.
                DDB_USER_AUTH_TABLE_ATTR_EMAIL)).getS());

        return userAuthentication;
    }
}
