package player;


import org.eclipse.palamedes.gdl.core.model.IGameNode;

public interface IHeuristic {
	public double calculateHeuristic(IGameNode node);
}