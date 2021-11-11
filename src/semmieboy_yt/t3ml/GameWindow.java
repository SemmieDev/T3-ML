package semmieboy_yt.t3ml;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;

public class GameWindow extends JFrame {
    private static final Color
            foreground = new Color(108, 64, 0),
            stripe = new Color(197, 38, 38);
    private static final int gameSize = 300;
    private static final BufferedImage background;

    static {
        background = new BufferedImage(gameSize, gameSize, Image.SCALE_FAST);
        Graphics2D graphics = background.createGraphics();

        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.setColor(new Color(255, 193, 99));
        graphics.fillRect(0, 0, gameSize, gameSize);

        graphics.setColor(foreground);
        int lineThickness = 10;
        graphics.setStroke(new BasicStroke(lineThickness));

        //graphics.drawRect(0, 0, gameSize, gameSize);

        byte squareSize = gameSize / 3;
        for (int i = 0; i < 2; i++) {
            int linePos = squareSize * i + squareSize - lineThickness / 2;
            graphics.fillRect(linePos, 0, lineThickness, gameSize);
            graphics.fillRect(0, linePos, gameSize, lineThickness);
        }

        graphics.dispose();
    }

    private Insets insets;
    private int x, y, size;

    public GameWindow() {
        super("T3-ML");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        System.setProperty("sun.java2d.opengl", "true");
        createBufferStrategy(2);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (!Main.autoLearn) {
                    int smallSize = size / 3;
                    int selectionX = (event.getX() - x) / smallSize, selectionY = (event.getY() - y) / smallSize;
                    if ((selectionX >= 0 && selectionX < 3) && (selectionY >= 0 && selectionY < 3)) {
                        Main.onMove(Main.pti(selectionX, selectionY));
                    }
                }
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_A) {
                    Main.autoLearn = !Main.autoLearn;
                    if (Main.autoLearn) {
                        synchronized (Main.autoLearnLock) {
                            Main.autoLearnLock.notifyAll();
                        }
                    }
                }
            }
        });
    }

    @Override
    public void paint(Graphics g) {
        if (insets == null) {
            insets = getInsets();
            Dimension windowSize = new Dimension(gameSize + insets.left + insets.right, gameSize + insets.top + insets.bottom);
            setMinimumSize(windowSize);
        }

        int width = getWidth() - insets.left - insets.right;
        int height = getHeight() - insets.top - insets.bottom;
        x = insets.left;
        y = insets.top;

        if (width > height) {
            size = height;
            x += width / 2 - height / 2;
        } else if (width < height) {
            size = width;
            y += height / 2 - width / 2;
        } else {
            size = width;
        }

        Graphics2D providedGraphics = (Graphics2D)g;
        providedGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        VolatileImage volatileImage = createVolatileImage(size, size);
        Graphics2D graphics = volatileImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        graphics.drawImage(background, 0, 0, size, size, (img, infoflags, x, y, width12, height12) -> false);

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float lineThickness = size / 30f;
        graphics.setStroke(new BasicStroke(lineThickness));
        graphics.setColor(foreground);

        byte shapeX = 0, shapeY = 0;
        int squareSize = size / 3, shapeMargin = size / 15;
        int shapeSize = squareSize - shapeMargin * 2;
        for (int i = 0; i < Main.board.length; i++) {
            switch (Main.board[i]) {
                case Main.cross: {
                    int crossX = shapeX * squareSize + shapeMargin, crossY = shapeY * squareSize + shapeMargin;
                    graphics.drawLine(crossX, crossY, crossX + shapeSize, crossY + shapeSize);
                    graphics.drawLine(crossX + shapeSize, crossY, crossX, crossY + shapeSize);
                    break;
                }
                case Main.circle: {
                    int circleX = shapeX * squareSize + shapeMargin, circleY = shapeY * squareSize + shapeMargin;
                    graphics.drawOval(circleX, circleY, shapeSize, shapeSize);
                    break;
                }
            }

            if (++shapeX == 3) {
                shapeX = 0;
                shapeY++;
            }
        }

        if (Main.gameOver && !Main.tie) {
            byte[] pos1 = Main.itp(Main.win[0]);
            byte[] pos2 = Main.itp(Main.win[1]);
            graphics.setStroke(new BasicStroke(lineThickness / 2));
            graphics.setColor(stripe);
            graphics.drawLine(
                    pos1[0] * squareSize + squareSize / 2,
                    pos1[1] * squareSize + squareSize / 2,
                    pos2[0] * squareSize + squareSize / 2,
                    pos2[1] * squareSize + squareSize / 2
            );
        }

        providedGraphics.clearRect(insets.left, insets.top, width, height);

        providedGraphics.drawImage(volatileImage.getSnapshot(), x, y, size, size, (img, infoflags, x1, y1, width1, height1) -> false);
        graphics.dispose();
        providedGraphics.dispose();
    }
}
