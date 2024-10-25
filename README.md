# ScotlandYardAI

ScotlandYardAI is a Java-based project developed as part of a university coursework to implement AI strategies in the game *Scotland Yard*. This project explores the complexities of game logic and AI decision-making, focusing on both Detective and MrX roles. Key techniques include the use of the Monte Carlo Tree Search (MCTS) algorithm and Dijkstra’s algorithm to create intelligent and adaptive gameplay.

## Features

- **Monte Carlo Tree Search (MCTS):** Implemented for MrX with move filtering and e-Greedy playouts for enhanced decision-making.
- **Detective AI with Dijkstra’s Algorithm:** Predicts MrX’s possible locations and finds optimal moves based on shortest paths.
- **Use of Design Patterns:** Includes Visitor and Observer patterns for efficient game state management.

## Project Structure

- **MyGameStateFactory:** Handles game state transitions, move tracking, and winning conditions using helper functions for clarity and efficiency.
- **Detective AI:** Uses Dijkstra’s algorithm to identify moves that minimize the distance to MrX’s possible locations.
- **MrX AI (MCTS):** Selects moves through iterative selection, expansion, playout, and backpropagation steps, improved by move filtering and e-Greedy strategies.

## AI Techniques Employed

1. **Monte Carlo Tree Search (MCTS):**
   - *Selection:* Uses Upper Confidence Bound formula with a custom constant to balance exploration and expansion.
   - *Expansion:* Adds new child nodes for unexplored moves.
   - *Playout:* Simulates random moves to the endgame state.
   - *Backpropagation:* Updates scores based on playout results, refining move predictions.
   
2. **Move Filtering:** Reduces unnecessary moves, enhancing efficiency and decision quality.
3. **e-Greedy Playouts:** Occasional heuristic-based moves improve the quality of simulated games.

## Test Suite for AI Progress

To effectively track the AI’s progress and simulate realistic gameplay scenarios, we developed a custom test suite. This suite allows for the simulation of full games, providing valuable insights into the AI's decision-making effectiveness and overall performance.

We modified the test suite in CW-Model to simulate entire game playthroughs by repeatedly calling `advance` and `pickMove` functions until a winner is determined. To ensure variety, we randomized the starting positions of all detectives and MrX in each iteration, generating unique game scenarios for comprehensive AI testing.

This testing approach allowed us to:
- Evaluate the AI’s performance under different starting conditions.
- Measure win rates and track improvements as additional strategies, such as MCTS enhancements, were implemented.

## Performance Testing

To end our project win rates for MrX AI were compared using the following models as you can see our final MCTS with Filtering and E-greedy was the best.

| MrX AI Model                   | Random Win Rate (%) | Dijkstra Win Rate(%)|
|--------------------------------|------------------|-------------------|
| Random Moves                   | 45               | 15                |
| Simple Dijkstra Detective      | 55               | 35                |
| Base MCTS                      | 60               | 45                |
| MCTS with Move Filtering       | 87               | 80                |
| MCTS with Filtering + e-Greedy | 99               | 92                |




## Future Improvements

- Integrate Progressive History into MCTS for enhanced selection strategies.
- Develop a Minimax-based AI for further comparison.

## Citations

- [Monte Carlo Tree Search Research Paper](https://dke.maastrichtuniversity.nl/m.winands/documents/Cig2011pape42.pdf)
- [Dijkstra’s Algorithm Wikipedia](https://en.wikipedia.org/wiki/Dijkstra%27s_algorithm)
- [MCTS Implementation in Tic-Tac-Toe](https://www.baeldung.com/java-monte-carlo-tree-search)
- [Scotland Yard Strategy Video](https://www.youtube.com/watch?v=aq6UxVNQztk)
