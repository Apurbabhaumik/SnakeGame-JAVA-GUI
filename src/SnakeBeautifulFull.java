/*
 * SnakeBeautifulFull.java
 *
 * A polished, playable Snake game implemented in a single Java file using Swing.
 * Features:
 * - Snake body stored in LinkedList<Node> (data-structures showcase)
 * - Smooth-looking animation via interpolation between logical steps
 * - Gradient background, rounded snake segments, glowing food
 * - Scoreboard, high score, Pause/Resume, Restart button
 * - Speed increases as score grows
 *
 * Save this file as src/SnakeBeautifulFull.java
 * Compile:
 *   javac -d out src/SnakeBeautifulFull.java
 * Run:
 *   java -cp out SnakeBeautifulFull
 *
 * Study the code and ask me to explain any part — I'll walk you through the logic.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.util.*;
import java.awt.geom.RoundRectangle2D;
class Node {
    int row, col;
    Node(int r, int c) { row = r; col = c; }
}

public class SnakeBeautifulFull extends JPanel implements ActionListener, KeyListener {
    // Grid and rendering
    private final int ROWS = 20;
    private final int COLS = 28;         // slightly wider board for nicer look
    private final int CELL_SIZE = 28;    // pixel size of each cell
    private final int HEADER_HEIGHT = 64; // area for score and controls

    // Game state (logical)
    private LinkedList<Node> snake;
    private Node food;
    private char direction = 'R'; // U, D, L, R
    private boolean running = true;
    private boolean paused = false;
    private int score = 0;
    private int highScore = 0;

    // Timing & animation
    private int moveDelay = 180; // ms between logical moves
    private long lastMoveTime = 0L;
    private javax.swing.Timer frameTimer; // force Swing Timer
 // runs at ~60fps for smooth render
    private final int FRAME_DELAY = 17; // ~60 FPS

    // For interpolation between logical states
    private ArrayList<Node> prevPositions; // snake positions before last logical move
    private float interpFraction = 0f;     // 0.0 - 1.0 position between prev and current

    // UI components
    private JButton restartBtn;
    private JButton pauseBtn;

    private Random rand = new Random();

    public SnakeBeautifulFull() {
        setPreferredSize(new Dimension(COLS * CELL_SIZE, ROWS * CELL_SIZE + HEADER_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        initGame();
        createUIButtons();

        frameTimer = new javax.swing.Timer(FRAME_DELAY, this);

        frameTimer.start();
    }

    private void initGame() {
        snake = new LinkedList<>();
        int sr = ROWS / 2;
        int sc = COLS / 2;
        snake.add(new Node(sr, sc));
        snake.add(new Node(sr, sc - 1));
        snake.add(new Node(sr, sc - 2));

        prevPositions = clonePositions(snake);
        direction = 'R';
        running = true;
        paused = false;
        score = 0;
        moveDelay = 180;
        spawnFood();
        lastMoveTime = System.currentTimeMillis();
    }

    private void createUIButtons() {
        // We'll use absolute layout in this panel when adding buttons so they float over the canvas
        setLayout(null);

        restartBtn = new JButton("Restart");
        restartBtn.setFocusable(false);
        restartBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        restartBtn.setBackground(new Color(52, 152, 219));
        restartBtn.setForeground(Color.WHITE);
        restartBtn.setBorderPainted(false);
        restartBtn.addActionListener(e -> restartGame());
        restartBtn.setVisible(false); // only visible on game over
        add(restartBtn);

        pauseBtn = new JButton("Pause");
        pauseBtn.setFocusable(false);
        pauseBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pauseBtn.addActionListener(e -> togglePause());
        add(pauseBtn);

        // initial placement; will be updated in doLayout() override
    }

    @Override
    public void doLayout() {
        super.doLayout();
        // position buttons top-right
        int pad = 12;
        int btnW = 100, btnH = 34;
        pauseBtn.setBounds(getWidth() - btnW - pad, pad + 8, btnW, btnH);
        restartBtn.setBounds(getWidth() / 2 - btnW/2, getHeight() / 2 + 24, btnW, btnH);
    }

    private ArrayList<Node> clonePositions(LinkedList<Node> list) {
        ArrayList<Node> copy = new ArrayList<>();
        for (Node n : list) copy.add(new Node(n.row, n.col));
        return copy;
    }

    private void spawnFood() {
        while (true) {
            int r = rand.nextInt(ROWS);
            int c = rand.nextInt(COLS);
            boolean onSnake = false;
            for (Node n : snake) { if (n.row == r && n.col == c) { onSnake = true; break; } }
            if (!onSnake) { food = new Node(r, c); break; }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background gradient
        GradientPaint gp = new GradientPaint(0, 0, new Color(8, 40, 99), 0, getHeight(), new Color(58, 123, 213));
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Header panel (semi-transparent)
        int headerPad = 12;
        RoundRectangle2D headerBg = new RoundRectangle2D.Float(headerPad, headerPad, getWidth() - headerPad*2, HEADER_HEIGHT - 18, 18, 18);
        g2d.setColor(new Color(255,255,255,40));
        g2d.fill(headerBg);

        // Draw score
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 20));
        g2d.drawString("Score: " + score, headerPad + 14, 42);
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        g2d.drawString("High Score: " + highScore, headerPad + 14, 62);

        // Draw small controls hint
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2d.drawString("Arrow keys to move • P to pause • Restart button when game over", headerPad + 220, 42);

        // Compute interpolation fraction for rendering between prevPositions and current snake
        // interpFraction is maintained by the logic timer (computed in actionPerformed)

        // Draw the grid area background (subtle)
        int gridX = 0;
        int gridY = HEADER_HEIGHT;
        int gridW = COLS * CELL_SIZE;
        int gridH = ROWS * CELL_SIZE;

        // drop shadow for grid panel
        g2d.setColor(new Color(0,0,0,80));
        g2d.fillRoundRect(gridX + 8, gridY + 8, gridW, gridH, 12, 12);

        // panel background
        g2d.setColor(new Color(255,255,255,8));
        g2d.fillRoundRect(gridX, gridY, gridW, gridH, 12, 12);

        // draw faint grid lines
        g2d.setColor(new Color(255,255,255,30));
        for (int r=0; r<=ROWS; r++) {
            g2d.drawLine(gridX, gridY + r*CELL_SIZE, gridX + gridW, gridY + r*CELL_SIZE);
        }
        for (int c=0; c<=COLS; c++) {
            g2d.drawLine(gridX + c*CELL_SIZE, gridY, gridX + c*CELL_SIZE, gridY + gridH);
        }

        // Draw food with glow (interpolated relative to grid bounds)
        drawFood(g2d, gridX, gridY);

        // Draw snake (interpolated segments)
        drawSnake(g2d, gridX, gridY);

        // If paused
        if (paused) {
            g2d.setColor(new Color(0,0,0,120));
            g2d.fillRect(gridX, gridY, gridW, gridH);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 32));
            g2d.drawString("PAUSED", getWidth() / 2 - 60, getHeight() / 2);
        }

        // Game over overlay
        if (!running) {
            g2d.setColor(new Color(0, 0, 0, 160));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 42));
            g2d.drawString("GAME OVER", getWidth() / 2 - 140, getHeight() / 2 - 20);
            g2d.setFont(new Font("Segoe UI", Font.PLAIN, 20));
            g2d.drawString("Score: " + score, getWidth() / 2 - 50, getHeight() / 2 + 12);
            restartBtn.setVisible(true);
        }
    }

    private void drawFood(Graphics2D g2d, int gridX, int gridY) {
        // glowing apple effect with multiple concentric ovals
        int px = gridX + food.col * CELL_SIZE;
        int py = gridY + food.row * CELL_SIZE;
        int size = CELL_SIZE;

        for (int i=3; i>=0; i--) {
            int alpha = 30 + i*30;
            int pad = i*3;
            g2d.setColor(new Color(255, 60, 60, alpha));
            g2d.fillOval(px + pad, py + pad, size - pad*2, size - pad*2);
        }
        // little highlight (leaf)
        g2d.setColor(new Color(50, 205, 50));
        g2d.fillOval(px + size/2 - 4, py + 4, 8, 6);
    }

    private void drawSnake(Graphics2D g2d, int gridX, int gridY) {
        // Interpolate drawing between prevPositions and current snake positions
        // If prevPositions is null or sizes differ, skip interpolation
        int len = snake.size();
        ArrayList<Node> currPositions = clonePositions(snake);

        for (int i=0; i<len; i++) {
            Node curr = currPositions.get(i);
            Node prev = (i < prevPositions.size()) ? prevPositions.get(i) : curr;

            float rInterpolated = prev.row + (curr.row - prev.row) * interpFraction;
            float cInterpolated = prev.col + (curr.col - prev.col) * interpFraction;

            int px = Math.round(gridX + cInterpolated * CELL_SIZE);
            int py = Math.round(gridY + rInterpolated * CELL_SIZE);

            // color gradient head -> tail
            if (i == 0) g2d.setColor(new Color(95, 219, 118));
            else {
                float t = (float)i / Math.max(1, len-1);
                // interpolate color between light-green and darker
                int rcol = (int)(144 + (95 - 144) * (1 - t));
                int gcol = (int)(238 + (219 - 238) * (1 - t));
                int bcol = (int)(144 + (118 - 144) * (1 - t));
                g2d.setColor(new Color(rcol, gcol, bcol));
            }

            g2d.fillRoundRect(px + 4, py + 4, CELL_SIZE - 8, CELL_SIZE - 8, 18, 18);

            // small inner shine for 3d look
            g2d.setColor(new Color(255,255,255, 40));
            g2d.fillRoundRect(px + 6, py + 6, CELL_SIZE - 12, CELL_SIZE - 12, 14, 14);
        }
    }

    // Game logic: perform a logical move when enough time has elapsed
    private void updateGameLogic() {
        if (!running || paused) return;
        long now = System.currentTimeMillis();
        long elapsed = now - lastMoveTime;
        if (elapsed >= moveDelay) {
            // prepare prevPositions for interpolation
            prevPositions = clonePositions(snake);

            // perform logical move
            Node head = snake.getFirst();
            int newRow = head.row;
            int newCol = head.col;
            switch (direction) {
                case 'U': newRow--; break;
                case 'D': newRow++; break;
                case 'L': newCol--; break;
                case 'R': newCol++; break;
            }

            // check collisions with walls
            if (newRow < 0 || newRow >= ROWS || newCol < 0 || newCol >= COLS) {
                running = false;
                restartBtn.setVisible(true);
                return;
            }

            // check collisions with self
            for (Node n : snake) {
                if (n.row == newRow && n.col == newCol) {
                    running = false;
                    restartBtn.setVisible(true);
                    return;
                }
            }

            // add new head
            snake.addFirst(new Node(newRow, newCol));

            // check food
            if (newRow == food.row && newCol == food.col) {
                score += 10;
                if (score > highScore) highScore = score;
                spawnFood();
                // increase speed a bit
                if (moveDelay > 60) moveDelay -= 6;
            } else {
                // normal move: remove tail
                snake.removeLast();
            }

            // reset interpolation timer
            lastMoveTime = now;
            interpFraction = 0f;
        } else {
            // compute interpolation fraction between 0 and 1
            interpFraction = Math.min(1.0f, (float)elapsed / (float)moveDelay);
        }
    }

    private void restartGame() {
        initGame();
        restartBtn.setVisible(false);
        requestFocusInWindow();
    }

    private void togglePause() {
        if (!running) return;
        paused = !paused;
        pauseBtn.setText(paused ? "Resume" : "Pause");
        requestFocusInWindow();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // called each frame (~60fps)
        updateGameLogic();
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_UP && direction != 'D') direction = 'U';
        else if (key == KeyEvent.VK_DOWN && direction != 'U') direction = 'D';
        else if (key == KeyEvent.VK_LEFT && direction != 'R') direction = 'L';
        else if (key == KeyEvent.VK_RIGHT && direction != 'L') direction = 'R';
        else if (key == KeyEvent.VK_P) togglePause();
        else if (key == KeyEvent.VK_R) restartGame();
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Snake Game - High-End Edition");
            SnakeBeautifulFull game = new SnakeBeautifulFull();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            game.requestFocusInWindow();
        });
    }
}
