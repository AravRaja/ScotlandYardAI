package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {

	@Nonnull @Override public String name() { return "AI"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		// returns a random move, replace with your own implementation
		var moves = board.getAvailableMoves().asList();
		List<Move.SingleMove> singleMoves = new ArrayList<>();
		List<Move.DoubleMove> doubleMoves = new ArrayList<>();
		List<Optional<Integer>> detLocs = new ArrayList<>();
		for (Piece.Detective d : Piece.Detective.values()) {
			detLocs.add(board.getDetectiveLocation(d));
		}
		List<Integer> scores = new ArrayList<>();
		for (Move m : moves) {
			if (m instanceof Move.SingleMove) {
				scores.add(board.getSetup().graph.adjacentNodes(((Move.SingleMove) m).destination).size());
			}
			else {
				scores.add(board.getSetup().graph.adjacentNodes(((Move.DoubleMove) m).destination2).size());
			}
		}
		int maxI = scores.indexOf(Collections.max(scores));
		return moves.get(maxI);
	}
}
