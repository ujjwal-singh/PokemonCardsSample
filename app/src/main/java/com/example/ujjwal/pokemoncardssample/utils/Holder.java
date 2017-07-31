package com.example.ujjwal.pokemoncardssample.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;

/**
 *  Created by ujjwal on 31/7/17.
 *  This class acts as a holder for generic Object type.
 *  Used for altering values in a thread.
 */
@Getter
@Setter
@AllArgsConstructor
public class Holder {

    /** Generic object. */
    private Object value;
}
