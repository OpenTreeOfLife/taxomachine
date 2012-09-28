package opentree.tnrs.adaptersupport.gnr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GNRResponse implements Iterable<NameResult> {
    public String id;
    public String url;
    public List<String> data_sources = new ArrayList<String>();
    public List<Context> context = new ArrayList<Context>();
    public List<NameResult> data = new ArrayList<NameResult>();
    public String status;
    public String message;
    public Parameters parameters;

    public Iterator<NameResult> iterator() {
        return data.iterator();
    }
}