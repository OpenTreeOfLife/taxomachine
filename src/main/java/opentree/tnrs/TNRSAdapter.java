package opentree.tnrs;

import java.io.IOException;
import java.util.ArrayList;

import org.codehaus.jackson.JsonParseException;

public abstract class TNRSAdapter {
    
    // unclear what will need to be included here

    public abstract void doQuery(ArrayList<String> searchStrings);
}
