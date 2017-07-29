package com.example.ujjwal.pokemoncardssample.dao.dynamodb;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.example.ujjwal.pokemoncardssample.Constants;

import lombok.Setter;

/**
 *  Created by ujjwal on 21/7/17.
 *  This class acts as the model class for UserAvailability DDB table.
 */
@DynamoDBTable(tableName = Constants.DDB_USER_AVAILABILITY_TABLE_NAME)
@Setter
public class UserAvailability {

    /** username of the User. */
    private String username;

    /** Stores online presence of user. */
    private boolean online;

    /** Stores whether user is currently in a game. */
    private boolean inGame;

    /**
     *  This method returns the username of the user.
     *  @return String  the username of the user.
     */
    @DynamoDBHashKey(attributeName =
            Constants.DDB_USER_AVAILABILITY_TABLE_ATTR_USERNAME)
    public String getUsername() {

        return username;
    }

    /**
     *  This method returns the online status of the user.
     *  True if the user is online, false otherwise.
     *  @return boolean   online status of the user.
     */
    @DynamoDBAttribute(attributeName =
            Constants.DDB_USER_AVAILABILITY_TABLE_ATTR_ONLINE)
    public boolean isOnline() {

        return online;
    }

    /**
     *  This method returns the inGame status of the user.
     *  True if the user is busy in a game, false otherwise.
     *  @return boolean   inGame status of the user.
     */
    @DynamoDBAttribute(attributeName =
            Constants.DDB_USER_AVAILABILITY_TABLE_ATTR_IN_GAME)
    public boolean isInGame() {

        return inGame;
    }
}
