package semmieboy_yt.t3ml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
            try (GZIPInputStream saveFileInput = new GZIPInputStream(new FileInputStream(saveFile))) {
                while (true) {
                    byte[] data = new byte[Memory.SIZE];
                    if (saveFileInput.read(data) == Memory.SIZE) {
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

    public static boolean processMove() {
        isThinking = true;

        Main.print("Thinking");
        boolean checkWin = true;
        lostMemory = null;
        AtomicInteger move = new AtomicInteger(-1);
        ArrayList<Byte> possibleMoves = new ArrayList<>();
        for (byte i = 0; i < Main.board.length; i++) if (Main.board[i] == Main.none) possibleMoves.add(i);

        if (possibleMoves.isEmpty()) {
            Main.gameOver = true;
            Main.tie = true;
        } else {
            AtomicReference<Memory> bestMemory = new AtomicReference<>();
            ArrayList<Byte> smartMoves = new ArrayList<>(possibleMoves);
            ArrayList<Memory> memories = new ArrayList<>(Brain.memories);
            memories.forEach(m -> {
                Memory memory = m.copy();
                if (memoryMatchesBoard(memory)) {
                    if (memory.points < 0) {
                        Main.print("Removing bad move: "+memory.move);
                        smartMoves.remove((Object)memory.move);
                    } else if (bestMemory.get() == null || memory.points > bestMemory.get().points) {
                        // Don't integrate this if statement in the above if statement, or the else block will be called
                        if (Main.board[memory.move] == Main.none) {
                            Main.print("Best move so far: "+memory.move);
                            bestMemory.set(memory);
                        }
                    } else {
                        // By the time this get reached, the memory is always the worst memory
                        Main.print("Forgetting worse move: "+memory.move);
                        Brain.memories.remove(memory);
                    }
                }
            });
            memories.clear();

            if (smartMoves.isEmpty()) {
                // TODO: 10/20/2021 Add a bad memory with the move that caused it to get trapped
                Main.print("Trapped");
                Main.board[possibleMoves.get(Main.random.nextInt(possibleMoves.size()))] = Main.circle;
            } else {
                if (bestMemory.get() == null) {
                    Main.print("Don't know any good moves");
                    byte[] board = Arrays.copyOf(Main.board, Main.board.length);
                    move.set(smartMoves.get(Main.random.nextInt(smartMoves.size())));
                    Main.board[move.get()] = Main.circle;
                    checkWin = false;
                    Main.checkWin();

                    if (Main.gameOver) {
                        // Game over after AI did something, so AI won
                        Main.print("Remembering good move: "+move.get());
                        Brain.memories.add(new Memory((byte)100, board, (byte)move.get()));
                    } else {
                        lostMemory = new Memory((byte)-100, board, (byte)move.get());
                    }
                } else {
                    Main.print("Doing best move: "+bestMemory.get().move);
                    Main.board[bestMemory.get().move] = Main.circle;
                }
            }
        }
        Main.print("Done thinking");

        isThinking = false;
        synchronized (thinkingLock) {
            thinkingLock.notifyAll();
        }
        return checkWin;
    }

    public static void onGameOver() {
        if (lostMemory != null) {
            Main.print("Remembering bad move: "+lostMemory.move);
            memories.add(lostMemory.copy());
            lostMemory = null;
        }
    }

    private static boolean memoryMatchesBoard(Memory memory) {
        if (Arrays.equals(Main.board, memory.board)) return true;
        byte[] board = Arrays.copyOf(Main.board, Main.board.length);
        byte[] before;

        // rotate the board, then check if it equals
        for (byte i = 0; i < 4; i++) {
            before = Arrays.copyOf(board, board.length);
            for (byte j = 0; j < board.length; j++) board[j] = before[rotate(j)];
            memory.move = rotate(memory.move);
            if (Arrays.equals(board, memory.board)) return true;
        }
        return false;
    }

    private static byte rotate(byte index) {
        switch (index) {
            case 0: return 2;
            case 1: return 5;
            case 2: return 8;
            case 3: return 1;
            case 5: return 7;
            case 6: return 0;
            case 7: return 3;
            case 8: return 6;
        }
        return index;
    }

    private static class Memory {
        public static int SIZE = 11;

        public byte points, move;
        public final byte[] board;

        public Memory(byte points, byte[] board, byte move) {
            this.points = points;
            this.board = board;
            this.move = move;
        }

        public Memory(byte[] data) {
            this(data[0], Arrays.copyOfRange(data, 1, 10), data[10]);
        }

        public byte[] toArray() {
            byte[] data = new byte[SIZE];
            data[0] = points;
            System.arraycopy(board, 0, data, 1, board.length);
            data[10] = move;
            return data;
        }

        public Memory copy() {
            return new Memory(points, board, move);
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof Memory) {
                Memory memory = (Memory)object;
                return memory.points == points && Arrays.equals(memory.board, board) && memory.move == move;
            }
            return false;
        }

        @Override
        public String toString() {
            return points+" "+Arrays.toString(board)+" "+ move;
        }
    }
}
