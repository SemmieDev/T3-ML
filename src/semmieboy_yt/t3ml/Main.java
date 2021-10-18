package semmieboy_yt.t3ml;

public class Main {
    public static final byte[] board = new byte[9];
    public static final byte cross = 0, circle = 1;
    public static volatile boolean gameOver, tie;
    public static final byte[] win = new byte[2];

    public static GameWindow gameWindow;

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(Brain::save));
        clearBoard();
        Brain.load();
        gameWindow = new GameWindow();
    }

    private static void clearBoard() {
        byte value = circle;
        for (byte i = 0; i < board.length; i++) board[i] = ++value;
    }

    public static void onMove(byte move) {
        if (gameOver) {
            gameOver = tie = false;
            clearBoard();
            gameWindow.repaint();
        } else {
            if (!Brain.isThinking) {
                if (board[move] != cross && board[move] != circle) {
                    board[move] = cross;
                    checkWin();
                    gameWindow.repaint();
                    if (!gameOver) {
                        Brain.processMove();
                        checkWin();
                        gameWindow.repaint();
                    }
                }
            }
        }
    }

    public static void checkWin() {
        for (byte i = 0; i < 3; i++) {
            if (board[pti(i, 1)] == board[pti(i, 0)] && board[pti(i, 2)] == board[pti(i, 0)]) {
                gameOver = true;
                win[0] = pti(i, 0);
                win[1] = pti(i, 2);
                return;
            }

            if (board[pti(1, i)] == board[pti(0, i)] && board[pti(2, i)] == board[pti(0, i)]) {
                gameOver = true;
                win[0] = pti(0, i);
                win[1] = pti(2, i);
                return;
            }
        }

        if (board[pti(1, 1)] == board[0] && board[pti(2, 2)] == board[0]) {
            gameOver = true;
            win[0] = 0;
            win[1] = pti(2, 2);
            return;
        }

        if (board[pti(1, 1)] == board[pti(2, 0)] && board[pti(0, 2)] == board[pti(2, 0)]) {
            gameOver = true;
            win[0] = pti(2, 0);
            win[1] = pti(0, 2);
        }
    }

    public static byte pti(int x, int y) {
        return (byte)(x + y * 3);
    }

    public static byte[] itp(byte index) {
        byte[] pos = new byte[2];
        pos[0] = (byte)(index % 3);
        pos[1] = (byte)(index / 3);
        return pos;
    }
}
