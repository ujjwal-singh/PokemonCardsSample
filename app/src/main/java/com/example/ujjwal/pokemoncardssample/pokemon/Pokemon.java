package com.example.ujjwal.pokemoncardssample.pokemon;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.example.ujjwal.pokemoncardssample.Constants;
import com.example.ujjwal.pokemoncardssample.R;
import com.example.ujjwal.pokemoncardssample.utils.JsonKey;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;

import lombok.Getter;

/**
 *  This class acts as the model class for Pokemons !!
 *  @author ujjwal
 */
@Getter
public class Pokemon {

    /** The number of the Pokemon in Kanto region.
     *  e.g., 1 for Bulbasaur */
    private int number;

    /** The name of the Pokemon.
     *  e.g., "Bulbasaur" for Bulbasaur */
    private String name;

    /** The height of the Pokemon, in metres.
     *  e.g., 0.70 for Bulbasaur */
    private float height;

    /** The weight of the Pokemon, in kg.
     *  e.g., 6.9 for Bulbasaur */
    private float weight;

    /** List to store the types of the pokemon.
     *  e.g., [Grass,Poison] for Bulbasaur */
    private List<String> types;

    /** Stores the ID (number) of the base
     *  Pokemon of the current Pokemon.
     *  e.g., 1 for Bulbasaur
     *        1 for Ivysaur */
    private int basePokemon;

    /** Stores image of the current Pokemon. */
    private Drawable image;

    /** Stores the context passed by the calling activity. */
    private Context context;

    /**
     *  The constructor for Pokemon class.
     *  @param passedContext    Context, passed by
     *                          the calling activity.
     *  @param pokemonNumber    Pokemon Number
     *                          e.g., 1 for Bulbasaur
     */
    public Pokemon(final Context passedContext,
                   final int pokemonNumber) {

        this.context = passedContext;
        this.number = pokemonNumber;
        setDetails();
    }

    /**
     *  This method sets other details of the current
     *  Pokemon, which are not set byb the Constructor.
     */
    private void setDetails() {

        try {
            /*
             *  File containing details of Pokemon, in JSON format.
             */
            String fileName = getFileName() + Constants.
                    EXTENSION_OF_DETAILS_FILE;
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    context.getResources().getAssets().open(fileName),
                    StandardCharsets.UTF_8));

            StringBuilder pokemonDetails = new StringBuilder();
            String currentLine = br.readLine();
            while (currentLine != null) {
                pokemonDetails.append(currentLine);
                currentLine = br.readLine();
            }
            br.close();

            JSONObject details = new JSONObject(pokemonDetails.toString());
            this.name = details.get(JsonKey.POKEMON_NAME.
                    getKey()).toString();
            this.height = Float.parseFloat(details.get(JsonKey.POKEMON_HEIGHT.
                    getKey()).toString());
            this.weight = Float.parseFloat(details.get(JsonKey.POKEMON_WEIGHT.
                    getKey()).toString());

            /*
             *  Example of types:-
             *
             *      "types": [
             *                  "grass",
             *                  "poison"
             *               ]
             */
            this.types = new ArrayList<String>();

            String[] pokemonTypes = (details.get(JsonKey.POKEMON_TYPES.
                    getKey()).toString()).split("\"");
            for (int i = 1; i < pokemonTypes.length; i += 2) {
                this.types.add(pokemonTypes[i]);
            }

            /*
             *  Example of chain:-
             *      "chain": [
             *                    {
             *                       "number": 1,
             *                       "name": "Bulbasaur"
             *                   }              ,
             *               ]
             */
            JSONObject chainInformation = ((JSONArray) (details.
                    get(JsonKey.POKEMON_CHAIN.getKey()))).getJSONObject(0);
            this.basePokemon = Integer.parseInt(chainInformation.
                    get(JsonKey.POKEMON_NUMBER.getKey()).toString());

            /*
             *  File containing image of the Pokemon.
             */
            String imageFileName = "icon" + getFileName();
            String uri = "@drawable/" + imageFileName;
            int imageResource = context.getResources().
                    getIdentifier(uri, null, context.getPackageName());
            /*
             *  Deprecated function call is used to support
             *  lower API levels (below level 21).
             */
            this.image = context.getResources().getDrawable(imageResource);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     *  This method builds up and returns a String
     *  which is the file name for the file containing
     *  details about the current Pokemon, or the image
     *  of the Pokemon.
     *  @return String FileName of the file containing
     *                 details about the current Pokemon,
     *                 or the image of the Pokemon.
     *                 "001" for Bulbasaur
     *                 "025" for Pikachu
     *                 "123" for Scyther
     */
    private String getFileName() {

        /*
         *  0 : to pad with zeroes
         *  3 : to set width to 3
         */
        return String.format("%03d", this.number);
    }

    /**
     *  Overriding toString method.
     *  @return String containing Pokemon's
     *          id and name.
     */
    @Override
    public String toString() {

        return (String.format(context.getResources().
                getString(R.string.pokemonIdAndName),
                String.valueOf(this.number), this.name));
    }
}
