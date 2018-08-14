package za.ac.sun.cs.green.util;

import java.util.logging.Logger;

/*
 * NullLogger to enable the new code
 */
public class NullLogger extends Logger {
    public NullLogger() {
        super("", "");
    }
}
