package com.example.ujjwal.pokemoncardssample.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *  This class contains JSON values as Enum.
 *  Created by ujjwal on 6/8/17.
 *  @author ujjwal
 */
@AllArgsConstructor
@Getter
public enum JsonValue {

    /** One of the values for MESSAGE_TYPE key.
     *  Header for every message. e.g.,
     *  "MessageType" : "GameStartRequest" */
    GAME_START_REQUEST("GameStartRequest"),

    /** One of the values for MESSAGE_TYPE key.
     *  Header for every message. e.g.,
     *  "MessageType" : "GameStartResponse"
     */
    GAME_START_RESPONSE("GameStartResponse");

    /** String to store the value of a JSON
     *  key-value pair. */
    private String value;
}
