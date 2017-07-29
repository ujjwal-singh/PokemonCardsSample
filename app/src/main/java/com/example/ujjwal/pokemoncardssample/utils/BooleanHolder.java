package com.example.ujjwal.pokemoncardssample.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;

/**
 *  Created by ujjwal on 20/7/17.
 *  This class acts as a holder for a boolean type.
 *  Used for altering values in a thread.
 */
@Getter
@Setter
@AllArgsConstructor
public class BooleanHolder {

    /** Value of the boolean variable. */
    private boolean value;
}
