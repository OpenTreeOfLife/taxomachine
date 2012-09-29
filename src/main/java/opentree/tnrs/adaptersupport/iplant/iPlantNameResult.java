package opentree.tnrs.adaptersupport.iplant;

import java.util.ArrayList;
import java.util.Iterator;

public class iPlantNameResult implements Iterable<iPlantMatch> {
    public int matchCount;
    public ArrayList<iPlantMatch> matches;
    public String submittedName;
    
    public Iterator<iPlantMatch> iterator() {
        return matches.iterator();
    }
}