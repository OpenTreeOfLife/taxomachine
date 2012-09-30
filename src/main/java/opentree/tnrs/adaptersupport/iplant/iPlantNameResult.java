package opentree.tnrs.adaptersupport.iplant;

import java.util.ArrayList;
import java.util.Iterator;

public class iPlantNameResult implements Iterable<iPlantHit> {
    public int matchCount;
    public ArrayList<iPlantHit> matches;
    public String submittedName;
    
    public Iterator<iPlantHit> iterator() {
        return matches.iterator();
    }
}