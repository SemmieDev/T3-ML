package semmieboy_yt.t3ml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Brain {
    public static volatile boolean isThinking = false;

    private static final ArrayList<Memory> memories = new ArrayList<>();
    private static final File saveFile = new File("brain.dat");
    private static final Random random = new Random();

    public static void debug() {
        byte[] board = new byte[9];
        byte[] square = new byte[1];
        for (int i = 0; i < 10000; i++) {
            random.nextBytes(board);
            random.nextBytes(square);
            memories.add(new Memory(random.nextBoolean(), board, square[0]));
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

    public static byte processMove() {
        isThinking = true;

        AtomicReference<Byte> move = new AtomicReference<>((byte)-1);
        ArrayList<Byte> moves = new ArrayList<>();
        for (byte i = 0; i < Main.board.length; i++) if (Main.board[i] == Main.none) moves.add(i);

        if (moves.isEmpty()) {
            Main.gameOver = true;
        } else {
            memories.forEach(memory -> {
                if (memoryMatchesBoard(memory)) {
                    if (memory.goodMove) {
                        Main.board[memory.move] = Main.circle;
                        move.set(memory.move);
                    } else {
                        moves.remove(memory.move);
                    }
                }
            });

            if (move.get() == -1) {
                // TODO: 10/10/2021 Remember if it was a good or bad move
                move.set(moves.get(random.nextInt(moves.size())));
                Main.board[move.get()] = Main.circle;
            }
        }

        isThinking = false;
        return move.get();
    }

    private static boolean memoryMatchesBoard(Memory memory) {
        // TODO: 10/10/2021 Check if the memory is the same as the board, on all rotations
        return false;
    }

    private static record Memory(boolean goodMove, byte[] board, byte move) {
        public static int SIZE = 11;

        public Memory(byte[] data) {
            this (1 == data[0], Arrays.copyOfRange(data, 1, 10), data[10]);
        }

        public byte[] toArray() {
            byte[] data = new byte[SIZE];
            data[0] = (byte)(goodMove ? 1 : 0);
            data[10] = move;
            System.arraycopy(board, 0, data, 1, board.length);
            return data;
        }

        @Override
        public String toString() {
            return goodMove+" "+Arrays.toString(board)+" "+ move;
        }
    }
}
