package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class gameTreeNode {
    Card card; // Represents the game state at this node

    gameTreeNode parent;
    List<gameTreeNode> children; // Child nodes

    public gameTreeNode(Card card, gameTreeNode parent) {
        this.card = card;
        this.parent = parent;
        this.children = new ArrayList<>();
    }

    // Add a child node
    public void addChild(gameTreeNode child) {
        children.add(child);
    }

    public void removeChild(gameTreeNode child) { children.remove(child); }

    public gameTreeNode getParent() {
        return parent;
    }

    public Card getCard() {
        return card;
    }

    public List<gameTreeNode> getChildren() {
        return children;
    }

    public void setCard(Card card) {
        this.card = card;
    }

    public void setChildren(List<gameTreeNode> children) {
        this.children = children;
    }

    public void setParent(gameTreeNode parent) {
        this.parent = parent;
    }

    public gameTreeNode getRandomChildNode() {
        return this.getChildren().get(new Random().nextInt(this.getChildren().size()));
    }

    public gameTreeNode getChildWithMaxScore() {
        double max = Integer.MIN_VALUE;
        gameTreeNode maxScoreChild = this.children.get(0);
        for (gameTreeNode c : this.children) {
            if (c.getCard().getScore() > max) {
                max = c.getCard().getScore();
                maxScoreChild = c;
            }
        }
        return maxScoreChild;
    }
}
