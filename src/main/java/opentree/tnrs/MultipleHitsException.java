package opentree.tnrs;

public class MultipleHitsException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    String error;

    public MultipleHitsException() {
        super();
        error = "";
    }

    public MultipleHitsException(String err) {
        super(err);
        error = err;
    }

    public String getError() {
        return error;
    }
}
