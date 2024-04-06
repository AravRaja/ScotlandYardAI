package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.sun.source.tree.Tree;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {
	static final int WIN_SCORE = 10;

	@Nonnull @Override public String name() { return "MCTS_MrxAi"; }

	private gameTreeNode selectPromisingNode(gameTreeNode root) {
		gameTreeNode node = root;
		while (!node.getChildren().isEmpty()) {
			node = UCT.findBestNodeWithUCT(node);
		}
		return node;
	}

	private void expandNode(gameTreeNode node) {
		List<Card> possibleCards = node.getCard().potentialCards();
		possibleCards.forEach(Card -> {
			gameTreeNode newNode = new gameTreeNode(Card,node);
			newNode.getCard().setPlayerNo(Math.abs(1-node.getCard().getPlayerNo()));
			node.addChild(newNode);
		});
	}

	private void backPropogation(gameTreeNode nodeToExplore, int playerNo) {
		gameTreeNode tempNode = nodeToExplore;
		while (tempNode != null) {
			tempNode.getCard().incrementVisit();
			if (tempNode.getCard().getPlayerNo() == playerNo) {
				tempNode.getCard().addScore(1);
			}
			tempNode = tempNode.getParent();
		}
	}
	private int simulateRandomPlayout(gameTreeNode node) {
		gameTreeNode tempNode = new gameTreeNode(node.getCard(),node.getParent());
		Card tempCard = tempNode.getCard();
		if (!tempCard.getState().getWinner().contains(Piece.MrX.MRX) && !tempCard.getState().getWinner().isEmpty()) {
			tempNode.getParent().getCard().setScore(-1);
			return 0;
		}
		while (tempCard.getState().getWinner().isEmpty()) {
			tempCard.setPlayerNo(Math.abs(1-tempCard.getPlayerNo()));
			tempCard.randomAdvance();
		}
		if (tempCard.getState().getWinner().contains(Piece.MrX.MRX)) {
			return 1;
		}
		else {
			return 0;
		}
	}

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		// returns a random move, replace with your own implementation
		var moves = board.getAvailableMoves().asList();
		Board.GameState state = (Board.GameState) board;
		Card initalCard = new Card(state,null);
		initalCard.setPlayerNo(0);

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

		long start = System.currentTimeMillis();
		long end = start + 5 * 1000;

		gameTreeNode rootNode =  new gameTreeNode(initalCard,null);
		gameTree tree = new gameTree(rootNode);

		while (System.currentTimeMillis() < end) {
			gameTreeNode promisingNode = selectPromisingNode(rootNode);
			if (promisingNode.getCard().getState().getWinner().isEmpty()) {
				expandNode(promisingNode);
			}
			gameTreeNode nodeToExplore = promisingNode;
			if (!promisingNode.getChildren().isEmpty()) {
				nodeToExplore = promisingNode.getRandomChildNode();
			}
			int playoutResult = simulateRandomPlayout(nodeToExplore);
			backPropogation(nodeToExplore, playoutResult);
		}

		gameTreeNode winnerNode = rootNode.getChildWithMaxScore();
		tree.setRoot(winnerNode);
        return winnerNode.getCard().getMoveTo();
	}
}
