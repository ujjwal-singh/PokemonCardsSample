package com.example.ujjwal.pokemoncardssample.utils;

import java.util.ArrayList;
import java.util.Random;

/**
 *  This class is used to generate random
 *  sequence of integers.
 *  @author ujjwal
 */
public final class RandomSequence {

    /**
     *  This method returns a random sequence of
     *  integers (of length count) bounded by min
     *  and max (both inclusive, i.e, [min, max]).
     *  There should be no repetition in the ArrayList,
     *  i.e., each number should occur exactly once.
     *  @param min  Integer, Lower limit of the values.
     *  @param max  Integer, Upper limit of the values.
     *  @param count    Integer, Number of items in the
     *                           sequence.
     *  @return ArrayList of the integers.
     */
    public static ArrayList<Integer> generateRandomSequence(
            final int min, final int max, final int count) {

        Random random = new Random();
        ArrayList<Integer> randomSequence = new ArrayList<>();

        while (randomSequence.size() < count) {

            int nextRandom = random.nextInt((max - min) + 1) + min;
            if (!randomSequence.contains(nextRandom)) {
                randomSequence.add(nextRandom);
            }
        }

        return randomSequence;
    }

    /**
     *  Util classes should not be initialized.
     *  Prevents initialization.
     */
    private RandomSequence() {

    }
}
