package semmieboy_yt.t3ml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
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

    static {
        if (System.getProperty("libraries.path.set") == null) {
            System.out.println("Setting library path");
            StringBuilder command = new StringBuilder();

            ProcessHandle.current().info().command().ifPresentOrElse(jvm -> {
                command.append("\"").append(jvm).append("\" ");
            }, () -> {
                throw new RuntimeException("Unable to find the JVM location");
            });

            ManagementFactory.getRuntimeMXBean().getInputArguments().forEach(argument -> command.append(argument).append(" "));

            File natives = new File("natives");
            if (!natives.isDirectory() && !natives.mkdir()) throw new RuntimeException("Unable to create natives directory");

            command.append("-Dlibraries.path.set=true ")
                    .append("-Djava.library.path=").append(natives.getAbsolutePath())
                    .append(" -cp ").append(System.getProperty("java.class.path"))
                    .append(" ").append(Main.class.getName());

            String sunCommand = System.getProperty("sun.java.command");
            int sunCommandIndex = sunCommand.indexOf(" ");
            if (sunCommandIndex != -1) command.append(sunCommand.substring(sunCommandIndex));

            System.out.println("Attempting to start process");
            try {
                System.exit(new ProcessBuilder(command.toString().split(" ")).inheritIO().start().waitFor());
            } catch (InterruptedException | IOException exception) {
                throw new RuntimeException("Unable to start process", exception);
            }
        }

        String os = System.getProperty("os.name").toLowerCase().replaceAll("\s", "");
        if (os.contains("win")) {
            System.out.println("Detected Windows");
            extractLib("sdl2gdx.dll");
            extractLib("sdl2gdx64.dll");
        } else if (os.contains("mac") || os.contains("osx")) {
            System.out.println("Detected Mac");
            extractLib("libsdl2gdx64.dylib");
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix") || os.contains("sunos")) {
            System.out.println("Detected Linux");
            extractLib("libsdl2gdx64.so");
        } else {
            throw new RuntimeException("Unsupported OS: "+System.getProperty("os.name"));
        }
    }

    private static void extractLib(String name) {
        File library = new File("natives"+File.separator+name);
        if (!library.isFile()) {
            System.out.println("Extracting library "+name);
            try (FileOutputStream libraryFile = new FileOutputStream(library)) {
                InputStream libraryStream = Main.class.getResourceAsStream("/"+name);
                if (libraryStream == null) throw new RuntimeException("Couldn't find library: "+name);
                libraryStream.transferTo(libraryFile);
            } catch (IOException exception) {
                throw new RuntimeException("Couldn't extract library: "+name, exception);
            }
        }
    }

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
                    ArrayList<Byte> moves = new ArrayList<>();
                    for (byte i = 0; i < board.length; i++) if (board[i] == none) moves.add(i);
                    if (moves.isEmpty()) {
                        gameOver = tie = true;
                        onMove((byte)0);
                    } else {
                        onMove(moves.get(random.nextInt(moves.size())));
                    }
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

    public static byte pti(int x, int y) {
        return (byte)(x + y * 3);
    }

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
