package opentree.tnrs.adapters.gnr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GNRResponse implements Iterable<GNRNameResult> {
    public String id;
    public String url;
    public List<String> data_sources = new ArrayList<String>();
    public List<Context> context = new ArrayList<Context>();
    public List<GNRNameResult> data = new ArrayList<GNRNameResult>();
    public String status;
    public String message;
    public Parameters parameters;

    public Iterator<GNRNameResult> iterator() {
        return data.iterator();
    }
}