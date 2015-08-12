package src.np2015;

public interface GuardedCommand {
	double getRateForTarget(final int x, final int y, Neighbor where);
}
