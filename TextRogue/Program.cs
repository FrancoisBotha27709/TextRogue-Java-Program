using System.Security.Cryptography;
using System.Text;

class Program
{

    static Dictionary<(int x, int y), Tile> world = new();

    static Player player;
    static int inventorySelectedIndex = 0;

    static int VIEW_X => MAP_WIDTH / 2;   // half width in tiles
    static int VIEW_Y => MAP_HEIGHT / 2;  // half height in tiles

    static int MAP_WIDTH => (int)(Console.WindowWidth * 0.7);
    static int MAP_HEIGHT => (int)(Console.WindowHeight * 0.8);
    // Adjusted sizes
    static int MAP_WIDTH_CONTENT => MAP_WIDTH - 2;
    static int MAP_HEIGHT_CONTENT => MAP_HEIGHT - 2;
    static int LOG_WIDTH_CONTENT => LOG_WIDTH - 2;
    static int LOG_HEIGHT_CONTENT => LOG_HEIGHT - 2;
    static int INPUT_WIDTH_CONTENT => INPUT_WIDTH - 2;
    static int INPUT_HEIGHT_CONTENT => INPUT_HEIGHT - 2;
    static int LOG_WIDTH => (int)(Console.WindowWidth * 0.3);
    static int LOG_HEIGHT => Console.WindowHeight;

    static int INPUT_WIDTH => (int)(Console.WindowWidth * 0.7);
    static int INPUT_HEIGHT => (int)(Console.WindowHeight * 0.2);

    static int LOG_X => MAP_WIDTH; // log starts after map
    static int INPUT_Y => MAP_HEIGHT; // input starts after map

    static List<string> log = new();

    static void Main()
    {
        Console.CursorVisible = false;

        Console.WriteLine("Please make the window Fullscreen, press F to start...");
        ConsoleKey key;
        do
        {
            key = Console.ReadKey(true).Key;
        } while (key != ConsoleKey.F);

        GenerateStart();
        UpdateBorderIfNeeded();
        DrawUIContent();

        while (true)
        {
            if (inventoryMode) DrawInventory();
            else DrawMap();
            UpdateBorderIfNeeded();  // only redraw if player entered new state
            DrawUIContent();          // map, log, input, inventory
            Input();
        }

    }

    static void DrawUIContent()
    {
        if (inventoryMode) DrawInventory();
        else DrawMap();
        DrawLog();
        DrawInput();
    }

    public static List<Weapon> weapons = new List<Weapon>();
    public static List<Enemy> enemies = new List<Enemy>();
    public static List<string> names = new List<string>();
    public static List<int> numbers = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20];
    public static List<int> healthAmounts = [10, 20, 30, 40, 50, 60, 70, 80, 90, 100];
    static void GenerateWeapons()
    {
        weapons = new List<Weapon>()
        {
            new Weapon() { Name = "Sword", Damage = RandomFrom(numbers) },
            new Weapon() { Name = "Spear", Damage = RandomFrom(numbers) },
            new Weapon() { Name = "Halberd", Damage = RandomFrom(numbers) },
            new Weapon() { Name = "Stick", Damage = RandomFrom(numbers) },
            new Weapon() { Name = "Knife", Damage = RandomFrom(numbers) },
            new Weapon() { Name = "Rock", Damage = RandomFrom(numbers) },
            new Weapon() { Name = "Scimitar", Damage = RandomFrom(numbers) },
            new Weapon() { Name = "Scythe", Damage = RandomFrom(numbers) },
            new Weapon() { Name = "Glaive", Damage = RandomFrom(numbers) },
            new Weapon() { Name = "Kurru", Damage = RandomFrom(numbers) },
            new Weapon() { Name = "Mallet", Damage = RandomFrom(numbers) },
            new Weapon() { Name = "Mace", Damage = RandomFrom(numbers) },
        };
    }

    static void GenerateEnemies()
    {
        enemies = new List<Enemy>()
        {
            new Enemy { Name = "Goblin", health = RandomFrom(healthAmounts)},
            new Enemy { Name = "Orc", health = RandomFrom(healthAmounts) },
            new Enemy { Name = "Skeleton", health = RandomFrom(healthAmounts) },
            new Enemy { Name = "Bandit", health = RandomFrom(healthAmounts) },
            new Enemy { Name = "Zombie", health = RandomFrom(healthAmounts) }
        };
    }

    static void GenerateNames()
    {
        names = new List<string>()
        {
            "Galley",
            "River",
            "Bridge",
            "Gateway",
            "Market",
            "Garden",
            "Well",
            "House",
            "Bar",
            "Granary",
            "Field",
            "Forest",
            "Hidden temple",
        };
    }

    static void GenerateStart()
    {
        GenerateEnemies();
        GenerateWeapons();
        GenerateNames();
        var start = CreateArea(0, 0, true);

        player = new Player
        {
            X = 0,
            Y = 0,
            EquippedWeapon = new Weapon() { Name = "Halberd", Damage = 10 }
        };

        start.Explored = true;

        Log("You wake up in a strange place.");
    }

    static char GetCorridorSymbol(int x, int y)
    {
        bool up = IsWalkable(x, y - 1);
        bool down = IsWalkable(x, y + 1);
        bool left = IsWalkable(x - 1, y);
        bool right = IsWalkable(x + 1, y);

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

    static bool IsWalkable(int x, int y)
    {
        if (!world.TryGetValue((x, y), out var t))
            return false;

        return t.Walkable;
    }

    static void DrawInput()
    {
        Console.SetCursorPosition(0, INPUT_Y + 1);
        string status = $"HP: \u001b[31m{player.health}\u001b[37m | STA: \u001b[31m{player.stamina}\u001b[37m | Weapon: \u001b[32m{player.EquippedWeapon?.Name ?? "None"}\u001b[37m ( DMG: \u001b[31m{player.EquippedWeapon?.Damage}\u001b[37m) | POS: \u001b[33m{player.X}\u001b[37m, \u001b[33m{player.Y}\u001b[37m";
        string inputHint = "  WASD move | E interact | Q inventory";

        // Combine and truncate if needed
        string fullLine = inputHint + "\n\n\n\n\n\n\n  " + status;
        if (fullLine.Length > INPUT_WIDTH_CONTENT)
            fullLine = fullLine.Substring(0, INPUT_WIDTH_CONTENT);

        Console.WriteLine(fullLine.PadRight(INPUT_WIDTH_CONTENT));
    }

    static bool inventoryMode = false;

    static void Input()
    {
        var k = Console.ReadKey(true);

        if (inventoryMode)
        {
            switch (k.Key)
            {
                case ConsoleKey.Q:
                    inventoryMode = false;
                    break;
                case ConsoleKey.W:
                    inventorySelectedIndex--;
                    if (inventorySelectedIndex < 0) inventorySelectedIndex = player.Inventory.Count - 1;
                    break;
                case ConsoleKey.S:
                    inventorySelectedIndex++;
                    if (inventorySelectedIndex >= player.Inventory.Count) inventorySelectedIndex = 0;
                    break;
                case ConsoleKey.E:
                    if (player.Inventory.Count > 0)
                    {
                        // Swap equipped weapon
                        var newWeapon = player.Inventory[inventorySelectedIndex];
                        var oldWeapon = player.EquippedWeapon;

                        player.EquippedWeapon = newWeapon;
                        player.Inventory[inventorySelectedIndex] = oldWeapon;

                        // if oldWeapon was null, remove it from inventory
                        if (oldWeapon == null) player.Inventory.RemoveAt(inventorySelectedIndex);

                        Log($"Equipped {newWeapon.Name}");
                    }
                    break;
            }

            return;
        }

        switch (k.Key)
        {
            case ConsoleKey.W: Move(0, -1); break;
            case ConsoleKey.S: Move(0, 1); break;
            case ConsoleKey.A: Move(-1, 0); break;
            case ConsoleKey.D: Move(1, 0); break;
            case ConsoleKey.E: Interact(); break;
            case ConsoleKey.Q: inventoryMode = true; break; // toggle inventory
        }
    }

    static void DrawMap()
    {
        int minX = player.X - VIEW_X;
        int maxX = player.X + VIEW_X;
        int minY = player.Y - VIEW_Y;
        int maxY = player.Y + VIEW_Y;

        StringBuilder sb = new StringBuilder();
        string prevAnsi = "";

        for (int y = minY; y <= maxY && y - minY < MAP_HEIGHT_CONTENT; y++)
        {
            sb.Clear();

            for (int x = minX; x <= maxX && x - minX < MAP_WIDTH_CONTENT; x++)
            {
                char c = ' ';
                string ansiColor = "\u001b[37m"; // default gray

                if (player.X == x && player.Y == y)
                {
                    c = '#';
                    ansiColor = "\u001b[33m"; // yellow
                }
                else if (world.TryGetValue((x, y), out var tile))
                {
                    if (!tile.Explored)
                    {
                        c = '.';
                        ansiColor = "\u001b[90m"; // dark gray
                    }
                    else if (tile.Enemy != null)
                    {
                        c = 'E';
                        ansiColor = "\u001b[31m"; // red
                    }
                    else if (tile.Item != null)
                    {
                        c = 'i';
                        ansiColor = "\u001b[32m"; // green
                    }
                    else if (tile.isStart == true)
                        ansiColor = "\u001b[35m"; //purple
                    else
                    {
                        c = tile.Symbol == '-' ? GetCorridorSymbol(x, y) : tile.Symbol;
                        ansiColor = (tile.Symbol == '-') ? "\u001b[90m" : "\u001b[37m";
                    }
                }

                // only write ANSI code if it changed
                if (ansiColor != prevAnsi)
                {
                    sb.Append(ansiColor);
                    prevAnsi = ansiColor;
                }

                sb.Append(c);
            }

            Console.SetCursorPosition(1, 1 + (y - minY));
            Console.Write(sb.ToString());
            prevAnsi = ""; // reset per row
        }

        // reset to default color
        Console.Write("\u001b[0m");
    }

    static void DrawLog()
    {
        int start = Math.Max(0, log.Count - LOG_HEIGHT_CONTENT);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < LOG_HEIGHT_CONTENT; i++)
        {
            sb.Clear();
            string line = (start + i < log.Count) ? log[start + i] : "";
            string ansiColor = "\u001b[36m"; // default cyan

            if (!string.IsNullOrEmpty(line))
            {
                if (line.StartsWith("> You attack") || line.Contains("dies") || line.Contains("HP"))
                    ansiColor = "\u001b[31m"; // red
                else if (line.Contains("pick up") || line.Contains("Item"))
                    ansiColor = "\u001b[32m"; // green
            }

            if (line.Length > LOG_WIDTH_CONTENT) line = line.Substring(0, LOG_WIDTH_CONTENT);
            sb.Append(ansiColor);
            sb.Append(line.PadRight(LOG_WIDTH_CONTENT));
            sb.Append("\u001b[0m"); // reset color

            Console.SetCursorPosition(LOG_X + 1, 1 + i);
            Console.Write(sb.ToString());
        }
    }

    static void DrawInventory()
    {
        // Clear the map area
        for (int y = 1; y <= MAP_HEIGHT_CONTENT; y++)
        {
            Console.SetCursorPosition(1, y);
            Console.Write(new string(' ', MAP_WIDTH_CONTENT));
        }

        int yPos = 1; // start inside border

        Console.SetCursorPosition(1, yPos++);
        Console.Write("\u001b[36mInventory:\u001b[0m"); // cyan title
        for (int i = 0; i < player.Inventory.Count && yPos <= MAP_HEIGHT_CONTENT; i++)
        {
            var w = player.Inventory[i];
            string marker = (i == inventorySelectedIndex ? "*" : " ");
            if (player.EquippedWeapon == w) marker = "!";

            string line = $"{marker} {w.Name} DMG:{w.Damage}";
            if (line.Length > MAP_WIDTH_CONTENT) line = line.Substring(0, MAP_WIDTH_CONTENT);

            Console.SetCursorPosition(1, yPos++);
            Console.Write("\u001b[36m" + line.PadRight(MAP_WIDTH_CONTENT) + "\u001b[0m");
        }
    }

    // store the last border color
    static string currentBorderColor = "\u001b[33m"; // bronze default

    static void UpdateBorderIfNeeded()
    {
        string newColor = "\u001b[33m"; // bronze
        if (world.TryGetValue((player.X, player.Y), out var tile))
        {
            if (tile.Enemy != null) newColor = "\u001b[31m"; // red
            else if (tile.Item != null) newColor = "\u001b[32m"; // green
            else if (tile.isStart == true) newColor = "\u001b[35m"; // purple
        }

        if (newColor != currentBorderColor)
        {
            currentBorderColor = newColor;
            DrawBorder(0, 0, MAP_WIDTH, MAP_HEIGHT, currentBorderColor);
            DrawBorder(LOG_X, 0, LOG_WIDTH, LOG_HEIGHT, currentBorderColor);
            DrawBorder(0, INPUT_Y, INPUT_WIDTH, INPUT_HEIGHT, currentBorderColor);
        }
    }

    // modify DrawBorder to accept color
    static void DrawBorder(int x, int y, int width, int height, string color)
    {
        // Top and bottom
        for (int i = 0; i < width; i++)
        {
            Console.SetCursorPosition(x + i, y);
            Console.Write(color + '─' + "\u001b[0m");
            Console.SetCursorPosition(x + i, y + height - 1);
            Console.Write(color + '─' + "\u001b[0m");
        }

        // Left and right
        for (int i = 0; i < height; i++)
        {
            Console.SetCursorPosition(x, y + i);
            Console.Write(color + '│' + "\u001b[0m");
            Console.SetCursorPosition(x + width - 1, y + i);
            Console.Write(color + '│' + "\u001b[0m");
        }

        // Corners
        Console.SetCursorPosition(x, y); Console.Write(color + '┌' + "\u001b[0m");
        Console.SetCursorPosition(x + width - 1, y); Console.Write(color + '┐' + "\u001b[0m");
        Console.SetCursorPosition(x, y + height - 1); Console.Write(color + '└' + "\u001b[0m");
        Console.SetCursorPosition(x + width - 1, y + height - 1); Console.Write(color + '┘' + "\u001b[0m");
    }

    static void Move(int dx, int dy)
    {
        int nx = player.X + dx;
        int ny = player.Y + dy;

        var tile = GetOrCreate(nx, ny);

        if (!tile.Walkable)
        {
            Log("You cannot go there.");
            return;
        }

        player.X = nx;
        player.Y = ny;

        tile.Explored = true;

        DescribeTile(tile);
    }

    static void Interact()
    {
        if (!world.TryGetValue((player.X, player.Y), out var tile))
            return;

        bool changed = false;
        if (tile.Enemy != null)
        {
            var enemy = tile.Enemy;
            int pdmg = player.EquippedWeapon?.Damage ?? 5;
            Log($"You attack {enemy.Name} for {pdmg} damage.");
            enemy.health -= pdmg;

            if (enemy.health <= 0)
            {
                Log($"{enemy.Name} dies.");
                if (RandomChance(0.5) && enemy.Weapon != null)
                {
                    tile.Item = enemy.Weapon;
                    Log($"{enemy.Name} dropped {enemy.Weapon.Name}");
                }
                tile.Enemy = null;
            }
            else
            {
                int edmg = enemy.Weapon?.Damage ?? 2;
                Log($"{enemy.Name} hits you for {edmg} damage! (HP: {player.health})");
                player.health -= edmg;
                if (player.health <= 0)
                {
                    PlayerDied();
                }
            }
        }

        if (tile.Item != null)
        {
            Log($"You pick up {tile.Item.Name} ( DMG: {tile.Item.Damage})");
            player.Inventory.Add(tile.Item);
            tile.Item = null;
            changed = true;
        }

        if (!changed)
            Log("Nothing to interact with.");

        // Update description after any change
        DescribeTile(tile);
    }

    static void PlayerDied()
    {
        // 1. Fade out current game
        FadeOutGame();

        FadeDeathText(false);  // fade in
        Thread.Sleep(1000);
        FadeDeathText(true); // fade out

        // 4. Reset game state
        world.Clear();
        log.Clear();
        GenerateStart();

        // 5. Fade game back in
        FadeInGame();
    }

    static string[] GetDeathArt()
    {
        string[] deathArt = new string[]{
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
        return deathArt;
    }

    static void FadeDeathText(bool fadeIn)
    {
        string[] deathArt = GetDeathArt(); // your existing array

        int steps = 5;
        int start = fadeIn ? 0 : steps;
        int end = fadeIn ? steps : 0;
        int increment = fadeIn ? 1 : -1;

        for (int step = start; fadeIn ? step <= end : step >= end; step += increment)
        {
            Console.Clear();
            Console.ForegroundColor = GetFadeColor(step, steps, ConsoleColor.Red);

            int startY = (Console.WindowHeight / 2) - (deathArt.Length / 2);
            foreach (var line in deathArt)
            {
                int startX = (Console.WindowWidth / 2) - (line.Length / 2);
                Console.SetCursorPosition(Math.Max(0, startX), startY++);
                Console.WriteLine(line);
            }

            Thread.Sleep(300);
        }

        Console.ResetColor();
    }

    static void FadeOutGame()
    {
        var fadeColors = new ConsoleColor[]
        {
        ConsoleColor.White,
        ConsoleColor.Gray,
        ConsoleColor.DarkGray,
        ConsoleColor.Black
        };

        DrawUIContent();

        for (int i = 0; i < fadeColors.Length; i++)
        {
            Console.ForegroundColor = fadeColors[i];
            DrawMap();
            DrawLog();
            DrawInput();
            Thread.Sleep(300);
        }

        Console.ResetColor();
    }

    static void FadeInGame()
    {
        var fadeColors = new ConsoleColor[]
        {
        ConsoleColor.Black,
        ConsoleColor.DarkGray,
        ConsoleColor.Gray,
        ConsoleColor.White
        };

        // Draw borders once, outside the fade
        DrawUIContent();

        for (int i = 0; i < fadeColors.Length; i++)
        {
            Console.ForegroundColor = fadeColors[i];
            DrawMap();
            DrawLog();
            DrawInput();
            Thread.Sleep(300);
        }

        Console.ResetColor();
    }

    static ConsoleColor GetFadeColor(int step, int maxStep, ConsoleColor baseColor = ConsoleColor.White)
    {
        // Approximate a fade: darker shades for higher step
        if (step == 0) return baseColor;
        if (step == 1) return ConsoleColor.Gray;
        if (step == 2) return ConsoleColor.DarkGray;
        if (step >= 3) return ConsoleColor.Black;
        return baseColor;
    }

    static Tile GetOrCreate(int x, int y)
    {
        if (world.TryGetValue((x, y), out var t))
            return t;

        // randomly decide if this is a room or corridor
        if (RandomChance(0.3)) // 30% chance to create a room
            return CreateArea(x, y);
        else
            return CreatePath(x, y);
    }

    static Tile CreateArea(int x, int y, bool isStart = false)
    {
        var newName = RandomFrom(names);
        char newSymbol = newName[0];
        var tile = new Tile
        {
            X = x,
            Y = y,
            isStart = isStart,
            Name = newName,
            Walkable = true,
            Symbol = newSymbol,
        };

        if (RandomChance(0.3))
            tile.Item = RandomFrom(weapons);

        if (RandomChance(0.3))
        {
            var enemyTemplate = RandomFrom(enemies);
            tile.Enemy = new Enemy
            {
                Name = enemyTemplate.Name,
                health = enemyTemplate.health,
                stamina = enemyTemplate.stamina,
                Weapon = RandomFrom(weapons)
            };
        }

        world[(x, y)] = tile;

        return tile;
    }

    static Tile CreatePath(int x, int y)
    {
        var tile = new Tile
        {
            X = x,
            Y = y,
            Walkable = true,
            Symbol = '-'
        };

        world[(x, y)] = tile;
        return tile;
    }

    static void DescribeTile(Tile tile)
    {
        if (tile.Symbol == '-')
            Log("You are in a corridor.");
        else
            Log($"You are in {tile.Name}.");

        if (tile.Enemy != null)
            Log($"A {tile.Enemy.Name} is here. HP: {tile.Enemy.health}");

        if (tile.Item != null)
            Log($"There is a {tile.Item} here.");
    }

    static void Log(string s)
    {
        log.Add("> " + s);

        if (log.Count > 200)
            log.RemoveAt(0);
    }
    static Random rng = new Random();

    static bool RandomChance(double p)
    {
        return rng.NextDouble() < p;
    }

    static T RandomFrom<T>(List<T> list)
    {
        if (list.Count == 0) return default;
        int index = rng.Next(list.Count);
        return list[index];
    }
}

class Weapon
{
    public string Name;
    public int Damage;
}

class Character
{
    public int health = 100;
    public int stamina = 100;
}

class Player : Character
{
    public int X, Y;
    public Weapon EquippedWeapon;
    public List<Weapon> Inventory = new();
}

class Enemy : Character
{
    public string Name;
    public Weapon Weapon;
}

class Tile
{
    public int X;
    public int Y;

    public string Name;

    public bool Walkable;

    public char Symbol;

    public bool Explored;
    public bool isStart = false;
    public Enemy Enemy;

    public Weapon Item;
}