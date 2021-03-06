package lpool.logic.match;

import lpool.logic.Game;
import lpool.logic.state.State;
import lpool.network.Message;

public class End implements State<Match> {
	/**
	 * Reason why the match ended.
	 * @author Gustavo
	 *
	 */
	public enum Reason
	{
		BLACK_BALL_SCORED_AS_LAST,
		BLACK_BALL_SCORED_ACCIDENTALLY,
		TIMEOUT,
		DISCONNECT
	}
	private Reason reason;
	private int winner;

	public End(Reason reason, int winner) {
		this.reason = reason;
		this.winner = winner;
	}

	@Override
	public void enter(Match match) {
		match.logEnd(winner);
		match.sendStateToClients();
		match.deleteCollisionObserver(match);
		match.getWorld().dispose();
		for (int i = 0; i < Game.numPlayers; i++)
		{
			match.getNetwork().send(new Message(i, Game.ProtocolCmd.KICK.ordinal()));
		}
	}

	@Override
	public void update(Match match, float dt) {
		System.out.println("I'm ended");
	}

	/**
	 * 
	 * @return The winner of the match.
	 */
	public int getWinner() {
		return winner;
	}

	/**
	 * 
	 * @return The reason why the match ended.
	 */
	public Reason getReason() {
		return reason;
	}

	@Override
	public void exit(Match owner) {
	}
}
