package com.example.ujjwal.pokemoncardssample.utils;

import com.example.ujjwal.pokemoncardssample.pokemon.Pokemon;

import java.util.ArrayList;

/**
 *  This class contains a static method for
 *  getting an ArrayList of IDs of Pokemons
 *  belonging to a given ArrayList of Pokemons.
 *
 *  @author ujjwal
 */
public final class PokemonIDList {

    /**
     *  This method builds and returns an ArrayList, which
     *  contains the IDs of Pokemons, passed as an ArrayList
     *  of Pokemons.
     *  @param pokemonList  ArrayList of Pokemons
     *  @return ArrayList of the IDs of the Pokemons in
     *          pokemonList.
     */
    public static ArrayList<Integer> getIDList(final ArrayList<Pokemon>
                                                       pokemonList) {

        ArrayList<Integer> idList = new ArrayList<>();

        for (Pokemon currentPokemon : pokemonList) {
            idList.add(currentPokemon.getNumber());
        }

        return idList;
    }

    /**
     *  Util classes should not be initialized.
     *  Prevents initialization.
     */
    private PokemonIDList() {
    }
}
