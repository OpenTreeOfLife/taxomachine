package opentree;

/**
 * Defines all the indexes that are used by (and accessible from) the TaxonomyContext objects. Each one of these name prefixes is combined with the a name suffix from the Taxonomy.ContextDescription enum to yield a complete index name.
 * @author cody hinchliff
 *
 */
public enum NodeIndexDescription {
    TAXON_BY_NAME               ("taxNodesByName"),         // all taxon nodes by name
    TAXON_BY_SYNONYM            ("taxNodesBySyn"),          // all taxon nodes by synonym
    PREFERRED_TAXON_BY_NAME     ("prefTaxNodesByName"),     // taxon nodes with preferred relationships by name
    PREFERRED_TAXON_BY_SYNONYM  ("prefTaxNodesBySyn"),      // taxon nodes with preferred relationships by synonym
    PREFERRED_TAXON_BY_NAME_OR_SYNONYM  ("prefTaxNodesByNameOrSyn"),
    
    TAX_SOURCES                 ("taxSources"),             // ?
    TAX_STATUS                  ("taxStatus"),
    TAX_RANK                    ("taxRank"),
    
    TAXON_UNIQID				("taxUniqId");

    public final String namePrefix;

    NodeIndexDescription(String namePrefix) {
        this.namePrefix = namePrefix;
    }
}