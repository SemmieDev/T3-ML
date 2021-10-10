package semmieboy_yt.t3ml;

public class Main {
    public static final byte[] BOARD = new byte[9];
    public static final byte NONE = 0, CROSS = 1, CIRCLE = 2;

    private static GameWindow gameWindow;

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(Brain::save));
        Brain.load();
        gameWindow = new GameWindow();
    }

    public static void onMove(int square) {
        if (!Brain.isThinking) {
            if (BOARD[square] == NONE) {
                BOARD[square] = CROSS;
                gameWindow.repaint();
                Brain.processMove();
            }
        }
    }
}
