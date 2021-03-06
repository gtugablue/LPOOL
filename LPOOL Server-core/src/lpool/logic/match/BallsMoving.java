package lpool.logic.match;

import lpool.logic.ball.Ball;
import lpool.logic.state.State;
import lpool.logic.state.TransitionState;

public class BallsMoving implements State<Match>{

	@Override
	public void enter(Match match) {
		match.sendStateToClients();
	}

	@Override
	public void update(Match match, float dt) {
		match.worldStep(dt);
		match.deleteRemovedBalls();
		match.tickBalls(dt);
		if (allStopped(match.getBalls()))
			continueMatch(match);
	}

	private boolean allStopped(Ball[] balls)
	{
		for (int i = 0; i < balls.length; i++)
		{
			if (!balls[i].isStopped())
				return false;
		}
		return true;
	}

	private void continueMatch(Match match)
	{
		if (matchEnded(match))
		{
			match.getStateMachine().changeState(new TransitionState<Match>(match.getStateMachine(), this, new End(whyMatchEnded(match), whoWon(match))));
			return;
		}
		match.incrementPlayNum();
		if (match.isBallInHand())
		{
			match.changeCurrentPlayer();
			match.getStateMachine().changeState(new TransitionState<Match>(match.getStateMachine(), this, new CueBallInHand()));
		}
		else
		{
			if (match.currentPlayerPlaysAgain())
				match.getStateMachine().changeState(new Play());
			else
			{
				match.changeCurrentPlayer();
				match.getStateMachine().changeState(new TransitionState<Match>(match.getStateMachine(), this, new Play()));
			}
		}
	}

	private boolean matchEnded(Match match)
	{
		return !match.getBlackBall().isOnTable();
	}
	
	private int whoWon(Match match)
	{
		if (match.isBallInHand())
			return match.getCurrentPlayer() == 0 ? 1 : 0;
		else
			return match.getCurrentPlayer();
	}

	private End.Reason whyMatchEnded(Match match)
	{
		if (match.isBallInHand())
			return End.Reason.BLACK_BALL_SCORED_ACCIDENTALLY;
		else
			return End.Reason.BLACK_BALL_SCORED_AS_LAST;
	}

	@Override
	public void exit(Match owner) {
	}
}
