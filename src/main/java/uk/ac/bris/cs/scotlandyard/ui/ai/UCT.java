package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UCT { // this code is taken from https://www.baeldung.com/java-monte-carlo-tree-search as it is a formula
    public static double uctValue( //calculates UCT/UCB1 formula to pick a promising node in a monte carlo tree search
            int totalVisit, double nodeWinScore, int nodeVisit) {
        if (nodeVisit == 0) {
            return Integer.MAX_VALUE;
        }
        return ( nodeWinScore / (double) nodeVisit) + 1.1 * Math.sqrt(Math.log(totalVisit) / (double) nodeVisit);
    }

    public static gameTreeNode findBestNodeWithUCT(gameTreeNode node) { //gets max UCT value
        int parentVisit = node.getCard().getVisitCount();
        List<Double> values = new ArrayList<>();
        for (gameTreeNode c : node.getChildren()) {
            values.add(uctValue(parentVisit,c.getCard().getScore(),c.getCard().getVisitCount()));
        }
        int maxI = values.indexOf(Collections.max(values));
        return node.getChildren().get(maxI);
    }
}
