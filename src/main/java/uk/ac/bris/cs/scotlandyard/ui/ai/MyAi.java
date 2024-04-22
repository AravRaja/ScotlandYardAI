package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {

	int movesPicked = 0;
	Set<Integer> ferryNodes = ImmutableSet.of(157, 194, 115, 108);

	List<Integer> detectiveLocations = new ArrayList<>();

	List<Optional<Board.TicketBoard>> detectiveTickets = new ArrayList<Optional<Board.TicketBoard>>();


	@Nonnull @Override public String name() { return "MCTS_MrxAi"; }

	public static class Destination implements Move.Visitor<Integer> {

		@Override
		public Integer visit(Move.SingleMove move) {
			//returns final location after single move
			return move.destination;
		}
		@Override
		public Integer visit(Move.DoubleMove move) {
			//returns final location after double move
			return move.destination2;
		}
	}

	//the following functions (selectPromisingNode, expandNode, backPropagation, simulateRandomPlayout) are influenced and adapted from https://www.baeldung.com/java-monte-carlo-tree-search

	private gameTreeNode selectPromisingNode(gameTreeNode root) { //employs the UCB1/UCT formula to select a promising node to expand
		gameTreeNode node = root;
		while (!node.getChildren().isEmpty()) {
			node = UCT.findBestNodeWithUCT(node);
		}
		return node;
	}

	private void expandNode(gameTreeNode node) { //adds children to a given node from the possible next moves (filters out double moves and secret moves dependant)
		List<Card> possibleCards = node.getCard().potentialCards();
		possibleCards = filterCards(possibleCards);
		possibleCards.forEach(Card -> {
			gameTreeNode newNode = new gameTreeNode(Card,node);
			newNode.getCard().setPlayerNo(Math.abs(1-node.getCard().getPlayerNo()));
			node.addChild(newNode);
		});
	}

	private void backPropagation(gameTreeNode nodeToExplore, int playerNo) { //adds score to potential moves
		gameTreeNode tempNode = nodeToExplore;
		while (tempNode != null) {
			tempNode.getCard().incrementVisit();
			if (tempNode.getCard().getPlayerNo() == playerNo) {
				tempNode.getCard().addScore(1);
			}
			tempNode = tempNode.getParent();
		}
	}
	private int simulateRandomPlayout(gameTreeNode node, Pair<Long, TimeUnit> timeoutPair) { //plays out a random(+e-greedy) game from a selected node and calculates who wins from it
		gameTreeNode tempNode = new gameTreeNode(node.getCard(),node.getParent());
		Card tempCard = tempNode.getCard();
		if (!tempCard.getState().getWinner().contains(Piece.MrX.MRX) && !tempCard.getState().getWinner().isEmpty()) {
			tempNode.getParent().getCard().setScore(-1);
			return 0;
		}
		while (tempCard.getState().getWinner().isEmpty()) {
			tempCard.setPlayerNo(Math.abs(1-tempCard.getPlayerNo()));
			int e;
			if (tempCard.isMrxTurn()) { e = 10; } //implements an e-greedy optimisation which instead of a random move being chosen a move is picked based on a heuristic with probability e
			else { e = 5; } //for mrx e-greedy should happen 1/10 times and for detectives 1/5
			Random rand = new Random();
			for (int i = 0; i < detectiveLocations.size(); i++) {
				if (1 == rand.nextInt(0,e)) { //does e-greedy for every single move (not all detectives or mrx but each detective or mrx)
					tempCard.calculatedAdvance(timeoutPair, detectiveLocations);
				}
				else {
					tempCard.randomAdvance();
				}
				if (tempCard.isMrxTurn()) { break; } //will only do 1 if it was mrx and will switch to a detective turn card
			}
		}
		if (tempCard.getState().getWinner().contains(Piece.MrX.MRX)) {
			return 1;
		}
		else {
			return 0;
		}
	}

	//research for the Monte Carlo tree search and ideas to improve it are influenced by this paper - https://dke.maastrichtuniversity.nl/m.winands/documents/TCAIG_ScotlandYard.pdf
	//this includes the ideas for filtering the cards and the use of e-greedy in MCTS

	public Boolean allEdgeTaxiOnly(int node, Board state) { //checks whether a given node only has taxi routes available from it
		for (int n : state.getSetup().graph.adjacentNodes(node)) {
			if (!(state.getSetup().graph.edgeValueOrDefault(node,n,null).contains(ScotlandYard.Transport.TAXI))) {
				return false;
			}
		}
		return true;
	}

	public List<Card> filterCards(List<Card> cards) {
		List<Card> copy = new ArrayList<>(cards);
		Board.GameState state = cards.get(0).getState();
		Move moveTo = cards.get(0).getMoveTo();
		Destination dest = new Destination();
		boolean canBeCaught = false;
		boolean exit = false;
		for (Integer i : detectiveLocations) { //calculates if mrX "canBeCaught" (which decides if double moves should be removed or not) i.e. he can move to a square that is an adjacent node to a detective - done prior to iterating through potential moves to save time
			if (i == null) { continue; }
			if (exit) { break; }
			for (Integer j : state.getSetup().graph.adjacentNodes(i)) {
				if (exit) { break; }
				if (state.getSetup().graph.adjacentNodes(moveTo.source()).contains(j)) {
					canBeCaught = true;
					exit = true;
				}
			}
		}
		for (Card c : cards) {
			Move m = c.getMoveTo();
			if (copy.size() > 1) { // will remove moves if another move is still available
				int destination = m.accept(dest);
				int detNo = 0;
				exit = false;
				for (Integer i : detectiveLocations) { //calculates if the potential move is to a square that a detective has a ticket to move into
					if (i == null) { continue; }
					if (exit) { break; }
					for (Integer j : state.getSetup().graph.adjacentNodes(i)) {
						if (destination == j && detectiveTickets.get(detNo).get().getCount(state.getSetup().graph.edgeValueOrDefault(destination, i, null).asList().get(0).requiredTicket()) >= 1) {
							copy.remove(c);
							exit = true;
							break;
						}
					}
					detNo += 1;
				}
			}
			for (ScotlandYard.Ticket t : m.tickets()) {
				if (t.equals(ScotlandYard.Ticket.SECRET)) {
					if (!ferryNodes.contains(moveTo.source())) { //ignores filtering of secret moves if a ferry cna be taken
						if (allEdgeTaxiOnly(moveTo.source(), state) || movesPicked < 2 || state.getSetup().moves.get(movesPicked)) { //prevents secret ticket use in first 2 moves, if all moves form current node to an adjacent node are taxi or if its on a surfacing move
							copy.remove(c); //removes illegal secret moves to avoid unnecessary exploration
						}
					}
				}
				else if (t.equals(ScotlandYard.Ticket.DOUBLE)) { //prevents use of double unless a detective is in an adjacent node to a move mrX can make
					if (!canBeCaught) { copy.remove(c); }
				}
			}
		}
		return copy;
	}

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {

		if (board.getMrXTravelLog().isEmpty()) { movesPicked = 0; } //if 2 games run back-to-back in GUI movesPicked needs to reset as it doesn't automatically go back to 0
		Board.GameState state = (Board.GameState) board;
		Card initalCard = new Card(state,null);
		initalCard.setPlayerNo(0);

		detectiveLocations.clear(); //gets rid of last moves detective locations
		detectiveTickets.clear(); //gets rid of last moves detective tickets
		for (Piece.Detective d : Piece.Detective.values()) { // gets detectives
			detectiveLocations.add(board.getDetectiveLocation(d).orElse(null)); //adds location
			detectiveTickets.add(board.getPlayerTickets(d)); //adds tickets
		}


		long start = System.currentTimeMillis();
		long end = start + 15 * 1000; //allows us to time how many random playouts are calculated so pickMove doesn't time out in the game
		gameTreeNode rootNode =  new gameTreeNode(initalCard,null);

		int pncount = 0;
		while (System.currentTimeMillis() < end && pncount < 10000) { //main loop for calculating the scores or each move, set to stop if enough iterations or a time restriction is met
			pncount += 1;
			gameTreeNode promisingNode = selectPromisingNode(rootNode);
			if (promisingNode.getCard().getState().getWinner().isEmpty()) {
				expandNode(promisingNode);
			}
			gameTreeNode nodeToExplore = promisingNode;
			if (!promisingNode.getChildren().isEmpty()) {
				nodeToExplore = promisingNode.getRandomChildNode();
			}
			int playoutResult = simulateRandomPlayout(nodeToExplore, timeoutPair);
			backPropagation(nodeToExplore, playoutResult);
		}
//		System.out.println(pncount + " : " + (end - System.currentTimeMillis()));

		gameTreeNode winnerNode = rootNode.getChildWithMaxScore();

//		System.out.println(winnerNode.getCard().getScore());
//		System.out.println(rootNode.getChildren().size());

		movesPicked += 1;

        return winnerNode.getCard().getMoveTo();
	}
}
