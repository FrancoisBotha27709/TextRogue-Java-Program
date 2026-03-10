package com.mycompany.roguelike;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

public class RogueLike {

    // Helper class for Dictionary key (tuples in C#)
    static class Coord {
        int x, y;
        Coord(int x, int y) { this.x = x; this.y = y; }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Coord)) return false;
            Coord coord = (Coord) o;
            return x == coord.x && y == coord.y;
        }
        @Override
        public int hashCode() { return Objects.hash(x, y); }
    }

    static Map<Coord, Tile> world = new HashMap<>();

    static Player player;
    static int inventorySelectedIndex = 0;

    // Simulated Console Dimensions
    static final int CONSOLE_WIDTH = 120;
    static final int CONSOLE_HEIGHT = 40;

    static int VIEW_X() { return MAP_WIDTH() / 2; }   // half width in tiles
    static int VIEW_Y() { return MAP_HEIGHT() / 2; }  // half height in tiles

    static int MAP_WIDTH() { return (int)(CONSOLE_WIDTH * 0.7); }
    static int MAP_HEIGHT() { return (int)(CONSOLE_HEIGHT * 0.8); }
    // Adjusted sizes
    static int MAP_WIDTH_CONTENT() { return MAP_WIDTH() - 2; }
    static int MAP_HEIGHT_CONTENT() { return MAP_HEIGHT() - 2; }
    static int LOG_WIDTH_CONTENT() { return LOG_WIDTH() - 2; }
    static int LOG_HEIGHT_CONTENT() { return LOG_HEIGHT() - 2; }
    static int INPUT_WIDTH_CONTENT() { return INPUT_WIDTH() - 2; }
    static int INPUT_HEIGHT_CONTENT() { return INPUT_HEIGHT() - 2; }
    static int LOG_WIDTH() { return (int)(CONSOLE_WIDTH * 0.3); }
    static int LOG_HEIGHT() { return CONSOLE_HEIGHT; }

    static int INPUT_WIDTH() { return (int)(CONSOLE_WIDTH * 0.7); }
    static int INPUT_HEIGHT() { return (int)(CONSOLE_HEIGHT * 0.2); }

    static int LOG_X() { return MAP_WIDTH(); } // log starts after map
    static int INPUT_Y() { return MAP_HEIGHT(); } // input starts after map

    static List<String> log = new ArrayList<>();

    // Swing UI Components
    static JFrame frame;
    static GamePanel panel;
    static final Object inputLock = new Object();
    static KeyEvent lastKeyEvent = null;
    static boolean gameStarted = false;

    // Virtual Console Buffer
    static char[][] charBuffer = new char[CONSOLE_HEIGHT][CONSOLE_WIDTH];
    static Color[][] colorBuffer = new Color[CONSOLE_HEIGHT][CONSOLE_WIDTH];
    static Color currentForegroundColor = Color.WHITE;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Roguelike Translation");
            panel = new GamePanel();
            frame.add(panel);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            frame.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    synchronized (inputLock) {
                        lastKeyEvent = e;
                        inputLock.notifyAll();
                    }
                }
            });

            new Thread(() -> runGame()).start();
        });
    }

    static void runGame() {
        // Initial clean
        clearBuffers();
        
        setCursorPosition(0, 0);
        writeLine("Please make the window Fullscreen, press F to start...");
        panel.repaint();

        KeyEvent key;
        do {
            key = readKey();
        } while (key == null || key.getKeyCode() != KeyEvent.VK_F);

        gameStarted = true;
        GenerateStart();
        UpdateBorderIfNeeded();
        DrawUIContent();
        panel.repaint();

        while (true) {
            if (inventoryMode) DrawInventory();
            else DrawMap();
            UpdateBorderIfNeeded();  // only redraw if player entered new state
            DrawUIContent();          // map, log, input, inventory
            panel.repaint();
            Input();
        }
    }

    static void DrawUIContent() {
        if (inventoryMode) DrawInventory();
        else DrawMap();
        DrawLog();
        DrawInput();
    }

    public static List<Weapon> weapons = new ArrayList<>();
    public static List<Enemy> enemies = new ArrayList<>();
    public static List<String> names = new ArrayList<>();
    public static List<Integer> numbers = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
    public static List<Integer> healthAmounts = new ArrayList<>(Arrays.asList(10, 20, 30, 40, 50, 60, 70, 80, 90, 100));

    static void GenerateWeapons() {
        weapons = new ArrayList<Weapon>() {{
            add(new Weapon("Sword", RandomFrom(numbers)));
            add(new Weapon("Spear", RandomFrom(numbers)));
            add(new Weapon("Halberd", RandomFrom(numbers)));
            add(new Weapon("Stick", RandomFrom(numbers)));
            add(new Weapon("Knife", RandomFrom(numbers)));
            add(new Weapon("Rock", RandomFrom(numbers)));
            add(new Weapon("Scimitar", RandomFrom(numbers)));
            add(new Weapon("Scythe", RandomFrom(numbers)));
            add(new Weapon("Glaive", RandomFrom(numbers)));
            add(new Weapon("Kurru", RandomFrom(numbers)));
            add(new Weapon("Mallet", RandomFrom(numbers)));
            add(new Weapon("Mace", RandomFrom(numbers)));
        }};
    }

    static void GenerateEnemies() {
        enemies = new ArrayList<Enemy>() {{
            add(new Enemy("Goblin", RandomFrom(healthAmounts)));
            add(new Enemy("Orc", RandomFrom(healthAmounts)));
            add(new Enemy("Skeleton", RandomFrom(healthAmounts)));
            add(new Enemy("Bandit", RandomFrom(healthAmounts)));
            add(new Enemy("Zombie", RandomFrom(healthAmounts)));
        }};
    }

    static void GenerateNames() {
        names = new ArrayList<String>() {{
            add("Galley");
            add("River");
            add("Bridge");
            add("Gateway");
            add("Market");
            add("Garden");
            add("Well");
            add("House");
            add("Bar");
            add("Granary");
            add("Field");
            add("Forest");
            add("Hidden temple");
        }};
    }

    static void GenerateStart() {
        GenerateEnemies();
        GenerateWeapons();
        GenerateNames();
        Tile start = CreateArea(0, 0, true);

        player = new Player();
        player.X = 0;
        player.Y = 0;
        player.EquippedWeapon = new Weapon("Halberd", 10);

        start.Explored = true;

        Log("You wake up in a strange place.");
    }

    static char GetCorridorSymbol(int x, int y) {
        boolean up = IsWalkable(x, y - 1);
        boolean down = IsWalkable(x, y + 1);
        boolean left = IsWalkable(x - 1, y);
        boolean right = IsWalkable(x + 1, y);

        int count =
            (up ? 1 : 0) +
            (down ? 1 : 0) +
            (left ? 1 : 0) +
            (right ? 1 : 0);

        if (left && right && !up && !down) return '-';
        if (up && down && !left && !right) return '|';

        if (up && right && !left && !down) return '└';
        if (up && left && !right && !down) return '┘';
        if (down && right && !left && !up) return '┌';
        if (down && left && !right && !up) return '┐';

        if (count >= 3) return '+';

        return '.';
    }

    static boolean IsWalkable(int x, int y) {
        Tile t = world.get(new Coord(x, y));
        if (t == null)
            return false;

        return t.Walkable;
    }

    static void DrawInput() {
        setCursorPosition(0, INPUT_Y() + 1);
        
        // Status string parsing colors (manual reproduction of ANSI logic)
        String statusPrefix = "HP: ";
        write(statusPrefix, Color.WHITE);
        write(String.valueOf(player.health), Color.RED);
        write(" | STA: ", Color.WHITE);
        write(String.valueOf(player.stamina), Color.RED);
        write(" | Weapon: ", Color.WHITE);
        write(player.EquippedWeapon != null ? player.EquippedWeapon.Name : "None", Color.GREEN);
        write(" ( DMG: ", Color.WHITE);
        write(String.valueOf(player.EquippedWeapon != null ? player.EquippedWeapon.Damage : 0), Color.RED);
        write(") | POS: ", Color.WHITE);
        write(player.X + ", " + player.Y, Color.YELLOW);

        setCursorPosition(0, INPUT_Y() + 1);
        String inputHint = "  WASD move | E interact | Q inventory";
        writeLine(inputHint, Color.WHITE);
        // Padding/Placement simulation
        setCursorPosition(2, INPUT_Y() + 8);
        // Note: the original C# code used a combined string with many \n. 
        // We replicate by specific cursor positioning.
    }

    static boolean inventoryMode = false;

    static void Input() {
        KeyEvent k = readKey();
        if (k == null) return;

        if (inventoryMode) {
            switch (k.getKeyCode()) {
                case KeyEvent.VK_Q:
                    inventoryMode = false;
                    break;
                case KeyEvent.VK_W:
                    inventorySelectedIndex--;
                    if (inventorySelectedIndex < 0) inventorySelectedIndex = player.Inventory.size() - 1;
                    break;
                case KeyEvent.VK_S:
                    inventorySelectedIndex++;
                    if (inventorySelectedIndex >= player.Inventory.size()) inventorySelectedIndex = 0;
                    break;
                case KeyEvent.VK_E:
                    if (player.Inventory.size() > 0) {
                        // Swap equipped weapon
                        Weapon newWeapon = player.Inventory.get(inventorySelectedIndex);
                        Weapon oldWeapon = player.EquippedWeapon;

                        player.EquippedWeapon = newWeapon;
                        player.Inventory.set(inventorySelectedIndex, oldWeapon);

                        // if oldWeapon was null, remove it from inventory
                        if (oldWeapon == null) player.Inventory.remove(inventorySelectedIndex);

                        Log("Equipped " + newWeapon.Name);
                    }
                    break;
            }
            return;
        }

        switch (k.getKeyCode()) {
            case KeyEvent.VK_W: Move(0, -1); break;
            case KeyEvent.VK_S: Move(0, 1); break;
            case KeyEvent.VK_A: Move(-1, 0); break;
            case KeyEvent.VK_D: Move(1, 0); break;
            case KeyEvent.VK_E: Interact(); break;
            case KeyEvent.VK_Q: inventoryMode = true; break; // toggle inventory
        }
    }

    static void DrawMap() {
        int minX = player.X - VIEW_X();
        int maxX = player.X + VIEW_X();
        int minY = player.Y - VIEW_Y();
        int maxY = player.Y + VIEW_Y();

        for (int y = minY; y <= maxY && y - minY < MAP_HEIGHT_CONTENT(); y++) {
            for (int x = minX; x <= maxX && x - minX < MAP_WIDTH_CONTENT(); x++) {
                char c = ' ';
                Color color = Color.WHITE;

                if (player.X == x && player.Y == y) {
                    c = '#';
                    color = Color.YELLOW;
                } else {
                    Tile tile = world.get(new Coord(x, y));
                    if (tile != null) {
                        if (!tile.Explored) {
                            c = '.';
                            color = Color.DARK_GRAY;
                        } else if (tile.Enemy != null) {
                            c = 'E';
                            color = Color.RED;
                        } else if (tile.Item != null) {
                            c = 'i';
                            color = Color.GREEN;
                        } else if (tile.isStart) {
                            c = tile.Symbol;
                            color = new Color(128, 0, 128); // Purple
                        } else {
                            c = tile.Symbol == '-' ? GetCorridorSymbol(x, y) : tile.Symbol;
                            color = (tile.Symbol == '-') ? Color.DARK_GRAY : Color.WHITE;
                        }
                    }
                }

                setCursorPosition(1 + (x - minX), 1 + (y - minY));
                write(String.valueOf(c), color);
            }
        }
    }

    static void DrawLog() {
        int startIdx = Math.max(0, log.size() - LOG_HEIGHT_CONTENT());
        for (int i = 0; i < LOG_HEIGHT_CONTENT(); i++) {
            String line = (startIdx + i < log.size()) ? log.get(startIdx + i) : "";
            Color color = Color.CYAN;

            if (!line.isEmpty()) {
                if (line.startsWith("> You attack") || line.contains("dies") || line.contains("HP"))
                    color = Color.RED;
                else if (line.contains("pick up") || line.contains("Item"))
                    color = Color.GREEN;
            }

            if (line.length() > LOG_WIDTH_CONTENT()) line = line.substring(0, LOG_WIDTH_CONTENT());
            
            setCursorPosition(LOG_X() + 1, 1 + i);
            // Clear line first
            write(String.format("%-" + LOG_WIDTH_CONTENT() + "s", ""), Color.BLACK);
            setCursorPosition(LOG_X() + 1, 1 + i);
            write(line, color);
        }
    }

    static void DrawInventory() {
        // Clear the map area
        for (int y = 1; y <= MAP_HEIGHT_CONTENT(); y++) {
            setCursorPosition(1, y);
            write(String.format("%" + MAP_WIDTH_CONTENT() + "s", ""), Color.BLACK);
        }

        int yPos = 1; // start inside border

        setCursorPosition(1, yPos++);
        write("Inventory:", Color.CYAN);
        for (int i = 0; i < player.Inventory.size() && yPos <= MAP_HEIGHT_CONTENT(); i++) {
            Weapon w = player.Inventory.get(i);
            String marker = (i == inventorySelectedIndex ? "*" : " ");
            if (player.EquippedWeapon == w) marker = "!";

            String line = marker + " " + w.Name + " DMG:" + w.Damage;
            if (line.length() > MAP_WIDTH_CONTENT()) line = line.substring(0, MAP_WIDTH_CONTENT());

            setCursorPosition(1, yPos++);
            write(String.format("%-" + MAP_WIDTH_CONTENT() + "s", line), Color.CYAN);
        }
    }

    static Color currentBorderColor = new Color(205, 127, 50); // bronze default

    static void UpdateBorderIfNeeded() {
        Color newColor = new Color(205, 127, 50); // bronze
        Tile tile = world.get(new Coord(player.X, player.Y));
        if (tile != null) {
            if (tile.Enemy != null) newColor = Color.RED;
            else if (tile.Item != null) newColor = Color.GREEN;
            else if (tile.isStart) newColor = new Color(128, 0, 128); // purple
        }

        if (!newColor.equals(currentBorderColor)) {
            currentBorderColor = newColor;
            DrawBorder(0, 0, MAP_WIDTH(), MAP_HEIGHT(), currentBorderColor);
            DrawBorder(LOG_X(), 0, LOG_WIDTH(), LOG_HEIGHT(), currentBorderColor);
            DrawBorder(0, INPUT_Y(), INPUT_WIDTH(), INPUT_HEIGHT(), currentBorderColor);
        }
    }

    static void DrawBorder(int x, int y, int width, int height, Color color) {
        // Top and bottom
        for (int i = 0; i < width; i++) {
            setCursorPosition(x + i, y);
            write("─", color);
            setCursorPosition(x + i, y + height - 1);
            write("─", color);
        }

        // Left and right
        for (int i = 0; i < height; i++) {
            setCursorPosition(x, y + i);
            write("│", color);
            setCursorPosition(x + width - 1, y + i);
            write("│", color);
        }

        // Corners
        setCursorPosition(x, y); write("┌", color);
        setCursorPosition(x + width - 1, y); write("┐", color);
        setCursorPosition(x, y + height - 1); write("└", color);
        setCursorPosition(x + width - 1, y + height - 1); write("┘", color);
    }

    static void Move(int dx, int dy) {
        int nx = player.X + dx;
        int ny = player.Y + dy;

        Tile tile = GetOrCreate(nx, ny);

        if (!tile.Walkable) {
            Log("You cannot go there.");
            return;
        }

        player.X = nx;
        player.Y = ny;

        tile.Explored = true;

        DescribeTile(tile);
    }

    static void Interact() {
        Tile tile = world.get(new Coord(player.X, player.Y));
        if (tile == null) return;

        boolean changed = false;
        if (tile.Enemy != null) {
            Enemy enemy = tile.Enemy;
            int pdmg = player.EquippedWeapon != null ? player.EquippedWeapon.Damage : 5;
            Log("You attack " + enemy.Name + " for " + pdmg + " damage.");
            enemy.health -= pdmg;

            if (enemy.health <= 0) {
                Log(enemy.Name + " dies.");
                if (RandomChance(0.5) && enemy.Weapon != null) {
                    tile.Item = enemy.Weapon;
                    Log(enemy.Name + " dropped " + enemy.Weapon.Name);
                }
                tile.Enemy = null;
            } else {
                int edmg = enemy.Weapon != null ? enemy.Weapon.Damage : 2;
                Log(enemy.Name + " hits you for " + edmg + " damage! (HP: " + player.health + ")");
                player.health -= edmg;
                if (player.health <= 0) {
                    PlayerDied();
                }
            }
            changed = true;
        }

        if (tile.Item != null) {
            Log("You pick up " + tile.Item.Name + " ( DMG: " + tile.Item.Damage + ")");
            player.Inventory.add(tile.Item);
            tile.Item = null;
            changed = true;
        }

        if (!changed)
            Log("Nothing to interact with.");

        DescribeTile(tile);
    }

    static void PlayerDied() {
        FadeOutGame();

        FadeDeathText(true);  // fade in
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        FadeDeathText(false); // fade out

        world.clear();
        log.clear();
        GenerateStart();

        FadeInGame();
    }

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

    static void FadeDeathText(boolean fadeOut) {
        String[] deathArt = GetDeathArt();
        int steps = 5;
        
        for (int step = 0; step <= steps; step++) {
            clearBuffers();
            int currentStep = fadeOut ? (steps - step) : step;
            Color fadeColor = GetFadeColor(currentStep, steps, Color.RED);

            int startY = (CONSOLE_HEIGHT / 2) - (deathArt.length / 2);
            for (String line : deathArt) {
                int startX = (CONSOLE_WIDTH / 2) - (line.length() / 2);
                setCursorPosition(Math.max(0, startX), startY++);
                write(line, fadeColor);
            }

            panel.repaint();
            try { Thread.sleep(300); } catch (InterruptedException e) {}
        }
    }

    static void FadeOutGame() {
        Color[] fadeColors = new Color[] {
            Color.WHITE,
            Color.GRAY,
            Color.DARK_GRAY,
            Color.BLACK
        };

        DrawUIContent();

        for (Color c : fadeColors) {
            overrideColors(c);
            panel.repaint();
            try { Thread.sleep(300); } catch (InterruptedException e) {}
        }
    }

    static void FadeInGame() {
        Color[] fadeColors = new Color[] {
            Color.BLACK,
            Color.DARK_GRAY,
            Color.GRAY,
            Color.WHITE
        };

        DrawUIContent();

        for (Color c : fadeColors) {
            overrideColors(c);
            panel.repaint();
            try { Thread.sleep(300); } catch (InterruptedException e) {}
        }
    }

    static Color GetFadeColor(int step, int maxStep, Color baseColor) {
        if (step >= 4) return Color.BLACK;
        if (step == 3) return Color.DARK_GRAY;
        if (step == 2) return Color.GRAY;
        if (step == 1) return Color.LIGHT_GRAY;
        return baseColor;
    }

    static Tile GetOrCreate(int x, int y) {
        Tile t = world.get(new Coord(x, y));
        if (t != null) return t;

        if (RandomChance(0.3)) 
            return CreateArea(x, y, false);
        else
            return CreatePath(x, y);
    }

    static Tile CreateArea(int x, int y, boolean isStart) {
        String newName = RandomFrom(names);
        char newSymbol = newName.charAt(0);
        Tile tile = new Tile();
        tile.X = x;
        tile.Y = y;
        tile.isStart = isStart;
        tile.Name = newName;
        tile.Walkable = true;
        tile.Symbol = newSymbol;

        if (RandomChance(0.3))
            tile.Item = RandomFrom(weapons);

        if (RandomChance(0.3)) {
            Enemy enemyTemplate = RandomFrom(enemies);
            Enemy enemy = new Enemy();
            enemy.Name = enemyTemplate.Name;
            enemy.health = enemyTemplate.health;
            enemy.stamina = enemyTemplate.stamina;
            enemy.Weapon = RandomFrom(weapons);
            tile.Enemy = enemy;
        }

        world.put(new Coord(x, y), tile);
        return tile;
    }

    static Tile CreatePath(int x, int y) {
        Tile tile = new Tile();
        tile.X = x;
        tile.Y = y;
        tile.Walkable = true;
        tile.Symbol = '-';

        world.put(new Coord(x, y), tile);
        return tile;
    }

    static void DescribeTile(Tile tile) {
        if (tile.Symbol == '-')
            Log("You are in a corridor.");
        else
            Log("You are in " + tile.Name + ".");

        if (tile.Enemy != null)
            Log("A " + tile.Enemy.Name + " is here. HP: " + tile.Enemy.health);

        if (tile.Item != null)
            Log("There is a " + tile.Item.Name + " here.");
    }

    static void Log(String s) {
        log.add("> " + s);
        if (log.size() > 200)
            log.remove(0);
    }

    static Random rng = new Random();

    static boolean RandomChance(double p) {
        return rng.nextDouble() < p;
    }

    static <T> T RandomFrom(List<T> list) {
        if (list.isEmpty()) return null;
        int index = rng.nextInt(list.size());
        return list.get(index);
    }

    // --- Virtual Console Methods ---
    static int cursorX = 0, cursorY = 0;
    static void setCursorPosition(int x, int y) {
        cursorX = Math.max(0, Math.min(x, CONSOLE_WIDTH - 1));
        cursorY = Math.max(0, Math.min(y, CONSOLE_HEIGHT - 1));
    }

    static void write(String s, Color color) {
        for (char c : s.toCharArray()) {
            if (cursorX < CONSOLE_WIDTH && cursorY < CONSOLE_HEIGHT) {
                charBuffer[cursorY][cursorX] = c;
                colorBuffer[cursorY][cursorX] = color;
                cursorX++;
            }
        }
    }

    static void writeLine(String s) {
        write(s, Color.WHITE);
        cursorX = 0;
        cursorY++;
    }

    static void writeLine(String s, Color color) {
        write(s, color);
        cursorX = 0;
        cursorY++;
    }

    static void clearBuffers() {
        for (int y = 0; y < CONSOLE_HEIGHT; y++) {
            for (int x = 0; x < CONSOLE_WIDTH; x++) {
                charBuffer[y][x] = ' ';
                colorBuffer[y][x] = Color.BLACK;
            }
        }
    }

    static void overrideColors(Color color) {
        for (int y = 0; y < CONSOLE_HEIGHT; y++) {
            for (int x = 0; x < CONSOLE_WIDTH; x++) {
                if (charBuffer[y][x] != ' ') {
                    colorBuffer[y][x] = color;
                }
            }
        }
    }

    static KeyEvent readKey() {
        synchronized (inputLock) {
            while (lastKeyEvent == null) {
                try { inputLock.wait(); } catch (InterruptedException e) {}
            }
            KeyEvent k = lastKeyEvent;
            lastKeyEvent = null;
            return k;
        }
    }

    static class GamePanel extends JPanel {
        GamePanel() {
            setPreferredSize(new Dimension(CONSOLE_WIDTH * 10, CONSOLE_HEIGHT * 18));
            setBackground(Color.BLACK);
            setFont(new Font("Monospaced", Font.PLAIN, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            int charW = fm.charWidth('A');
            int charH = fm.getHeight();

            for (int y = 0; y < CONSOLE_HEIGHT; y++) {
                for (int x = 0; x < CONSOLE_WIDTH; x++) {
                    char c = charBuffer[y][x];
                    if (c != ' ' && c != 0) {
                        g.setColor(colorBuffer[y][x]);
                        g.drawString(String.valueOf(c), x * charW, (y + 1) * charH - 5);
                    }
                }
            }
        }
    }
}

class Weapon {
    public String Name;
    public int Damage;
    public Weapon(String name, int damage) { this.Name = name; this.Damage = damage; }
}

class Character {
    public int health = 100;
    public int stamina = 100;
}

class Player extends Character {
    public int X, Y;
    public Weapon EquippedWeapon;
    public List<Weapon> Inventory = new ArrayList<>();
}

class Enemy extends Character {
    public String Name;
    public Weapon Weapon;
    public Enemy() {}
    public Enemy(String name, int health) { this.Name = name; this.health = health; }
}

class Tile {
    public int X, Y;
    public String Name;
    public boolean Walkable;
    public char Symbol;
    public boolean Explored = false;
    public boolean isStart = false;
    public Weapon Item;
    public Enemy Enemy;
}
