package semmieboy_yt.t3ml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Brain {
    public static volatile boolean isThinking;
    public static final Object thinkingLock = new Object();

    private static Memory lostMemory;
    private static final ArrayList<Memory> memories = new ArrayList<>();
    private static final File saveFile = new File("brain.dat");

    public static void debug() {
        byte[] board = new byte[9];
        byte[] squareAndPoints = new byte[2];
        for (int i = 0; i < 10000; i++) {
            Main.random.nextBytes(board);
            Main.random.nextBytes(squareAndPoints);
            memories.add(new Memory(squareAndPoints[0], board, squareAndPoints[1]));
        }
        long time = System.currentTimeMillis();
        save();
        time = System.currentTimeMillis() - time;
        System.out.println("Saving finished in "+time+" milliseconds");
        memories.clear();
        time = System.currentTimeMillis();
        load();
        time = System.currentTimeMillis() - time;
        System.out.println("Loading finished in "+time+" milliseconds");
    }

    public static void save() {
        try (GZIPOutputStream saveFileOutput = new GZIPOutputStream(new FileOutputStream(saveFile))) {
            for (Memory memory : memories) saveFileOutput.write(memory.toArray());
        } catch (IOException exception) {
            exception.printStackTrace();
            System.err.println("Unable to write to save file");
        }
    }

    public static void load() {
        if (saveFile.isFile()) {
            try (GZIPInputStream saveFileInput = new GZIPInputStream(new FileInputStream(saveFile));) {
                while (true) {
                    byte[] data = new byte[Memory.SIZE];
                    if (saveFileInput.readNBytes(data, 0, data.length) == Memory.SIZE) {
                        memories.add(new Memory(data));
                    } else {
                        break;
                    }
                }
            } catch (IOException exception) {
                exception.printStackTrace();
                System.err.println("Unable to read from save file");
            }
        }
    }

    public static void processMove() {
        isThinking = true;

        lostMemory = null;
        var move = new Object() {
            public byte value = -1;
        };
        ArrayList<Byte> possibleMoves = new ArrayList<>();
        for (byte i = 0; i < Main.board.length; i++) if (Main.board[i] == Main.none) possibleMoves.add(i);

        if (possibleMoves.isEmpty()) {
            Main.gameOver = true;
            Main.tie = true;
        } else {
            var bestMemory = new Object() {
                public Memory value = null;
            };
            ArrayList<Byte> smartMoves = new ArrayList<>(possibleMoves);
            ArrayList<Memory> memories = new ArrayList<>(Brain.memories);
            memories.forEach(m -> {
                Memory memory = m.copy();
                if (memoryMatchesBoard(memory)) {
                    if (memory.points < 0) {
                        System.out.println("Removing bad move: "+memory.move);
                        smartMoves.remove((Object)memory.move);
                    } else if (bestMemory.value == null || memory.points > bestMemory.value.points) {
                        // Don't integrate this if statement in the above if statement, or the else block will be called
                        if (Main.board[memory.move] == Main.none) {
                            System.out.println("Best move so far: "+memory.move);
                            bestMemory.value = memory;
                        }
                    } else {
                        // By the time this get reached, the memory is always the worst memory
                        System.out.println("Forgetting worse move: "+memory.move);
                        Brain.memories.remove(memory);
                    }
                }
            });
            memories.clear();

            if (smartMoves.isEmpty()) {
                // TODO: 10/20/2021 Add a bad memory with the move that caused it to get trapped
                System.out.println("Trapped");
                Main.board[possibleMoves.get(Main.random.nextInt(possibleMoves.size()))] = Main.circle;
            } else {
                if (bestMemory.value == null) {
                    System.out.println("Don't know any good moves");
                    byte[] board = Arrays.copyOf(Main.board, Main.board.length);
                    move.value = smartMoves.get(Main.random.nextInt(smartMoves.size()));
                    Main.board[move.value] = Main.circle;
                    Main.checkWin();

                    if (Main.gameOver) {
                        // Game over after AI did something, so AI won
                        System.out.println("Remembering good move: "+move.value);
                        Brain.memories.add(new Memory((byte) 100, board, move.value));
                    } else {
                        lostMemory = new Memory((byte) -100, board, move.value);
                    }
                } else {
                    System.out.println("Doing best move: "+bestMemory.value.move);
                    Main.board[bestMemory.value.move] = Main.circle;
                    Main.checkWin();
                }
            }
        }

        isThinking = false;
        synchronized (thinkingLock) {
            thinkingLock.notifyAll();
        }
    }

    public static void onGameOver() {
        if (lostMemory != null) {
            System.out.println("Remembering bad move: "+lostMemory.move);
            memories.add(lostMemory.copy());
            lostMemory = null;
        }
    }

    private static boolean memoryMatchesBoard(Memory memory) {
        if (boardEquals(Main.board, memory.board)) return true;
        byte[] board = Arrays.copyOf(Main.board, Main.board.length);
        byte[] before;

        // rotate the board, then check if it equals
        for (byte i = 0; i < 8; i++) {
            before = Arrays.copyOf(board, board.length);
            for (byte j = 0; j < board.length; j++) board[j] = before[rotate(j)];
            memory.move = rotate(memory.move);
            if (boardEquals(board, memory.board)) return true;
        }
        return false;
    }

    private static boolean boardEquals(byte[] board, boolean[] memoryBoard) {
        boolean matches = true;
        for (int i = 0; i < board.length; i++) {
            if (board[i] == Main.cross != memoryBoard[i]) {
                matches = false;
                break;
            }
        }
        return matches;
    }

    private static byte rotate(byte index) {
        switch (index) {
            // TODO: 10/20/2021 Precalculate these values
            case 0 -> index = Main.pti(0, 1);
            case 1 -> index = Main.pti(0, 0);
            case 2 -> index = Main.pti(1, 0);
            case 5 -> index = Main.pti(2, 0);
            case 8 -> index = Main.pti(2, 1);
            case 7 -> index = Main.pti(2, 2);
            case 6 -> index = Main.pti(1, 2);
            case 3 -> index = Main.pti(0, 2);
        }
        return index;
    }

    private static class Memory {
        public static int SIZE = 11;

        public byte points, move;
        public final boolean[] board;

        public Memory(byte points, boolean[] board, byte move) {
            this.points = points;
            this.board = board;
            this.move = move;
        }

        public Memory(byte points, byte[] board, byte move) {
            this.points = points;
            this.board = new boolean[board.length];
            this.move = move;

            for (int i = 0; i < board.length; i++) if (board[i] == Main.cross) this.board[i] = true;
        }

        public Memory(byte[] data) {
            this(data[0], Arrays.copyOfRange(data, 1, 10), data[10]);
        }

        public byte[] toArray() {
            byte[] data = new byte[SIZE];
            data[0] = points;
            // TODO: 10/20/2021 Store as bits
            for (int i = 1; i < board.length + 1; i++) data[i] = (byte)(board[i - 1] ? 1 : 0);
            data[10] = move;
            return data;
        }

        public Memory copy() {
            return new Memory(points, board, move);
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof Memory memory) return memory.points == points && Arrays.equals(memory.board, board) && memory.move == move;
            return false;
        }

        @Override
        public String toString() {
            return points+" "+Arrays.toString(board)+" "+ move;
        }
    }
}
