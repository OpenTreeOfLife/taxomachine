package opentree.tnrs.adaptersupport.iplant;

import java.util.ArrayList;
import java.util.Iterator;

public class iPlantResponse implements Iterable<iPlantNameResult> {
    public String status;
    public ArrayList<iPlantNameResult> names;    
    public Metadata metadata;
    
    public Iterator<iPlantNameResult> iterator() {
        return names.iterator();
    }
}

