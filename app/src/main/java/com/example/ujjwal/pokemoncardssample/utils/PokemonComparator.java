package com.example.ujjwal.pokemoncardssample.utils;

import android.content.Context;
import android.widget.Toast;

import com.example.ujjwal.pokemoncardssample.Constants;
import com.example.ujjwal.pokemoncardssample.GamePage;
import com.example.ujjwal.pokemoncardssample.R;
import com.example.ujjwal.pokemoncardssample.pokemon.Pokemon;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 *  This class implements functions for comparing
 *  two Pokemons based on their attributes.
 *  @author ujjwal
 */

public final class PokemonComparator {

    /**
     *  Util classes should not be initialized.
     *  Prevents initialization.
     */
    private PokemonComparator() {

    }

    /**
     *  This method compares two Pokemons, based on a given
     *  attribute.
     *  Also, an extra parameter is passed which determines
     *  the result in case of ties. If it is current user's
     *  turn, then he/she is given preference over the other
     *  user.
     *
     *  @param context Passed context
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
    public static boolean comparePokemon(final Context context,
                                         final Pokemon myPokemon,
                                         final Pokemon otherPokemon,
                                         final String attribute,
                                         final boolean isMyTurn) {

        if (attribute.equals(JsonValue.POKEMON_NUMBER.getValue())) {

            return (compareNumber(myPokemon.getNumber(),
                    otherPokemon.getNumber(), isMyTurn));

        } else if (attribute.equals(JsonValue.POKEMON_HEIGHT.getValue())) {

            return (compareHeight(myPokemon.getHeight(),
                    otherPokemon.getHeight(), isMyTurn));

        } else if (attribute.equals(JsonValue.POKEMON_WEIGHT.getValue())) {

            return (compareWeight(myPokemon.getWeight(),
                    otherPokemon.getWeight(), isMyTurn));
        } else if (attribute.equals(JsonValue.POKEMON_TYPE.getValue())) {

            return (compareType(context, myPokemon, otherPokemon, isMyTurn));
        }

        return false;
    }

    /**
     *  This method compares two Pokemons based on their Pokemon IDs.
     *
     *  @param myNumber Current user's Pokemon ID
     *  @param otherNumber  Other user's Pokmeon ID
     *  @param isMyTurn     Boolean variable to indicate whose
     *                      turn is it. True, if it is current
     *                      user's turn, else False.
     *  @return Boolean
     */
    private static boolean compareNumber(final int myNumber,
                                            final int otherNumber,
                                            final boolean isMyTurn) {

        if (myNumber > otherNumber) {

            return true;
        }

        if ((myNumber == otherNumber) && isMyTurn) {

            return true;
        }

        return false;
    }

    /**
     *  This method compares two Pokemons based on their heights.
     *
     *  @param myHeight Current user's Pokemon Height
     *  @param otherHeight  Other user's Pokemon Weight
     *  @param isMyTurn     Boolean variable to indicate whose
     *                      turn is it. True, if it is current
     *                      user's turn, else False.
     *  @return Boolean
s     */
    private static boolean compareHeight(final float myHeight,
                                         final float otherHeight,
                                         final boolean isMyTurn) {

        if (myHeight > otherHeight) {

            return true;
        }

        if ((myHeight == otherHeight) && isMyTurn) {

            return true;
        }

        return false;
    }

    /**
     *  This method compares two Pokemons based on their weights.
     *
     *  @param myWeight Current user's Pokemon weight
     *  @param otherWeight  Other user's Pokemon weight
     *  @param isMyTurn     Boolean variable to indicate whose
     *                      turn is it. True, if it is current
     *                      user's turn, else False.
     *  @return Boolean
     */
    private static boolean compareWeight(final float myWeight,
                                         final float otherWeight,
                                         final boolean isMyTurn) {

        if (myWeight > otherWeight) {

            return true;
        }

        if ((myWeight == otherWeight) && isMyTurn) {

            return true;
        }

        return false;
    }

    /**
     *  This method returns the score of a single type against
     *  multiple opposition types.
     *
     *  e.g., myType : rock
     *        otherTypes : [normal, fighting]
     *        Return : 0.5
     *
     *        myType : fighting
     *        otherTypes : [normal]
     *        Return : 2.0
     *
     *  @param context  Passed context
     *  @param myType   String containing type of current user's
     *                  Pokemon
     *  @param otherTypes   List of strings containing other user's
     *                      Pokemon's types
     *  @return Float score of the type of the current user against
     *          the types of the other user's Pokemon
     */
    private static float getSingleTypeScore(final Context context,
                                            final String myType,
                                            final List<String> otherTypes) {

        StringBuilder key = new StringBuilder();

        key.append(myType + "*");

        for (int index = 0; index < otherTypes.size(); index++) {

            key.append(otherTypes.get(index));

            if (index != otherTypes.size() - 1) {

                key.append("/");
            }
        }

        try {

            String fileName = Constants.TYPE_COMPARISON_FILE_NAME;

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    context.getResources().getAssets().open(fileName),
                    StandardCharsets.UTF_8));

            String data = br.readLine();

            JSONObject json = new JSONObject(data);
            float result = Float.parseFloat(String.valueOf(json.
                    getDouble(key.toString())));

            return result;

        } catch (IOException e) {

            e.printStackTrace();
            return 0.00f;
        } catch (JSONException e) {

            e.printStackTrace();
            return 0.00f;
        }
    }

    /**
     *  This method compares two Pokemons based on their type.
     *  If scores match, then the other three attributes are
     *  used for tie-breaking. The Pokemon who scores maximum in
     *  other three comparisons wins the round.
     *
     *  e.g.,   myTypes : [rock/ghost]
     *          otherTypes : [normal/bug]
     *          myScore : score(rock against [normal/bug]) +
     *                    score(ghost against [normal/bug])
     *                    i.e., 2.0 + 0.0 = 2.0
     *
     *  @param context  Passed context
     *  @param myPokemon    Current user's Pokemon
     *  @param otherPokemon Other user's Pokemon
     *  @param isMyTurn     Boolean variable to indicate whose
     *                      turn is it. True, if it is current
     *                      user's turn, else False.
     *  @return Boolean
     */
    private static boolean compareType(final Context context,
                                       final Pokemon myPokemon,
                                       final Pokemon otherPokemon,
                                       final boolean isMyTurn) {

        List<String> myTypes = myPokemon.getTypes();
        List<String> otherTypes = otherPokemon.getTypes();

        float myScore = 0.00f;
        float opponentScore = 0.00f;

        for (String myType : myTypes) {

            myScore += getSingleTypeScore(context, myType, otherTypes);
        }

        for (String otherType : otherTypes) {

            opponentScore += getSingleTypeScore(context, otherType, myTypes);
        }

        ((GamePage) context).showToast(String.format(context.getResources().
                getString(R.string.typeScoreDisplay), myTypes.toString(),
                otherTypes.toString(), String.valueOf(myScore),
                otherTypes.toString(),
                myTypes.toString(),
                String.valueOf(opponentScore)), Toast.LENGTH_SHORT);

        /* Pause for two sec to show type comparison result.
        *  Note that the current thread is not the UI thread.
        *  So, the UI thread does not go to sleep. */
        try {
            Thread.sleep(Constants.TYPE_RESULT_DISPLAY_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (myScore > opponentScore) {

            return true;
        }

        if (myScore < opponentScore) {

            return false;
        }

        int myAuxiliaryScore = 0;
        int otherAuxiliaryScore = 0;

        if (compareNumber(myPokemon.getNumber(), otherPokemon.getNumber(),
                isMyTurn)) {

            myAuxiliaryScore++;
        } else {

            otherAuxiliaryScore++;
        }

        if (compareHeight(myPokemon.getHeight(), otherPokemon.getHeight(),
                isMyTurn)) {

            myAuxiliaryScore++;
        } else {

            otherAuxiliaryScore++;
        }

        if (compareWeight(myPokemon.getWeight(), otherPokemon.getWeight(),
                isMyTurn)) {

            myAuxiliaryScore++;
        } else {

            otherAuxiliaryScore++;
        }

        if (myAuxiliaryScore > otherAuxiliaryScore) {

            return true;
        }

        return false;
    }
}
