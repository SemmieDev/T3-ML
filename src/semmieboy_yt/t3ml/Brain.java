package semmieboy_yt.t3ml;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
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
        byte[] squareAndPoints = new byte[2];
        for (int i = 0; i < 10000; i++) {
            random.nextBytes(board);
            random.nextBytes(squareAndPoints);
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

        AtomicReference<Byte> move = new AtomicReference<>((byte)-1);
        ArrayList<Byte> moves = new ArrayList<>();
        for (byte i = 0; i < Main.board.length; i++) if (Main.board[i] != Main.cross && Main.board[i] != Main.circle) moves.add(i);

        if (moves.isEmpty()) {
            Main.gameOver = true;
            Main.tie = true;
        } else {
            AtomicReference<Memory> bestMemory = new AtomicReference<>();
            memories.forEach(m -> {
                Memory memory = m.copy();
                if (memoryMatchesBoard(memory)) {
                    if (memory.points < 0) {
                        moves.remove(memory.move);
                    } else if (bestMemory.get() == null || memory.points > bestMemory.get().points) {
                        bestMemory.set(memory);
                    } else {
                        memories.remove(memory);
                    }
                }
            });

            if (bestMemory.get() == null) {
                // TODO: 10/14/2021 Make algorithm for points

                byte[] board = Arrays.copyOf(Main.board, Main.board.length);
                move.set(moves.get(random.nextInt(moves.size())));
                Main.board[move.get()] = Main.circle;
                Main.gameWindow.repaint();

                AtomicReference<Byte> points = new AtomicReference<>();

                JFrame jFrame = new JFrame("How good did I do?");
                Container container = jFrame.getContentPane();

                JTextField pointsInput = new JTextField();
                pointsInput.setBounds(0, 0, 300, 300);
                container.add(pointsInput);

                jFrame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent event) {
                        points.set(Byte.parseByte(pointsInput.getText()));
                        jFrame.dispose();
                        synchronized (jFrame) {
                            jFrame.notifyAll();
                        }
                    }
                });

                jFrame.setSize(300, 300);
                jFrame.setVisible(true);

                while (jFrame.isVisible()) {
                    try {
                        synchronized (jFrame) {
                            jFrame.wait();
                        }
                    } catch (InterruptedException ignored) {}
                }

                if (points.get() != null) memories.add(new Memory(points.get(), board, move.get()));
            } else {
                Main.board[bestMemory.get().move] = Main.circle;
            }
        }

        isThinking = false;
    }

    private static boolean memoryMatchesBoard(Memory memory) {
        if (Arrays.equals(Main.board, memory.board)) return true;
        byte[] board = Arrays.copyOf(Main.board, Main.board.length);
        byte[] before;

        // rotate the board, then check if it equals
        for (byte i = 0; i < 8; i++) {
            before = Arrays.copyOf(board, board.length);
            for (byte j = 0; j < board.length; j++) board[j] = before[rotate(j)];
            memory.move = rotate(memory.move);
            if (Arrays.equals(board, memory.board)) return true;
        }
        return false;
    }

    private static byte rotate(byte index) {
        switch (index) {
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
        public final byte[] board;

        public Memory(byte points, byte[] board, byte move) {
            this.points = points;
            this.board = board;
            this.move = move;
        }

        public Memory(byte[] data) {
            this (data[0], Arrays.copyOfRange(data, 1, 10), data[10]);
        }

        public byte[] toArray() {
            byte[] data = new byte[SIZE];
            data[0] = points;
            data[10] = move;
            System.arraycopy(board, 0, data, 1, board.length);
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
