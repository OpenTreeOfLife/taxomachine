package opentree;

public class ContextNotFoundException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    String error;
    String contextName;

    public ContextNotFoundException() {
        super();
        error = "the specified context could not be found";
    }

    public ContextNotFoundException(String contextName) {
        super();
        error = "the context '" + contextName + "' could not be found";
        this.contextName = contextName;
    }
    
    public String getError() {
        return error;
    }
}
