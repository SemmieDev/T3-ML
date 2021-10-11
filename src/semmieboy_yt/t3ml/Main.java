package semmieboy_yt.t3ml;

import java.util.Arrays;

public class Main {
    public static final byte[] board = new byte[9];
    public static final byte none = 0, cross = 1, circle = 2;
    public static volatile boolean gameOver;
    public static final byte[] win = new byte[2];

    private static GameWindow gameWindow;

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(Brain::save));
        Brain.load();
        gameWindow = new GameWindow();
    }

    public static void onMove(byte move) {
        if (gameOver) {
            gameOver = false;
            Arrays.fill(board, none);
            gameWindow.repaint();
        } else {
            if (!Brain.isThinking) {
                if (board[move] == none) {
                    board[move] = cross;
                    checkWin(move);
                    gameWindow.repaint();
                    if (!gameOver) {
                        checkWin(Brain.processMove());
                        gameWindow.repaint();
                    }
                }
            }
        }
    }

    public static void checkWin(byte lastMove) {
        //win will always be at last move
        //dont need to know who won
    }

    public static byte positionToIndex(int x, int y) {
        return (byte)(x + y * 3);
    }

    /*public static void indexToPosition(byte index) {
        x = index / 3;
        y = index % 3;
    }*/
}
