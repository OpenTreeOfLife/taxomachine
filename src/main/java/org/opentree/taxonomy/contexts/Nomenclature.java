package org.opentree.taxonomy.contexts;

public enum Nomenclature {
//  Name         code       description    
    ICN        ("ICN",    "plants, fungi, and some protists?"),
    ICNP        ("ICNP",    "bacteria"),
    ICZN        ("ICZN",    "animals"),
    Undefined   ("undefined",        "governing code unclear, nonexistent, or multiple codes");
    
    public final String code;
    public final String description;

    Nomenclature (String code, String description) {
        this.code = code;
        this.description = description;
    }
}
