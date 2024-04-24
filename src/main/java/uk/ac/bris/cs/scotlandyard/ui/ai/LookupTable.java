package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.io.Resources;
import uk.ac.bris.cs.scotlandyard.model.GameSetup;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.IntStream;

import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.readGraph;

public class LookupTable {
    private static ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> defaultGraph; //this code is taken from cw-Model
    public static void setUp() {
        try {
            defaultGraph = readGraph(Resources.toString(Resources.getResource(
                            "graph.txt"),
                    StandardCharsets.UTF_8));
        } catch (IOException e) { throw new RuntimeException("Unable to read game graph", e); }
    }

    public static final ImmutableSet<Integer> REVEAL_MOVES =
            ImmutableSet.of(3, 8, 13, 18, 24);

    public static final ImmutableList<Boolean> STANDARD24MOVES = IntStream.rangeClosed(1, 24)
            .mapToObj(REVEAL_MOVES::contains)
            .collect(ImmutableList.toImmutableList());

    @Nonnull
    static GameSetup standard24MoveSetup() {
        return new GameSetup(defaultGraph, STANDARD24MOVES);
    } //end of taken from cw-Model

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

    public ArrayList<ArrayList<Float>> create(Float w1, Float w2, Float w3) {
        setUp();
        GameSetup setup = standard24MoveSetup();
        ArrayList<ArrayList<Float>> table = new ArrayList<>();
        for(Integer root: setup.graph.nodes()){
            table.add(dijkstraShortestPath(root, ImmutableList.of(w1,w2, w3) , setup));

        }
        return table;
    }
}
