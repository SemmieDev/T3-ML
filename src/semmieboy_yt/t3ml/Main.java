package semmieboy_yt.t3ml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Main {
    public static final byte[] board = new byte[9];
    public static final byte none = 0, cross = 1, circle = 2;
    public static volatile boolean gameOver, tie;
    public static final byte[] win = new byte[2];
    public static boolean autoLearn = !(System.getProperty("autoLearn") == null);
    public static final Object autoLearnLock = new Object();
    public static final Random random = new Random();

    public static GameWindow gameWindow;

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            autoLearn = false;
            synchronized (Brain.thinkingLock) {
                while (Brain.isThinking) {
                    try {
                        Brain.thinkingLock.wait();
                    } catch (InterruptedException exception) {
                        exception.printStackTrace();
                    }
                }
            }
            Brain.save();
        }));
        Arrays.fill(board, none);
        Brain.load();
        gameWindow = new GameWindow();

        synchronized (autoLearnLock) {
            while (true) {
                if (autoLearn) {
                    doAutoMove();
                } else {
                    try {
                        autoLearnLock.wait();
                    } catch (InterruptedException exception) {
                        exception.printStackTrace();
                    }
                }
            }
        }
    }

    public static void onMove(byte move) {
        if (gameOver) {
            gameOver = tie = false;
            Arrays.fill(board, none);
            gameWindow.repaint();
        } else {
            if (!Brain.isThinking) {
                if (board[move] == none) {
                    board[move] = cross;
                    checkWin();
                    gameWindow.repaint();
                    if (!gameOver) {
                        if (Brain.processMove()) checkWin();
                        gameWindow.repaint();
                    }
                }
            }
        }
    }

    public static void doAutoMove() {
        ArrayList<Byte> moves = new ArrayList<>();
        for (byte i = 0; i < board.length; i++) if (board[i] == none) moves.add(i);
        if (moves.isEmpty()) {
            gameOver = tie = true;
            onMove((byte)0);
        } else {
            onMove(moves.get(random.nextInt(moves.size())));
        }
    }

    public static boolean isSame(byte i1, byte i2, byte i3) {
        if (board[i1] == none || board[i2] == none || board[i3] == none) return false;
        return board[i1] == board[i2] && board[i1] == board[i3];
    }

    public static void checkWin() {
        for (byte i = 0; i < 3; i++) {
            if (isSame(pti(i, 0), pti(i, 1), pti(i, 2))) {
                gameOver = true;
                win[0] = pti(i, 0);
                win[1] = pti(i, 2);
                break;
            }

            if (isSame(pti(0, i), pti(1, i), pti(2, i))) {
                gameOver = true;
                win[0] = pti(0, i);
                win[1] = pti(2, i);
                break;
            }
        }

        if (!gameOver) {
            if (isSame((byte)0, pti(1, 1), pti(2, 2))) {
                gameOver = true;
                win[0] = 0;
                win[1] = pti(2, 2);
            } else if (isSame(pti(2, 0), pti(1, 1), pti(0, 2))) {
                gameOver = true;
                win[0] = pti(2, 0);
                win[1] = pti(0, 2);
            }
        }

        if (gameOver) Brain.onGameOver();
    }

    // Position to index
    public static byte pti(int x, int y) {
        return (byte)(x + y * 3);
    }

    // Index to position
    public static byte[] itp(byte index) {
        byte[] pos = new byte[2];
        pos[0] = (byte)(index % 3);
        pos[1] = (byte)(index / 3);
        return pos;
    }

    public static void print(String message) {
        if (!autoLearn) System.out.println(message);
    }
}
