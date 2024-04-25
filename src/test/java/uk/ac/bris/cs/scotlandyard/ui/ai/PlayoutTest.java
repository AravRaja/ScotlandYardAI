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

public class PlayoutTest extends ParameterisedModelTestBase {

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
        ai.onStart();
        ai1.onStart();
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
            System.out.println("GAMES WON BY DETECTIVE: " + score);
            System.out.println("GAMES WON BY MRX: " + (move-score));
            System.out.println("GAMES PLAYED: " + move);

            state = gameStateFactory.build(standard24MoveSetup(),
                    mrX, red, green, blue, white, yellow);
        }


    }

}


