package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Card {
    Board.GameState state;

    Move moveTo;

    int playerNo;
    int visitCount;
    double score;

    public Card(Board.GameState state, Move move) {
        this.state = state;
        this.moveTo = move;
        this.playerNo = 2;
        this.visitCount = 0;
        this.score = 0;
    }

    public Board.GameState getState() {
        return state;
    }

    public int getVisitCount() {
        return visitCount;
    }

    public double getScore() {
        return score;
    }

    public int getPlayerNo() {
        return playerNo;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public void setState(Board.GameState state) {
        this.state = state;
    }

    public void setPlayerNo(int playerNo) {
        this.playerNo = playerNo;
    }

    public Move getMoveTo() {
        return moveTo;
    }

    public void addScore(int increment) {
        this.score = score + increment;
    }

    public void incrementVisit() {
        this.visitCount += 1;
    }

    public ArrayList<Card> potentialCards() { //creates list of Cards for each move that cna be made from current state
        var moves = state.getAvailableMoves().asList();
        ArrayList<Card> potCards = new ArrayList<>();
        for (Move m : moves) {
            Card temp = new Card(state.advance(m),m);
            temp.setPlayerNo(Math.abs(1-this.getPlayerNo()));
            potCards.add(temp);
        }
        return potCards;
    }

    public boolean isMrxTurn() { //checks whose turn it is by checking who commences availableMoves
        var moves = state.getAvailableMoves().asList();
        if (!moves.isEmpty()){
            return moves.get(0).commencedBy() == Piece.MrX.MRX;
        }
        else{return true;}

    }

    public void randomAdvance() { //does a random advance from available moves (for the random playouts)
        var moves = state.getAvailableMoves().asList();
        this.state = state.advance(moves.get(new Random().nextInt(moves.size())));
    }

    public ArrayList<Float> dijkstraShortestPath(Integer root, ImmutableList<Float> weights, GameSetup setup) { //alternative dyjkstra to DetectiveAi for mrX to maximise his distance from the detetectives
        ArrayList<Float> distances = new ArrayList<Float>();
        ArrayList<Boolean> visitedList = new ArrayList<>();
        Integer currentNode = root;
        ArrayList<Float> calcWeights = new ArrayList<Float>();
        Float infinity = 100000F;

        for (int i = 1; i <= 199; i++) {
            if (i == root) {
                distances.add(0F);

            } else {
                distances.add(infinity);
            }
            visitedList.add(false);
        }
        ArrayList<Float> unvisitedDistances = (ArrayList<Float>) distances.clone();
        float newDistance = 0F;

        while (visitedList.contains(false)) {

            for (Integer node : setup.graph.adjacentNodes(currentNode)) {
                if (!visitedList.get(node - 1)) {
                    for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(currentNode, node, ImmutableSet.of())) {
                        if (t.requiredTicket().equals(ScotlandYard.Ticket.TAXI)) {
                            calcWeights.add(weights.get(0));
                        } else if (t.requiredTicket().equals(ScotlandYard.Ticket.BUS)) {
                            calcWeights.add(weights.get(1));
                        } else if (t.requiredTicket().equals(ScotlandYard.Ticket.UNDERGROUND)) {
                            calcWeights.add(weights.get(2));
                        } else {
                            calcWeights.add(infinity);
                        }
                    }
                    newDistance = Collections.min(calcWeights) + distances.get(currentNode - 1);

                    if (newDistance < distances.get(node - 1)) {
                        distances.remove(node - 1);
                        distances.add(node - 1, newDistance);
                        unvisitedDistances.remove(node - 1);
                        unvisitedDistances.add(node - 1, newDistance);
                    }
                    newDistance = 0;
                    calcWeights.clear();
                }
            }

            visitedList.remove(currentNode - 1);
            visitedList.add(currentNode - 1, true);
            unvisitedDistances.remove(currentNode - 1);
            unvisitedDistances.add(currentNode - 1, infinity);
            currentNode = unvisitedDistances.indexOf(Collections.min(unvisitedDistances)) + 1;
        }

        return distances;
    }
    public static class LocationUpdate implements Move.Visitor<Integer> {

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


    public static ImmutableList<Float> getWeights(Optional<Board.TicketBoard> ticketBoard, Float w1, Float w2, Float w3){
        Set<ScotlandYard.Ticket> tickets =   Set.of(ScotlandYard.Ticket.TAXI, ScotlandYard.Ticket.BUS, ScotlandYard.Ticket.UNDERGROUND);
        ArrayList<Float> weights = new ArrayList<>();
        if (ticketBoard.isEmpty()){throw new RuntimeException("Detective doesn't have any tickets");}
        else{
            for(ScotlandYard.Ticket t:tickets){
                if (ticketBoard.get().getCount(t) != 0){
                    weights.add(w1/ticketBoard.get().getCount(t));

                }
                else{weights.add(10000F);}
            }
            return ImmutableList.copyOf(weights);

            //return ImmutableList.copyOf(Arrays.asList(w1/ticketBoard.get().getCount(ScotlandYard.Ticket.TAXI), w2/ticketBoard.get().getCount(ScotlandYard.Ticket.BUS),w3/ticketBoard.get().getCount(ScotlandYard.Ticket.UNDERGROUND)));
        }
    }

    public void calculatedAdvance(Pair<Long, TimeUnit> timeoutPair, List<Integer> detectiveLocations) { // employ the e-greedy method where with probability e a heuristic calculates the move instead of random
        Move bestMove = null;
        if (isMrxTurn()) {
            //calc distances of detectives to mrX and chose a move that maximises distance from them (distance meaning no. of moves that need to be made)

//            ImmutableList<Float> ticketWeights = getWeights(playerTickets, 1.5F, 5F,7F);
            ImmutableList<Float> ticketWeights2 = ImmutableList.of(2F,6F,7F);
            Float score = 0F;
            Float prevScore = 0F;
            LocationUpdate lc = new LocationUpdate();
            List<Float> shortestPath;

            for(Move m: state.getAvailableMoves()){

                shortestPath = dijkstraShortestPath(m.accept(lc),  ticketWeights2, state.getSetup());

                for(Integer location: detectiveLocations){
                    if (location != null) {
                        score += shortestPath.get(location-1);
                    }
                }

                if(score > prevScore){
                    bestMove = m;
                    prevScore = score;
                    score =  0F;
                }


            }
            this.state = state.advance(bestMove);        }

        else {
                //calc smallest distance to nearest mrx possible mrx location
                Ai tempDetAi = new DetectiveAi(); //use heuristic used to calculate bestmove for a detective
                if (!isMrxTurn() && state.getWinner().isEmpty()) {
                    bestMove = tempDetAi.pickMove(this.state, timeoutPair);
                    this.state = state.advance(bestMove);
                }
//            }
        }
    }
}
