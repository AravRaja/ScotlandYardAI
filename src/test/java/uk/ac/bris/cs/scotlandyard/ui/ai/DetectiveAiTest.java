package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;
import org.junit.Test;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.LogEntry;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.*;
import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket.*;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultDetectiveTickets;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultMrXTickets;

public class DetectiveAiTest extends ParameterisedModelTestBase {
    @Test public void testLastKnownLocation() {
        var mrX = new Player(MRX, defaultMrXTickets(), 106);
        var red = new Player(RED, defaultDetectiveTickets(), 91);
        var green = new Player(GREEN, defaultDetectiveTickets(), 29);
        var blue = new Player(BLUE, defaultDetectiveTickets(), 94);
        var white = new Player(WHITE, defaultDetectiveTickets(), 50);
        var yellow = new Player(YELLOW, defaultDetectiveTickets(), 138);
        Pair<Long, TimeUnit> t = new Pair<>(30L, TimeUnit.SECONDS);
        GameState state = gameStateFactory.build(standard24MoveSetup(),
                mrX, red, green, blue, white, yellow);


        DetectiveAi ai = new DetectiveAi();


        ArrayList<LogEntry> log = new ArrayList<>();
        assertThat(ai.getLastKnownXLocation(ImmutableList.copyOf(log)).left()).containsExactly();
        assertThat(ai.getLastKnownXLocation(ImmutableList.copyOf(log)).right()).isEmpty();
        log.add(LogEntry.hidden(BUS));
        log.add(LogEntry.hidden(BUS));
        assertThat(ai.getLastKnownXLocation(ImmutableList.copyOf(log)).left()).containsExactly(BUS, BUS);
        assertThat(ai.getLastKnownXLocation(ImmutableList.copyOf(log)).right()).isEmpty();
        log.add(LogEntry.reveal(BUS, 84));
        log.add(LogEntry.hidden(TAXI));
        log.add(LogEntry.hidden(BUS));
        log.add(LogEntry.hidden(SECRET));
        assertThat(ai.getLastKnownXLocation(ImmutableList.copyOf(log)).left()).containsExactly(TAXI, BUS, SECRET);
        assertThat(ai.getLastKnownXLocation(ImmutableList.copyOf(log)).right()).contains(84);
        log.add(LogEntry.reveal(BUS, 90));
        assertThat(ai.getLastKnownXLocation(ImmutableList.copyOf(log)).left()).containsExactly();
        assertThat(ai.getLastKnownXLocation(ImmutableList.copyOf(log)).right()).contains(90);
    }
    @Test public void testGetMrXLocation(){
        var mrX = new Player(MRX, defaultMrXTickets(), 106);
        var red = new Player(RED, defaultDetectiveTickets(), 91);
        var green = new Player(GREEN, defaultDetectiveTickets(), 29);
        var blue = new Player(BLUE, defaultDetectiveTickets(), 94);
        var white = new Player(WHITE, defaultDetectiveTickets(), 50);
        var yellow = new Player(YELLOW, defaultDetectiveTickets(), 138);
        GameState state = gameStateFactory.build(standard24MoveSetup(),
                mrX, red, green, blue, white, yellow);

        state = state
                .advance(x2(MRX, 106, TAXI, 105, BUS, 87))

                .advance(taxi(YELLOW, 138, 152))
                .advance(taxi(WHITE, 50, 49))
                .advance(bus(BLUE, 94, 77))
                .advance(taxi(GREEN, 29, 41))
                .advance(taxi(RED, 91, 105))

                .advance(taxi(MRX, 87, 88))

                .advance(bus(RED, 105, 87))
                .advance(taxi(WHITE, 49, 66))
                .advance(taxi(BLUE, 77, 96))
                .advance(taxi(YELLOW, 152, 138))
                .advance(taxi(GREEN, 41, 54))

                .advance(x2(MRX, 88, TAXI, 89, UNDERGROUND, 67));

        DetectiveAi ai = new DetectiveAi();
        assertThat(ai.getMrXLocations(state)).contains(67);

    }
    @Test public void testMrXLocations() {
        var mrX = new Player(MRX, defaultMrXTickets(), 88);
        var blue = new Player(BLUE, defaultDetectiveTickets(), 85);
        GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, blue);
        state = state.advance(x2(MRX, 88, TAXI, 89, UNDERGROUND, 67));
        DetectiveAi ai = new DetectiveAi();
        assertThat(ai.getMrXLocations(state).size()).isEqualTo(47);

    }
    @Test public void dijkstraAlgorithm() {
        var mrX = new Player(MRX, defaultMrXTickets(), 88);
        var blue = new Player(BLUE, defaultDetectiveTickets(), 85);
        GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, blue);
        state = state.advance(x2(MRX, 88, TAXI, 89, UNDERGROUND, 67));
        LookupTable lc = new LookupTable();

        ImmutableList<Float> weights = ImmutableList.of(3F,3F,3F);
        System.out.println(lc.dijkstraShortestPath(63,weights , state.getSetup()).get(33));

    }
    public boolean availableMovesChecker(ImmutableSet<Move> availableMoves){
        for(Move m: availableMoves){
            if(m.commencedBy().isDetective()){return true;}
        }
        return false;
    }

    @Test public void testSimpleGame() {
        HashSet<Integer> randLocations = new HashSet<>();
        while(randLocations.size() != 6){
            randLocations.add(new Random().nextInt(199)+1);
        }
        ArrayList<Integer> randomLocations = new ArrayList<>(randLocations);

        var mrX = new Player(MRX, defaultMrXTickets(), randomLocations.get(0));
        var red = new Player(RED, defaultDetectiveTickets(), randomLocations.get(1));
        var green = new Player(GREEN, defaultDetectiveTickets(), randomLocations.get(2));
        var blue = new Player(BLUE, defaultDetectiveTickets(), randomLocations.get(3));
        var white = new Player(WHITE, defaultDetectiveTickets(), randomLocations.get(4));
        var yellow = new Player(YELLOW, defaultDetectiveTickets(), randomLocations.get(5));
        GameState state = gameStateFactory.build(standard24MoveSetup(),
                mrX, red, green, blue, white, yellow);

        Ai ai = new DetectiveAi();
        Ai ai1 = new MyAi();
        Pair<Long, TimeUnit> time = Pair.pair(30L, TimeUnit.SECONDS);
        int score = 0;
        int move = 0;
        var moves = state.getAvailableMoves().asList();

        for(int i = 1; i <= 100; i++){
            randLocations = new HashSet<>();
            while(randLocations.size() != 6){
                randLocations.add(new Random().nextInt(199)+1);
            }
            randomLocations = new ArrayList<>(randLocations);

            mrX = new Player(MRX, defaultMrXTickets(), randomLocations.get(0));
            red = new Player(RED, defaultDetectiveTickets(), randomLocations.get(1));
            green = new Player(GREEN, defaultDetectiveTickets(), randomLocations.get(2));
            blue = new Player(BLUE, defaultDetectiveTickets(), randomLocations.get(3));
            white = new Player(WHITE, defaultDetectiveTickets(), randomLocations.get(4));
            yellow = new Player(YELLOW, defaultDetectiveTickets(), randomLocations.get(5));
            state = gameStateFactory.build(standard24MoveSetup(),
                    mrX, red, green, blue, white, yellow);
            while(state.getWinner().isEmpty()) {
                ;   moves = state.getAvailableMoves().asList();

                //if(availableMovesChecker(state.getAvailableMoves())){
                state = state.advance(ai1.pickMove(state, time));

                for(int j = 1; j <= 5; j++) {
                    if(availableMovesChecker(state.getAvailableMoves())){
                        moves = state.getAvailableMoves().asList();
                        state = state.advance(ai.pickMove(state, time));
                    }
                }

                // MrX captured here

            }
            move +=1;
            System.out.println(score);
            if (state.getWinner().contains(RED)){score += 1;}
            System.out.println("GAMES WON BE DETECTIVE:" + score);
            System.out.println("GAMES PLAYED: " + move);
            System.out.println("PERCENT WIN: " + (score/move));
            state = gameStateFactory.build(standard24MoveSetup(),
                    mrX, red, green, blue, white, yellow);
        }


    }

}


