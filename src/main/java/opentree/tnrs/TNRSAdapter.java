package opentree.tnrs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.jackson.JsonParseException;

public abstract class TNRSAdapter {
    
    // unclear what will need to be included here

    public abstract void doQuery(HashSet<String> searchStrings);
}
