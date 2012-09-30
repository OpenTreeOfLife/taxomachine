package opentree.tnrs.adaptersupport.gnr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GNRNameResult implements Iterable<GNRHit> {
    public String supplied_name_string;
    public String supplied_id;
    public List<GNRHit> results = new ArrayList<GNRHit>();

    public Iterator<GNRHit> iterator() {
        return results.iterator();
    }
}
