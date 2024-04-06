package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class UCT {
    public static double uctValue(
            int totalVisit, double nodeWinScore, int nodeVisit) {
        if (nodeVisit == 0) {
            return Integer.MAX_VALUE;
        }
        return ((double) nodeWinScore / (double) nodeVisit)
                + 1.41 * Math.sqrt(Math.log(totalVisit) / (double) nodeVisit);
    }

    public static gameTreeNode findBestNodeWithUCT(gameTreeNode node) {
        int parentVisit = node.getCard().getVisitCount();
        List<Double> values = new ArrayList<>();
        for (gameTreeNode c : node.getChildren()) {
            values.add(uctValue(parentVisit,c.getCard().getScore(),c.getCard().getVisitCount()));
        }
        int maxI = values.indexOf(Collections.max(values));
        return node.getChildren().get(maxI);
    }
}
