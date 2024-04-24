package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;
import org.junit.Test;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.*;
import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket.*;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultDetectiveTickets;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultMrXTickets;

public class MCTSTest extends ParameterisedModelTestBase {

    @Test public void testAllEdgeTaxiOnly(){
        MyAi testAI = new MyAi();
        var mrX = new Player(MRX, defaultMrXTickets(), 106);
        var red = new Player(RED, defaultDetectiveTickets(), 91);
        var green = new Player(GREEN, defaultDetectiveTickets(), 29);
        var blue = new Player(BLUE, defaultDetectiveTickets(), 94);
        var white = new Player(WHITE, defaultDetectiveTickets(), 50);
        var yellow = new Player(YELLOW, defaultDetectiveTickets(), 138);
        Pair<Long, TimeUnit> t = new Pair<>(30L, TimeUnit.SECONDS);
        GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, red, green, blue, white, yellow);
        assert(testAI.allEdgeTaxiOnly(54, state));
        assert(!testAI.allEdgeTaxiOnly(41, state));
    }

    @Test public void testTurn() {
        MyAi testAI = new MyAi();
        var mrX = new Player(MRX, defaultMrXTickets(), 106);
        var red = new Player(RED, defaultDetectiveTickets(), 91);
        var green = new Player(GREEN, defaultDetectiveTickets(), 29);
        var blue = new Player(BLUE, defaultDetectiveTickets(), 94);
        var white = new Player(WHITE, defaultDetectiveTickets(), 50);
        var yellow = new Player(YELLOW, defaultDetectiveTickets(), 138);
        Pair<Long, TimeUnit> t = new Pair<>(30L, TimeUnit.SECONDS);
        GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, red, green, blue, white, yellow);
        Card testCard = new Card(state,null);
        assert(testCard.isMrxTurn());
        testCard.randomAdvance();
        assert(!testCard.isMrxTurn());
    }

    @Test public void testPotentialCards() {
        var mrX = new Player(MRX, defaultMrXTickets(), 106);
        var red = new Player(RED, defaultDetectiveTickets(), 91);
        var green = new Player(GREEN, defaultDetectiveTickets(), 29);
        var blue = new Player(BLUE, defaultDetectiveTickets(), 94);
        var white = new Player(WHITE, defaultDetectiveTickets(), 50);
        var yellow = new Player(YELLOW, defaultDetectiveTickets(), 138);
        Pair<Long, TimeUnit> t = new Pair<>(30L, TimeUnit.SECONDS);
        GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, red, green, blue, white, yellow);
        Card testCard = new Card(state, null);
        List<Card> children = new ArrayList<>();
        children = testCard.potentialCards();
        assert (children.size() == state.getAvailableMoves().size());
        assert (children.get(0).getPlayerNo() == 1);
        assert (children.get(0).getMoveTo() == state.getAvailableMoves().asList().get(0));
    }

    @Test public void testFilterCards(){
        var mrX = new Player(MRX, defaultMrXTickets(), 106);
        var red = new Player(RED, defaultDetectiveTickets(), 91);
        var green = new Player(GREEN, defaultDetectiveTickets(), 29);
        var blue = new Player(BLUE, defaultDetectiveTickets(), 94);
        var white = new Player(WHITE, defaultDetectiveTickets(), 50);
        var yellow = new Player(YELLOW, defaultDetectiveTickets(), 138);
        Pair<Long, TimeUnit> t = new Pair<>(30L, TimeUnit.SECONDS);
        GameState state = gameStateFactory.build(standard24MoveSetup(), mrX, red, green, blue, white, yellow);
        Card testCard = new Card(state, null);
        List<Card> children;
        children = testCard.potentialCards();
        MyAi testAI = new MyAi();
        MyAi.Destination testDest = new MyAi.Destination();
        List<Card> filteredChildren = testAI.filterCards(children);
        for (Card c : filteredChildren) {
            int dest = c.getMoveTo().accept(testDest);
            for (ScotlandYard.Ticket tic : c.getMoveTo().tickets()) {
                assert(tic != SECRET);
                assert(tic != DOUBLE);
                for (int i : testAI.detectiveLocations) {
                    for (int j : state.getSetup().graph.adjacentNodes(i)) {
                          assert(j != dest);
                    }
                }
            }
        }

    }
}

