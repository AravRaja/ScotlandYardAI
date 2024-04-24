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
    public void calculatedAdvance(Pair<Long, TimeUnit> timeoutPair, List<Integer> detectiveLocations,ArrayList<ArrayList<Float>> lookupTable, DetectiveAi tempDetAi) { // employ the e-greedy method where with probability e a heuristic calculates the move instead of random
        Move bestMove = null;
        if (isMrxTurn()) {
            //calc distances of detectives to mrX and chose a move that maximises distance from them (distance meaning no. of moves that need to be made)
            Float score = 0F;
            Float prevScore = 0F;
            LocationUpdate lc = new LocationUpdate();

            for(Move m: state.getAvailableMoves()){


                for(Integer location: detectiveLocations){
                    if (location != null) {
                        score += lookupTable.get(m.accept(lc)-1).get(location-1);
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
                if (!isMrxTurn() && state.getWinner().isEmpty()) {
                    bestMove = tempDetAi.pickMove(this.state, timeoutPair);
                    this.state = state.advance(bestMove);
                }
//            }
        }
    }
}
