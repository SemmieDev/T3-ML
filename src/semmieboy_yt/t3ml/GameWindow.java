package semmieboy_yt.t3ml;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.VolatileImage;

public class GameWindow extends JFrame {
    private static final Color background = new Color(255, 193, 99), foreground = new Color(108, 64, 0);
    private static final int lineTickness = 10, gameSize = 300, squareSize = gameSize / 3;
    private Insets insets;
    private Dimension windowSize;
    private final VolatileImage volatileImage = getGraphicsConfiguration().createCompatibleVolatileImage(gameSize, gameSize);
    private final Graphics2D graphics = volatileImage.createGraphics();
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
                int smallSize = size / 3;
                int selectionX = (event.getX() - x) / smallSize, selectionY = (event.getY() - y) / smallSize;
                if ((selectionX >= 0 && selectionX < 3) && (selectionY >= 0 && selectionY < 3)) {
                    Main.onMove(Main.positionToIndex(selectionX, selectionY));
                }
            }
        });

        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D providedGraphics = (Graphics2D)g;

        providedGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        if (insets == null) {
            insets = getInsets();
            windowSize = new Dimension(gameSize + insets.left + insets.right, gameSize + insets.top + insets.bottom);
            setMinimumSize(windowSize);
        }

        graphics.setColor(background);
        graphics.fillRect(0, 0, gameSize, gameSize);

        graphics.setColor(foreground);
        graphics.setStroke(new BasicStroke(lineTickness));

        //graphics.drawRect(0, 0, gameSize, gameSize);

        for (int i = 0; i < 2; i++) {
            int linePos = squareSize * i + squareSize - lineTickness / 2;
            graphics.fillRect(linePos, 0, lineTickness, gameSize);
            graphics.fillRect(0, linePos, gameSize, lineTickness);
        }

        byte shapeX = 0, shapeY = 0;
        int shapeMargin = 20;
        int shapeSize = squareSize - shapeMargin * 2;
        for (int i = 0; i < Main.board.length; i++) {
            switch (Main.board[i]) {
                case Main.cross -> {
                    int crossX = shapeX * squareSize + shapeMargin, crossY = shapeY * squareSize + shapeMargin;
                    graphics.drawLine(crossX, crossY, crossX + shapeSize, crossY + shapeSize);
                    graphics.drawLine(crossX + shapeSize, crossY, crossX, crossY + shapeSize);
                }
                case Main.circle -> {
                    int circleX = shapeX * squareSize + shapeMargin, circleY = shapeY * squareSize + shapeMargin;
                    graphics.drawOval(circleX, circleY, shapeSize, shapeSize);
                }
            }
            if (++shapeX == 3) {
                shapeX = 0;
                shapeY++;
            }
        }

        int width = getWidth() - insets.left - insets.right;
        int height = getHeight() - insets.top - insets.bottom;
        x = insets.left;
        y = insets.top;

        providedGraphics.clearRect(x, y, width, height);

        if (width > height) {
            size = height;
            x += width / 2 - height / 2;
        } else if (width < height) {
            size = width;
            y += height / 2 - width / 2;
        } else {
            size = width;
        }

        providedGraphics.drawImage(volatileImage.getSnapshot(), x, y, size, size, (img, infoflags, x1, y1, width1, height1) -> false);
    }
}
