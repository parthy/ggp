package player;


import org.eclipse.palamedes.gdl.core.model.IGameNode;

public interface IHeuristic {
	public int calculateHeuristic(IGameNode node);
}
