
package com.hourglassapps.cpi_ii.snowball.tartarus;
import java.lang.reflect.InvocationTargetException;

import org.tartarus.snowball.SnowballProgram;

public abstract class SnowballStemmer extends SnowballProgram {
    public abstract boolean stem();
};
