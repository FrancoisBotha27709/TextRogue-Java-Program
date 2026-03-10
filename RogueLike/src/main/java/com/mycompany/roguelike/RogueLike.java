package com.mycompany.roguelike;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

public class RogueLike extends JFrame {

    static HashMap<Point, Tile> world = new HashMap<>();
    static Player player;
    static int inventorySelectedIndex = 0;
    static boolean inventoryMode = false;
    static final int MAP_WIDTH = 60, MAP_HEIGHT = 25;
    static final int VIEW_X = MAP_WIDTH / 2, VIEW_Y = MAP_HEIGHT / 2;

    static List<String> log = new ArrayList<>();
    static List<Weapon> weapons = new ArrayList<>();
    static List<Enemy> enemies = new ArrayList<>();
    static List<String> names = new ArrayList<>();
    static Random rng = new Random();

    GamePanel panel = new GamePanel();
    static GamePanel panelRef;

    public RogueLike() {
        setTitle("Roguelike");
        setSize(1000, 700); // initial size
        setExtendedState(JFrame.MAXIMIZED_BOTH); // maximize window
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        add(panel);
        panelRef = panel;
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (inventoryMode) {
                    handleInventoryInput(e);
                } else {
                    handleGameInput(e);
                }
                panel.repaint();
            }
        });
        GenerateStart();
        setVisible(true);
    }

    public static void main(String[] args) {
        new RogueLike();
    }
    // Add this inside your RogueLike class

    static String[] GetDeathArt() {
        return new String[]{
            "+---------------------------------------------------------------------------------------------+",
            "                                                                                              ",
            "       ##### /    ##                                ##### ##                          ##      ",
            "    ######  /  #####                             /#####  /##      #                    ##     ",
            "   /#   /  /     #####                         //    /  / ###    ###                   ##     ",
            "  /    /  ##     # ##                         /     /  /   ###    #                    ##     ",
            "      /  ###     #                                 /  /     ###                        ##     ",
            "     ##   ##     #    /###   ##   ####            ## ##      ## ###       /##      ### ##     ",
            "     ##   ##     #   / ###  / ##    ###  /        ## ##      ##  ###     / ###    #########   ",
            "     ##   ##     #  /   ###/  ##     ###/         ## ##      ##   ##    /   ###  ##   ####    ",
            "     ##   ##     # ##    ##   ##      ##          ## ##      ##   ##   ##    ### ##    ##     ",
            "     ##   ##     # ##    ##   ##      ##          ## ##      ##   ##   ########  ##    ##     ",
            "      ##  ##     # ##    ##   ##      ##          #  ##      ##   ##   #######   ##    ##     ",
            "       ## #      # ##    ##   ##      ##             /       /    ##   ##        ##    ##     ",
            "        ###      # ##    ##   ##      /#        /###/       /     ##   ####    / ##    /#     ",
            "         #########  ######     ######/ ##      /   ########/      ### / ######/   ####/       ",
            "           #### ###  ####       #####   ##    /       ####         ##/   #####     ###        ",
            "                 ###                          #                                               ",
            "     ########     ###                          ##                                             ",
            "   /############  /#                                                                          ",
            "  /           ###/                                                                            ",
            "+--------------------------------------------------------------------------------------------+"
        };
    }

    static void FadeDeathText(boolean fadeIn) {
        GamePanel panel = panelRef; // use the actual panel
        if (panel == null) {
            return;   // safety check
        }
        String[] deathArt = GetDeathArt();
        int steps = 5;
        final int[] step = {fadeIn ? 0 : steps};
        final int increment = fadeIn ? 1 : -1;

        Timer timer = new Timer(250, null);
        timer.addActionListener(e -> {
            panel.setDeathOverlay(deathArt, step[0], steps);
            panel.repaint();

            step[0] += increment;
            if ((fadeIn && step[0] > steps) || (!fadeIn && step[0] < 0)) {
                ((Timer) e.getSource()).stop();
                if (fadeIn) {
                    FadeDeathText(false); // start fade-out after fade-in
                } else {
                    // reset game after fade-out
                    world.clear();
                    log.clear();
                    GenerateStart();
                    panel.clearDeathOverlay();
                    panel.repaint();
                }
            }
        });
        timer.start();
    }

    // ========================= INPUT =============================
    private void handleGameInput(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W:
                Move(0, -1);
                break;
            case KeyEvent.VK_S:
                Move(0, 1);
                break;
            case KeyEvent.VK_A:
                Move(-1, 0);
                break;
            case KeyEvent.VK_D:
                Move(1, 0);
                break;
            case KeyEvent.VK_E:
                Interact();
                break;
            case KeyEvent.VK_Q:
                inventoryMode = true;
                break;
        }
    }

    private void handleInventoryInput(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_Q:
                inventoryMode = false;
                break;
            case KeyEvent.VK_W:
                inventorySelectedIndex--;
                if (inventorySelectedIndex < 0) {
                    inventorySelectedIndex = player.Inventory.size() - 1;
                }
                break;
            case KeyEvent.VK_S:
                inventorySelectedIndex++;
                if (inventorySelectedIndex >= player.Inventory.size()) {
                    inventorySelectedIndex = 0;
                }
                break;
            case KeyEvent.VK_E:
                if (!player.Inventory.isEmpty()) {
                    Weapon w = player.Inventory.get(inventorySelectedIndex);
                    Weapon old = player.EquippedWeapon;
                    player.EquippedWeapon = w;
                    player.Inventory.set(inventorySelectedIndex, old);
                    if (old == null) {
                        player.Inventory.remove(inventorySelectedIndex);
                    }
                    Log("Equipped " + w.Name);
                }
                break;
        }
    }

    // ========================= MOVEMENT ==========================
    static void Move(int dx, int dy) {
        int nx = player.X + dx;
        int ny = player.Y + dy;

        Tile t = GetOrCreate(nx, ny);
        if (!t.Walkable) {
            Log("You cannot go there.");
            return;
        }

        player.X = nx;
        player.Y = ny;
        if (player.health != 100)
            player.health++;
        t.Explored = true;
        DescribeTile(t);
    }

    // ========================= WORLD GENERATION ==================
    static Tile GetOrCreate(int x, int y) {
        Point p = new Point(x, y);
        if (world.containsKey(p)) {
            return world.get(p);
        }
        if (RandomChance(0.3)) {
            return CreateArea(x, y, false);
        } else {
            return CreatePath(x, y);
        }
    }

    static Tile CreateArea(int x, int y, boolean isStart) {
        String n = RandomFrom(names);
        Tile t = new Tile();
        t.X = x;
        t.Y = y;
        t.isStart = isStart;
        t.Name = n;
        t.Symbol = n.charAt(0);
        t.Walkable = true;
        if (RandomChance(0.3)) {
            t.Item = RandomFrom(weapons);
        }
        if (RandomChance(0.3)) {
            Enemy e = RandomFrom(enemies);
            t.Enemy = new Enemy(e.Name, e.health);
            t.Enemy.Weapon = RandomFrom(weapons);
        }
        world.put(new Point(x, y), t);
        return t;
    }

    static Tile CreatePath(int x, int y) {
        Tile t = new Tile();
        t.X = x;
        t.Y = y;
        t.Walkable = true;
        t.Symbol = '-';
        world.put(new Point(x, y), t);
        return t;
    }

    static void GenerateStart() {
        generateEnemies();
        generateWeapons();
        generateNames();
        Tile start = CreateArea(0, 0, true);
        player = new Player();
        player.X = 0;
        player.Y = 0;
        player.EquippedWeapon = new Weapon("Halberd", 10);
        start.Explored = true;
        Log("You wake up in a strange place.");
    }

    static void generateWeapons() {
        weapons.clear();
        weapons.add(new Weapon("Sword", RandomFrom(1, 20)));
        weapons.add(new Weapon("Spear", RandomFrom(1, 20)));
        weapons.add(new Weapon("Halberd", RandomFrom(1, 20)));
        weapons.add(new Weapon("Stick", RandomFrom(1, 20)));
        weapons.add(new Weapon("Knife", RandomFrom(1, 20)));
    }

    static void generateEnemies() {
        enemies.clear();
        enemies.add(new Enemy("Goblin", RandomFrom(30, 60)));
        enemies.add(new Enemy("Orc", RandomFrom(40, 100)));
        enemies.add(new Enemy("Skeleton", RandomFrom(10, 50)));
    }

    static void generateNames() {
        names = Arrays.asList("Galley", "River", "Bridge", "Gateway", "Market", "Garden", "Well", "House");
    }

    // ========================= TILE INTERACTIONS =================
    static void Interact() {
        Tile t = world.get(new Point(player.X, player.Y));
        if (t == null) {
            return;
        }
        boolean action = false;
        if (t.Enemy != null) {
            Enemy e = t.Enemy;
            int dmg = player.EquippedWeapon != null ? player.EquippedWeapon.Damage : 5;
            Log("You attack " + e.Name + " for " + dmg + " damage.");
            e.health -= dmg;
            if (e.health <= 0) {
                Log(e.Name + " dies.");
                if (RandomChance(0.5) && e.Weapon != null) {
                    t.Item = e.Weapon;
                    Log(e.Name + " dropped " + e.Weapon.Name);
                }
                t.Enemy = null;
            } else {
                int edmg = e.Weapon != null ? e.Weapon.Damage : 2;
                Log(e.Name + " hits you for " + edmg + " HP!");
                player.health -= edmg;
                if (player.health <= 0) {
                    PlayerDied();
                }
            }
            action = true;
        }
        if (t.Item != null) {
            Log("You pick up " + t.Item.Name + " (DMG: " + t.Item.Damage + ")");
            player.Inventory.add(t.Item);
            t.Item = null;
            action = true;
        }
        if (!action) {
            Log("Nothing to interact with.");
        }
    }

    static void DescribeTile(Tile t) {
        if (t.Symbol != '-') {
            Log("You are in " + t.Name + ".");
        }
        if (t.Enemy != null) {
            Log("A " + t.Enemy.Name + " is here. HP: " + t.Enemy.health);
        }
        if (t.Item != null) {
            Log("There is a " + t.Item.Name + " here.");
        }
    }

    static void PlayerDied() {
        Log("You died.");
        FadeDeathText(true);
    }

    // ========================= UTIL =============================
    static void Log(String s) {
        log.add("> " + s);
        if (log.size() > 20) {
            log.remove(0);
        }
    }

    static boolean RandomChance(double p) {
        return rng.nextDouble() < p;
    }

    static <T> T RandomFrom(List<T> l) {
        return l.get(rng.nextInt(l.size()));
    }

    static int RandomFrom(int min, int max) {
        return rng.nextInt(max - min + 1) + min;
    }

    // ========================= RENDERING ========================
    class GamePanel extends JPanel {

        // inside GamePanel
        boolean IsCorridor(int x, int y) {
            Tile t = world.get(new Point(x, y));
            return t != null && t.Walkable && t.Symbol == '-';
        }

        char GetCorridorSymbol(int x, int y) {
            boolean up = IsCorridor(x, y - 1);
            boolean down = IsCorridor(x, y + 1);
            boolean left = IsCorridor(x - 1, y);
            boolean right = IsCorridor(x + 1, y);

            int count = (up ? 1 : 0) + (down ? 1 : 0) + (left ? 1 : 0) + (right ? 1 : 0);

            if (count == 4) {
                return '┼';
            }
            if (up && down && left && !right) {
                return '┤';
            }
            if (up && down && !left && right) {
                return '├';
            }
            if (up && !down && left && right) {
                return '┴';
            }
            if (!up && down && left && right) {
                return '┬';
            }
            if (up && down && !left && !right) {
                return '│';
            }
            if (!up && !down && left && right) {
                return '─';
            }
            if (!up && down && !left && right) {
                return '┌';
            }
            if (!up && down && left && !right) {
                return '┐';
            }
            if (up && !down && !left && right) {
                return '└';
            }
            if (up && !down && left && !right) {
                return '┘';
            }

            return '.'; // default for isolated tile
        }

        Font font = new Font("Monospaced", Font.PLAIN, 16);

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBackground(Color.BLACK);
            Graphics2D g2d = (Graphics2D) g.create();

            int panelWidth = getWidth();
            int panelHeight = getHeight();

            // map view size in tiles
            int mapCols = VIEW_X * 2 + 1;
            int mapRows = VIEW_Y * 2 + 1;

            // dynamic cell size based on panel
            int cellWidth = panelWidth / (mapCols + 20);   // extra space for log/HUD
            int cellHeight = panelHeight / (mapRows + 10); // extra space for HUD/log
            int cellSize = (int)(Math.min(cellWidth, cellHeight) * 0.96); // 70% of original size

            // draw map
            if (!inventoryMode) {
                int minX = player.X - VIEW_X;
                int maxX = player.X + VIEW_X;
                int minY = player.Y - VIEW_Y;
                int maxY = player.Y + VIEW_Y;

                for (int y = minY, row = 0; y <= maxY; y++, row++) {
                    for (int x = minX, col = 0; x <= maxX; x++, col++) {
                        char c = ' ';
                        Color color = Color.LIGHT_GRAY;

                        if (player.X == x && player.Y == y) {
                            c = '#';
                            color = Color.YELLOW;
                        } else {
                            Tile t = world.get(new Point(x, y));
                            if (t != null) {
                                if (t.Enemy != null) {
                                    c = 'E';
                                    color = Color.RED;
                                } else if (t.Item != null) {
                                    c = 'i';
                                    color = Color.GREEN;
                                } else if (t.Symbol == '-') {
                                    c = GetCorridorSymbol(x, y);
                                } else {
                                    c = t.Symbol;
                                }
                            }
                        }

                        g2d.setColor(color);
                        g2d.setFont(new Font("Monospaced", Font.PLAIN, cellSize));
                        g2d.drawString("" + c, col * cellSize, row * cellSize + cellSize);
                    }
                }

                // map borders
                g2d.setColor(Color.GRAY);
                for (int y = 0; y <= maxY - minY; y++) {
                    g2d.drawString("|", 0, y * cellSize + cellSize);
                    g2d.drawString("|", mapCols * cellSize, y * cellSize + cellSize);
                }
                for (int x = 0; x <= maxX - minX; x++) {
                    g2d.drawString("-", x * cellSize, cellSize);
                    g2d.drawString("-", x * cellSize, (mapRows + 1) * cellSize);
                }
            }

            // ===== HUD at bottom =====
            int hudY = panelHeight - 3 * cellSize;
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Monospaced", Font.PLAIN, cellSize));
            g2d.drawString("Controls: W/A/S/D = Move, E = Interact, Q = Inventory", cellSize, hudY);
            hudY += cellSize;

            String weaponName = player.EquippedWeapon != null ? player.EquippedWeapon.Name : "None";
            int weaponDmg = player.EquippedWeapon != null ? player.EquippedWeapon.Damage : 0;
            g2d.drawString(
                    "HP: " + player.health + "  Stamina: " + player.stamina
                    + "  Weapon: " + weaponName + " (DMG: " + weaponDmg + ")"
                    + "  Pos: (" + player.X + "," + player.Y + ")",
                    cellSize, hudY
            );

            // draw log on right
            int logX = panelWidth - 20 * cellSize; // 20 cells from right
            int logY = cellSize;
            g2d.setColor(Color.CYAN);
            for (String s : log) {
                g2d.drawString(s, logX, logY);
                logY += cellSize;
            }

            // draw inventory when active
            if (inventoryMode) {
                int invX = cellSize;
                int invY = cellSize;
                g2d.setColor(Color.CYAN);
                g2d.drawString("Inventory:", invX, invY);
                invY += cellSize;
                for (int i = 0; i < player.Inventory.size(); i++) {
                    String marker = i == inventorySelectedIndex ? "*" : " ";
                    Weapon w = player.Inventory.get(i);
                    g2d.drawString(marker + " " + w.Name + " DMG:" + w.Damage, invX, invY);
                    invY += cellSize;
                }
            }

            // death overlay
            if (deathArt != null) {
                float alpha = (float) deathStep / deathMaxStep; // 0..1
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2d.setColor(Color.RED);
                g2d.fillRect(0, 0, panelWidth, panelHeight);

                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Monospaced", Font.BOLD, panelHeight / 25));
                int startY = panelHeight / 2 - deathArt.length * (panelHeight / 50);
                for (String line : deathArt) {
                    int startX = panelWidth / 2 - g2d.getFontMetrics().stringWidth(line) / 2;
                    g2d.drawString(line, startX, startY);
                    startY += panelHeight / 25;
                }
            }

            g2d.dispose();
        }
        // Inside GamePanel, add:
        private String[] deathArt = null;
        private int deathStep = 0;
        private int deathMaxStep = 0;

        void setDeathOverlay(String[] art, int step, int maxStep) {
            this.deathArt = art;
            this.deathStep = step;
            this.deathMaxStep = maxStep;
        }

        void clearDeathOverlay() {
            this.deathArt = null;
        }
    }
}

// ========================= DATA CLASSES =========================
class Weapon {

    String Name;
    int Damage;

    Weapon(String n, int d) {
        Name = n;
        Damage = d;
    }

    Weapon() {
    }
}

class Character {

    int health = 100;
    int stamina = 100;
}

class Player extends Character {

    int X, Y;
    Weapon EquippedWeapon;
    List<Weapon> Inventory = new ArrayList<>();
}

class Enemy extends Character {

    String Name;
    Weapon Weapon;

    Enemy() {
    }

    Enemy(String n, int h) {
        Name = n;
        health = h;
    }
}

class Tile {

    int X, Y;
    String Name;
    boolean Walkable;
    char Symbol;
    boolean Explored;
    boolean isStart = false;
    Enemy Enemy;
    Weapon Item;
}
