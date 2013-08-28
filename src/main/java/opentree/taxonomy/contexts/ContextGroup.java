package opentree.taxonomy.contexts;

import java.util.EnumSet;

public enum ContextGroup {
    LIFE,
    BACTERIA,
    ANIMALS,
    PLANTS,
    FUNGI;
    
    public EnumSet<ContextDescription> getDescriptions() {
        
        EnumSet<ContextDescription> contexts = EnumSet.noneOf(ContextDescription.class);
        
        for (ContextDescription cd : ContextDescription.values())
            if (cd.group == this)
                contexts.add(cd);

        return contexts;
    }
}