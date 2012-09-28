package opentree.tnrs.adaptersupport.gnr;

import java.util.HashMap;

import org.neo4j.graphdb.Node;

import opentree.tnrs.TNRSMatchParser;

public class GNRMatch implements TNRSMatchParser {
    public String data_source_id;
    public String gni_uuid;
    public String name_string;
    public String canonical_form;
    public String classification_path;
    public String classification_path_ids;
    public String taxon_id;
    public String local_id;
    public String match_type;
    public String global_id;
    public String url;
    public String prescore;
    public String score;
    public String searchString = null;
    public Node matchedNode = null;
    
    public Node getMatchedNode() {
        return matchedNode;
    }

    public Node getSynonymNode() {
        // gnr does not return any synonym information. thus, to implement this,
        // we need to be able to look up synonyms in the graph
        return null;
    }

    public String getSearchString() {
        return searchString;
    }

    public String getSourceName() {
        return "gnr";
    }

    public boolean getIsExactNode() {
        return false;
    }
    
    public boolean getIsApprox() {
        return match_type.compareTo("1") != 0;
    }
    
    public boolean getIsSynonym() {
        // gnr does not return any synonym information. thus, to implement this,
        // we need to be able to look up synonyms in the graph
        return false;
    }
    
    public double getScore() {
        return Double.parseDouble(score);
    }
    
    public HashMap<String, String> getOtherData() {
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("data_source_id",data_source_id);
        data.put("name_string",name_string);
        data.put("canonical_form",canonical_form);
        data.put("classification_path",classification_path);
        data.put("classification_path_ids",classification_path_ids);
        data.put("taxon_id",taxon_id);
        data.put("local_id",local_id);
        data.put("match_type",match_type);
        data.put("global_id",global_id);
        data.put("url",url);
        data.put("prescore",prescore);
        data.put("score",score);
        return data;
    }
}