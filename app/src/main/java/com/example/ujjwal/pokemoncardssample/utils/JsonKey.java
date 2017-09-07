package com.example.ujjwal.pokemoncardssample.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *  This class contains JSON keys as Enum.
 *  Created by ujjwal on 6/8/17.
 *  @author ujjwal
 */
@AllArgsConstructor
@Getter
public enum JsonKey {

    /** Header of every JSON message,
     *  identifies the type of message. e.g.,
     *  "MessageType" : "GameStartRequest" */
    MESSAGE_TYPE("MessageType"),

    /** Key of username attribute. e.g.-
     *  "username" : "ABC" */
    USERNAME("username"),

    /**
     *  This key identifies a boolean response.
     *  (Acts as key for a boolean response value.)
     */
    RESPONSE("response"),

    /**
     *  This acts as key for the initial list
     *  of Pokemon IDs sent by the controller user
     *  to the other user.
     */
    INIT_POKEMON_LIST("InitPokemonList"),

    /**
     *  This key identifies the name of the
     *  Pokemon in the JSON file containing info
     *  about the Pokemon.
     */
    POKEMON_NAME("name"),

    /**
     *  This key identifies the height of the
     *  Pokemon in the JSON file containing info
     *  about the Pokemon.
     */
    POKEMON_HEIGHT("height"),

    /**
     *  This key identifies the weight of the
     *  Pokemon in the JSON file containing info
     *  about the Pokemon.
     */
    POKEMON_WEIGHT("weight"),

    /**
     *  This key identifies the types of the
     *  Pokemon in the JSON file containing info
     *  about the Pokemon.
     */
    POKEMON_TYPES("types"),

    /**
     *  This key identifies the chain of the
     *  Pokemon in the JSON file containing info
     *  about the Pokemon.
     */
    POKEMON_CHAIN("chain"),

    /**
     *  This key identifies the number of the
     *  base Pokemon in the JSON file containing
     *  info about the Pokemon.
     */
    POKEMON_NUMBER("number");

    /** String to store the key of a JSON
     *  key-value pair. */
    private String key;
}
