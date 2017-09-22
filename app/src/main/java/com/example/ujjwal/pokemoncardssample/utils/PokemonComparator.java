package com.example.ujjwal.pokemoncardssample.utils;

import com.example.ujjwal.pokemoncardssample.pokemon.Pokemon;

/**
 *  This class implements functions for comparing
 *  two Pokemons based on their attributes.
 *  @author ujjwal
 */

public final class PokemonComparator {

    /**
     *  This method compares two Pokemons, based on a given
     *  attribute.
     *  Also, an extra parameter is passed which determines
     *  the result in case of ties. If it is current user's
     *  turn, then he/she is given preference over the other
     *  user.
     *  @param myPokemon    The Pokemon of the current user.
     *  @param otherPokemon The Pokemon of the other user.
     *  @param attribute    String, the attribute of
     *                      the Pokemons to be compared.
     *                      e.g., number
     *                            height, etc.
     *  @param isMyTurn     Boolean variable to indicate whose
     *                      turn is it. True, if it is current
     *                      user's turn, else False.
     *  @return Boolean
     */
    public static boolean comparePokemon(final Pokemon myPokemon,
                                         final Pokemon otherPokemon,
                                         final String attribute,
                                         final boolean isMyTurn) {

        if (attribute.equals(JsonValue.POKEMON_NUMBER.getValue())) {

            if (myPokemon.getNumber() > otherPokemon.getNumber()) {

                return true;
            }

            if ((myPokemon.getNumber() == otherPokemon.getNumber())
                    && isMyTurn) {

                return true;
            }
        } else if (attribute.equals(JsonValue.POKEMON_HEIGHT.getValue())) {

            if (myPokemon.getHeight() > otherPokemon.getHeight()) {

                return true;
            }

            if ((myPokemon.getHeight() == otherPokemon.getHeight())
                    && isMyTurn) {

                return true;
            }
        } else if (attribute.equals(JsonValue.POKEMON_WEIGHT.getValue())) {

            if (myPokemon.getWeight() > otherPokemon.getWeight()) {

                return true;
            }

            if ((myPokemon.getWeight() == otherPokemon.getWeight())
                    && isMyTurn) {

                return true;
            }
        }

        return false;
    }

    /**
     *  Util classes should not be initialized.
     *  Prevents initialization.
     */
    private PokemonComparator() {

    }
}
