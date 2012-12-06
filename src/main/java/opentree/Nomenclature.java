package opentree;

public enum Nomenclature {
//  Name         code       description    
    ICBN        ("ICBN",    "plants, fungi, and some protists?"),
    ICNB        ("ICNB",    "bacteria"),
    ICZN        ("ICZN",    "animals"),
    Undefined   ("",        "governing code unclear, nonexistent, or multiple codes");
    
    public final String code;
    public final String description;

    Nomenclature (String code, String description) {
        this.code = code;
        this.description = description;
    }
}
