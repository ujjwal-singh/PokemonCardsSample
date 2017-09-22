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
    GAME_START_RESPONSE("GameStartResponse"),

    /** One of the values for MESSAGE_TYPE
     *  key. Sent when initializing the game,
     *  wherein the controller user sends the
     *  PokemonIds of the other user to the other
     *  user. e.g.,
     *  "MessageType" : "PokemonCardsInitMessage"
     */
    POKEMON_CARDS_INIT_MESSAGE("PokemonCardsInitMessage"),

    /** One of the values for MESSAGE_TYPE
     *  key. Used when the Controller user
     *  sends toss info to the non-controller
     *  user.
     */
    TOSS_DECISION_MESSAGE("TossDecisionMessage"),

    /**
     *  One of the values for MESSAGE_TYPE key.
     *  Sent when the first user (user who won the previous round)
     *  plays his card.
     */
    POKEMON_MOVE_MESSAGE("PokemonMoveMessage"),

    /**
     *  One of the values for MESSAGE_TYPE key.
     *  Sent when the non in-charge user responds to the
     *  in-charge user's move.
     */
    POKEMON_MOVE_RESPONSE_MESSAGE("PokemonMoveResponseMessage"),

    /**
     *  This is one of the values for
     *  POKEMON_ATTRIBUTE key.
     */
    POKEMON_NUMBER("PokemonNumber"),

    /**
     *  This is one of the values for
     *  POKEMON_ATTRIBUTE key.
     */
    POKEMON_HEIGHT("PokemonHeight"),

    /**
     *  This is one of the values for
     *  POKEMON_ATTRIBUTE key.
     */
    POKEMON_WEIGHT("PokemonWeight"),

    /**
     *  This is one of the values for
     *  POKEMON_ATTRIBUTE key.
     */
    POKEMON_TYPE("PokemonType");

    /** String to store the value of a JSON
     *  key-value pair. */
    private String value;
}
