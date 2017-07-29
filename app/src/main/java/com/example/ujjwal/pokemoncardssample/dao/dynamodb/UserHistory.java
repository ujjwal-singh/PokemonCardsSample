package com.example.ujjwal.pokemoncardssample.dao.dynamodb;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.example.ujjwal.pokemoncardssample.Constants;

import lombok.Setter;

/**
 * Created by ujjwal on 21/7/17.
 * This class acts as the model class for UserHistory DDB table.
 */
@DynamoDBTable(tableName = Constants.DDB_USER_HISTORY_TABLE_NAME)
@Setter
public class UserHistory {

    /** Username of the user. */
    private String username;

    /** Number of games played by the user. */
    private int gamesPlayed;

    /** Number of games won by the user. */
    private int gamesWon;

    /** Number of games lost by the user. */
    private int gamesLost;

    /**
     *  This method returns the username of the user.
     *  @return String  username of the user.
     */
    @DynamoDBHashKey(attributeName =
            Constants.DDB_USER_HISTORY_TABLE_ATTR_USERNAME)
    public String getUsername() {

        return username;
    }

    /**
     *  This method returns the number of games played by the user.
     *  @return int     number of games played by the user.
     */
    @DynamoDBAttribute(attributeName =
            Constants.DDB_USER_HISTORY_TABLE_ATTR_GAMES_PLAYED)
    public int getGamesPlayed() {

        return gamesPlayed;
    }

    /**
     *  This method returns the number of games won by the user.
     *  @return int     number of games won by the user.
     */
    @DynamoDBAttribute(attributeName =
            Constants.DDB_USER_HISTORY_TABLE_ATTR_GAMES_WON)
    public int getGamesWon() {

        return gamesWon;
    }

    /**
     *  This method returns the number of games lost by the user.
     *  @return int     number of games lost by the user.
     */
    @DynamoDBAttribute(attributeName =
            Constants.DDB_USER_HISTORY_TABLE_ATTR_GAMES_LOST)
    public int getGamesLost() {

        return gamesLost;
    }
}
