package com.example.ujjwal.pokemoncardssample.utils;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

/**
 *  Created by ujjwal on 19/7/17.
 *  Class to calculate hash.
 */
public final class HashCalculator {

    /**
     *  This method returns the MD5 hash of a String.
     *  The output is a 32 char long string.
     *  @param key  String whose hash is to be calculated.
     *  @return The MD5 hash.
     */
    public static String getMD5Hash(final String key) {

        return (new String(Hex.encodeHex(DigestUtils.md5(key))));
    }

    /**
     *  Util classes should not be initialized.
     *  Prevents initialization.
     */
    private HashCalculator() {

    }
}
