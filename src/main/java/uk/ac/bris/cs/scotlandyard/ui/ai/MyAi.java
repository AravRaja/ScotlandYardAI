package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import com.sun.source.tree.Tree;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {

	int movesPicked = -1;
	Set<Integer> ferryNodes = ImmutableSet.of(157, 194, 115, 108);

	List<Integer> detectiveLocations = new ArrayList<>();


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
		possibleCards = filterCards(possibleCards);
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

	public Boolean allEdgeTaxiOnly(int node, Board state) {
		for (int n : state.getSetup().graph.adjacentNodes(node)) {
			if (!(state.getSetup().graph.edgeValueOrDefault(node,n,null).contains(ScotlandYard.Ticket.TAXI) && state.getSetup().graph.edgeValueOrDefault(node,n,null).size() == 1)) {
				return false;
			}
		}
		return true;
	}

	public List<Card> filterCards(List<Card> cards) {
		List<Card> copy = new ArrayList<>(cards);
		Board.GameState state = cards.get(0).getState();
		Move moveTo = cards.get(0).getMoveTo();
		for (Card c : cards) {
			Move m = c.getMoveTo();
			for (ScotlandYard.Ticket t : m.tickets()) {
				if (t.equals(ScotlandYard.Ticket.SECRET)) {
					if (!ferryNodes.contains(moveTo.source())) {
						if (allEdgeTaxiOnly(moveTo.source(), state) || movesPicked < 2 || state.getSetup().moves.get(movesPicked)) { //prevents secret ticket use in first 2 moves, if all moves form current node to an adjacent node are taxi or if its on a surfacing move
							copy.remove(c); //removes illegal secret moves to avoid unnecessary exploration
						}
					}
				}
				else if (t.equals(ScotlandYard.Ticket.DOUBLE)) { //prevents use of double unless a detective is in an adjacent node to a move mrX can make
					Boolean canBeCaught = false;
					Boolean exit = false;
					for (Integer i : detectiveLocations) {
						if (exit == true) { break; }
						if (c.getState().getSetup().graph.adjacentNodes(m.source()).contains(i)) {
							canBeCaught = true;
							break;
						}
						for (Integer j : c.getState().getSetup().graph.adjacentNodes(i)) {
							if (c.getState().getSetup().graph.adjacentNodes(m.source()).contains(j)) {
								canBeCaught = true;
								exit = true;
							}
						}
					}
					if (!canBeCaught) {
						copy.remove(c);
					}
				}
			}
		}
		return copy;
	}

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {

		if (board.getMrXTravelLog().isEmpty()) { movesPicked = -1; }
		movesPicked += 1;
		var moves = board.getAvailableMoves().asList();
		Board.GameState state = (Board.GameState) board;
		Card initalCard = new Card(state,null);
		initalCard.setPlayerNo(0);

		detectiveLocations.clear();
		for (Piece.Detective d : Piece.Detective.values()) {
			detectiveLocations.add(board.getDetectiveLocation(d).orElse(null));
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
		long softEnd = start + 1 * 1000;
		long hardEnd = start + 15 * 1000;

		gameTreeNode rootNode =  new gameTreeNode(initalCard,null);
		gameTree tree = new gameTree(rootNode);

		int pncount = 0;
		while (System.currentTimeMillis() < softEnd || pncount < 1000) {
			pncount += 1;
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

        Move winMove = winnerNode.getCard().getMoveTo();
		return winMove;
	}
}
