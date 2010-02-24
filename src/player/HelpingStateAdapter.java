package player;
import java.util.Collection;

import org.eclipse.palamedes.gdl.core.model.utils.AbstractState;
import org.eclipse.palamedes.gdl.core.resolver.prologprover.EclipseConnector;
import org.eclipse.palamedes.gdl.core.resolver.prologprover.FluentAdapter;

import com.parctechnologies.eclipse.CompoundTerm;

public class HelpingStateAdapter extends AbstractState<Collection<CompoundTerm>> {
	
	private EclipseConnector eclipse;
	
    public HelpingStateAdapter(EclipseConnector eclipse, Collection<CompoundTerm> objState) {
        super(objState);
        this.eclipse=eclipse;
    }

    @Override
    public void initFluentList() {
        for ( CompoundTerm prologFluent : nativeState )
            fluents.add(new FluentAdapter(prologFluent));
    }

    @Override
    public String toString() {
        return eclipse.toGdlString(nativeState);
    }
}
