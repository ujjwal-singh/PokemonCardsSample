package com.example.ujjwal.pokemoncardssample.utils;

import android.content.Context;

import com.example.ujjwal.pokemoncardssample.Constants;
import com.example.ujjwal.pokemoncardssample.pokemon.Pokemon;

import java.util.ArrayList;

/**
 *  Created by ujjwal on 30/9/17.
 *  This class provides help for Pokemon
 *  evolution.
 *
 *  @author ujjwal
 */
public final class PokemonEvolver {

    /**
     *  This method returns an ArrayList of evolution
     *  options for the given Pokemon.
     *  e.g.,   Bulbasaur -- [2]
     *          Pikachu   -- [26]
     *          Golem     -- []
     *          Tauros    -- []
     *          Eevee     -- [134, 135, 136]
     *
     * @param context   Context passed by the calling activity.
     *  @param pokemon  The Pokemon whose evolution options
     *                  have to be determined.
     *  @return ArrayList of Pokemon IDs (evolution options for
     *                                    the given Pokemon).
     */
    public static ArrayList<Integer> getEvolveList(
            final Context context, final Pokemon pokemon) {

        ArrayList<Integer> evolveList = new ArrayList<>();

        /* Handle border case of Hitmonlee.
         * Should not show option for evolving into Hitmonchan. */
        if (pokemon.getNumber() == Constants.HITMONLEE_POKEMON_NUMBER) {

            return evolveList;
        }

        /* Handle border case of Vaporeon and Jolteon.
         * Should not show option for evolving into Jolteon
          * and Flareon respectively. */
        if ((pokemon.getNumber() == Constants.VAPOREON_POKEMON_NUMBER)
                || pokemon.getNumber() == Constants.JOLTEON_POKEMON_NUMBER) {

            return evolveList;
        }

        /* Handle border case of Eevee. */
        if (pokemon.getNumber() == Constants.EEVEE_POKEMON_NUMBER) {

            evolveList.add(Constants.VAPOREON_POKEMON_NUMBER);
            evolveList.add(Constants.JOLTEON_POKEMON_NUMBER);
            evolveList.add(Constants.FLAREON_POKEMON_NUMBER);

            return evolveList;
        }

        /* Handle border case of last pokemon. */
        if (pokemon.getNumber() == Constants.NUMBER_OF_POKEMONS) {

            return evolveList;
        }

        if (pokemon.getBasePokemon() == (new Pokemon(context,
                pokemon.getNumber() + 1)).getBasePokemon()) {

            evolveList.add(pokemon.getNumber() + 1);
            return evolveList;
        }

        return evolveList;
    }

    /**
     *  Util classes should not be initialized.
     *  Prevents initialization.
     */
    private PokemonEvolver() {

    }
}
