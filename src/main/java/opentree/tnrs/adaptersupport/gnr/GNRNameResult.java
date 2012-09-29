package opentree.tnrs.adaptersupport.gnr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GNRNameResult implements Iterable<GNRMatch> {
    public String supplied_name_string;
    public String supplied_id;
    public List<GNRMatch> results = new ArrayList<GNRMatch>();

    public Iterator<GNRMatch> iterator() {
        return results.iterator();
    }
}
