package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.sun.source.tree.Tree;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;
public class gameTree {
    static gameTreeNode root; // Root node of the tree

    public gameTree(gameTreeNode root) {

        this.root = root;
    }
}