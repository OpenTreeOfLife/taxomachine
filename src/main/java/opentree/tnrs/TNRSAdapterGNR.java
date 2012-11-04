package opentree.tnrs;

/**
 * An adapter used by TNRSQuery to perform searches against the Global Names Resolver. This class is basically
 * obsolete and is on indefinite development hold because the GNR is way too slow. It will need to be mostly
 * re-written if it is ever re-implemented, including switching the REST client to the Apache libraries, and
 * accepting arguments as specified by the TNRSAdapter interface.
 * 
 * @author Cody Hinchliff
 *
 */
public class TNRSAdapterGNR /* implements TNRSAdapter */ {

    /*  

    String url = "http://resolver.globalnames.org/name_resolvers.json";

    public void doQuery(HashSet<String> searchStrings) {
        int id = 0; // just a placeholder

        System.out.println("gnr checkpoint 1");

        // build the query
        String queryString = "{\"data\":\"";
        boolean isFirst = true;
        for (String s : searchStrings) {
            if (isFirst)
                isFirst = false;
            else
                queryString += "\n";

            queryString += id + "|" + s;
        }
        queryString += "\"}";

        System.out.println(queryString);

        // set up the connection to GNR
        ClientConfig cc = new DefaultClientConfig();
        Client c = Client.create(cc);
        WebResource gnr = c.resource(url);

        // send the query (get the response)
        String respJSON = gnr.accept(MediaType.APPLICATION_JSON_TYPE).type(MediaType.APPLICATION_JSON_TYPE).post(String.class, queryString);

        // System.out.println(respJSON);

        // parse the JSON response
        GNRResponse response = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            response = mapper.readValue(respJSON, GNRResponse.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("gnr checkpoint 2");

        for (GNRNameResult thisNameResult : response) {
            System.out.println(thisNameResult.supplied_name_string);
            for (GNRMatch thisMatch : thisNameResult) {
                String matchedName = thisMatch.canonical_form;
                boolean thisNameMatched = false;
                if (matchedName != null) {
                    Node matchedNode = _taxonomy.findTaxNodeByName(thisMatch.canonical_form);
                    if (matchedNode != null) {
                        thisNameMatched = true;
                        thisMatch.matchedNode = matchedNode;
                        thisMatch.searchString = thisNameResult.supplied_name_string;
                        _results.addMatch(thisMatch);
                    } else {
                        // check if it matches a synonym
                    }
                }
                if (thisNameMatched) {
                    for (String s : _unmatchedNames) {
                        if (s.compareTo(matchedName) == 0) {
                            _unmatchedNames.remove(s);
                        }
                    }
                }
            }
        }
    } */
}