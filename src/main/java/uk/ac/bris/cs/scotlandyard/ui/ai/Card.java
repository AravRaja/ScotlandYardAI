package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.ArrayList;
import java.util.Random;

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

    public void setVisitCount(int visitCount) {
        this.visitCount = visitCount;
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

    public ArrayList<Card> potentialCards() {
        var moves = state.getAvailableMoves().asList();
        ArrayList<Card> potCards = new ArrayList<>();
        for (Move m : moves) {
            Card temp = new Card(state.advance(m),m);
            temp.setPlayerNo(Math.abs(1-this.getPlayerNo()));
            potCards.add(temp);
        }
        return potCards;
    }

    public boolean isMrxTurn() {
        var moves = state.getAvailableMoves().asList();
        return moves.get(0).commencedBy() == Piece.MrX.MRX;
    }

    public void randomAdvance() {
        var moves = state.getAvailableMoves().asList();
        if (isMrxTurn()) {
            this.state = state.advance(moves.get(new Random().nextInt(moves.size())));
        }
        else {
            int n = state.getPlayers().size()-1;
            for (int i = 0; i < n; i++) {
                if (state.getWinner().isEmpty()) {
                    this.state = state.advance(moves.get(new Random().nextInt(moves.size())));
                }
                moves = state.getAvailableMoves().asList();
            }
        }
    }
}
