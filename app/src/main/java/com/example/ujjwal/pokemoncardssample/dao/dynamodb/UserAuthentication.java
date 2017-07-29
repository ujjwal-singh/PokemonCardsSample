package com.example.ujjwal.pokemoncardssample.dao.dynamodb;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;

import com.example.ujjwal.pokemoncardssample.Constants;

import lombok.Setter;

/**
 *  Created by ujjwal on 19/7/17.
 *  This class acts as the model class for UserAuthentication DDB table.
 */
@DynamoDBTable(tableName = Constants.DDB_USER_AUTH_TABLE_NAME)
@Setter
public class UserAuthentication {

    /** Username of the user. */
    private String username;

    /** MD5 hash of the password of the user. */
    private String password;

    /**
     *  This method returns the username of the user.
     *  @return String  username of the user.
     */
    @DynamoDBHashKey(attributeName =
            Constants.DDB_USER_AUTH_TABLE_ATTR_USERNAME)
    public String getUsername() {

        return username;
    }

    /**
     *  This method returns the password of the user.
     *  @return String  password of the user.
     */
    @DynamoDBAttribute(attributeName =
            Constants.DDB_USER_AUTH_TABLE_ATTR_PASSWORD)
    public String getPassword() {

        return password;
    }
}
