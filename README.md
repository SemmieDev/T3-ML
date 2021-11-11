# T3-ML
 T3-ML is a machine learning AI for tic-tac-toe
 
**How it works**
Every time the AI wins or loses, it remembers that it shouldn't do that move on that board configuration. After the player did a move, the AI looks through all its memories and if the memory matches the current board, it will check if it's a good or bad move. If it's a bad move, it will remove that move from the possible moves. If it's a good move, it will check if the move is better than the current best move. If it is, save it as the new best move. If it's not, it will forget that move (since there is a better one). After all of that, it checks if it has any possible moves. If it doesn't, it is trapped and does a random move, otherwise it checks if it knows a good move. When it does, it does that good move. Otherwise, it does a possible move, remembering if it won or lost.

**How to use**
Download the latest version from the [releases](https://github.com/SemmieboyYT/T3-ML/releases) and run it (minimum java version is 8). There is only one special control: when you press A, it starts learning automatically at the maximum possible speed. The maximum possible speed depends on your system.
