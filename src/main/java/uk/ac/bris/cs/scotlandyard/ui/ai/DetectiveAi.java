
package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.Tree;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;


public class DetectiveAi implements testAi{
    @Nonnull @Override public String name() {
        return "Henry"; }

    public Piece getCurrentPiece(ImmutableSet<Move> availableMoves){
        if(availableMoves.isEmpty()){throw new RuntimeException("No available moves for detectives");}

        Piece currentPiece = null;

        for(Move m: availableMoves){
            if (m.commencedBy().isDetective()){
                currentPiece = m.commencedBy();
                break;
            }
        }
        if (currentPiece == null){throw new RuntimeException("No available moves for detectives could be mrX's turn");}
        return currentPiece;
    }
    public ImmutableList<Float> getWeights(Optional<Board.TicketBoard> ticketBoard, Float w1, Float w2, Float w3){
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

    public Set<Move> getPlayersMoves(ImmutableSet<Move> availableMoves, Piece currentPiece){

        HashSet<Move> playerMoves = new HashSet<Move>();
        for(Move m: availableMoves){
            if(m.commencedBy().equals(currentPiece)){
                playerMoves.add(m);
            }
        }
        return playerMoves;
    }

    public Pair<ArrayList<ScotlandYard.Ticket>, Optional<Integer>> getLastKnownXLocation(ImmutableList<LogEntry> log){
        //returns a pair with tickets used since mrX last surface and mrX's last known location

        Optional<Integer> location = Optional.empty();
        ArrayList<ScotlandYard.Ticket> tickets = new ArrayList<>();
        for(int i = log.size(); i-- >0;){
            if (log.get(i).location().isPresent()){
                location = log.get(i).location();
                Collections.reverse(tickets);
                return new Pair<>(tickets, location);

            }
            tickets.add(log.get(i).ticket());

        }
        return new Pair<>(tickets, location);
    }

    public Set<Integer> getDetectivesLocations(Board board){
        HashSet<Integer> detectiveLocations = new HashSet<>();
        for(Piece p: board.getPlayers()){
            if (p.isDetective()){
                detectiveLocations.add(board.getDetectiveLocation((Piece.Detective) p).get());
            }
        }
        return detectiveLocations;

    }

    public Set<Integer> getAdjacentValidNodes(Integer source, ScotlandYard.Ticket ticket,GameSetup setup, Set<Integer> detectiveLocations){
        HashSet<Integer> validNodes = new HashSet<>();
        if (ticket.equals(ScotlandYard.Ticket.SECRET)){
            validNodes.addAll(setup.graph.adjacentNodes(source));
        }
        else{
            for(Integer node: setup.graph.adjacentNodes(source)){
                if (!detectiveLocations.contains(node)){
                    for (ScotlandYard.Transport t: setup.graph.edgeValueOrDefault(source, node, ImmutableSet.of())) {
                        if (t.requiredTicket().equals(ticket)) {
                            validNodes.add(node);
                        }
                    }
                }
            };
        }
        return validNodes;


    }
    public Set<Integer> getPossibleNextLocations(Integer source, ArrayList<ScotlandYard.Ticket> tickets, GameSetup setup, Set<Integer> detectiveLocations){
        HashSet<Integer> possibleNextLocations = new HashSet<>();
        possibleNextLocations.add(source);
        HashSet<Integer> temp = new HashSet<>();
        for(ScotlandYard.Ticket t: tickets){

            for(Integer l: possibleNextLocations) {

                temp.addAll(getAdjacentValidNodes(l, t, setup, detectiveLocations));
            }
            possibleNextLocations = (HashSet<Integer>) temp.clone();
            temp.clear();

        }
        return possibleNextLocations;
    }


    public Set<Integer> getMrXLocations(@Nonnull Board board){
        ImmutableList<LogEntry> log = board.getMrXTravelLog();
        GameSetup setup = board.getSetup();
        Pair<ArrayList<ScotlandYard.Ticket>, Optional<Integer>> xInfo = getLastKnownXLocation(log);
        Set<Integer> xLocations = new HashSet<>();
        ArrayList<ScotlandYard.Ticket> tickets = xInfo.left();
        Optional<Integer> lastLocation = xInfo.right();
        Set<Integer> detectivesLocations = getDetectivesLocations(board);

        if (log.isEmpty()) {

            return setup.graph.nodes();}
        if(tickets.isEmpty() && lastLocation.isPresent()){

            return Set.of(lastLocation.get());}

        if (lastLocation.isPresent()){

            return getPossibleNextLocations(lastLocation.get(), tickets, setup, detectivesLocations);
        }
        else   {
            for(Integer node: setup.graph.nodes()){
                xLocations.addAll(getPossibleNextLocations(node, tickets, setup, detectivesLocations));
            }
            return xLocations;
        }
    }

    public ArrayList<Float> dijkstraShortestPath(Integer root, ImmutableList<Float> weights, GameSetup setup){
        ArrayList<Float> distances = new ArrayList<Float>();
        ArrayList<Boolean> visitedList = new ArrayList<>();
        Integer currentNode = root;
        ArrayList<Float> calcWeights = new ArrayList<Float>();
        Float infinity = 100000F;

        for (int i = 1; i <= 199; i++) {
            if (i == root){
                distances.add(0F);

            }
            else {
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
                        }
                        else{ calcWeights.add(infinity);}
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
    @Nonnull
    @Override
    public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
        Set<Integer> mrXLocations = getMrXLocations(board);
        Piece currentPiece = getCurrentPiece(board.getAvailableMoves());
        Set<Move> playerMoves = getPlayersMoves(board.getAvailableMoves(), currentPiece);
        Optional<Board.TicketBoard> playerTickets = board.getPlayerTickets(currentPiece);
        ImmutableList<Float> ticketWeights = getWeights(playerTickets, 1.5F, 5F,7F);
        ImmutableList<Float> ticketWeights2 = ImmutableList.of(2F,6F, 7F);
        Move move = null;
        ArrayList<Float> shortestPath = null;
        Float score = 0F;
        Float prevScore = 10000000F;
        LocationUpdate lc = new LocationUpdate();

        for(Move m: playerMoves){

            shortestPath = dijkstraShortestPath(m.accept(lc),  ticketWeights, board.getSetup());

            for(Integer location: mrXLocations){
                score += shortestPath.get(location-1);
            }

            if(score < prevScore){
                move = m;
                prevScore = score;
                score =  0F;
            }


        }
        return move;
    }
}
