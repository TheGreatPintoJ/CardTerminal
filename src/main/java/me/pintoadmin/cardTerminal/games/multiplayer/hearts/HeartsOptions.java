package me.pintoadmin.cardTerminal.games.multiplayer.hearts;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;
import java.io.*;
import java.util.*;
import java.util.function.*;
import me.pintoadmin.cardTerminal.*;
import me.pintoadmin.cardTerminal.games.spider.*;
import me.pintoadmin.cardTerminal.screens.*;

public class HeartsOptions extends GameOptions {
    private final Screen screen = Main.screen;
    private final TextGraphics g = Main.g;

    private long suggestedSeed = new Random().nextLong();
    private String SEED = "";
    private int endingPoints = 100;
    private String host = "";
    private int port = 51337;

    private final Supplier<Integer> W = () -> g.getSize().getColumns();
    private final Supplier<Integer> H = () -> g.getSize().getRows();

    private Option selectedOption = Option.SEED;

    private final LinkedHashMap<String, String> keybinds =
            new LinkedHashMap<>() {
                {
                    put("Move cursor", "◀ ▲ ▼ ▶");
                    put("Select/Deselect Card (play card)", "Space");
                    put("Pass selected cards", "Enter");
                }
            };

    private enum Option {
        SEED,
        ENDPOINTS,
        PLAY_HOST,
        HOST,
        PORT,
        PLAY_JOIN
    }

    public HeartsOptions(String name) {
        super(name);
    }

    @Override
    public void setSeed(long seed) {
        SEED = String.valueOf(seed);
    }

    private void build() throws IOException {
        screen.clear();
        Arrays.stream(SGR.values()).toList().forEach(g::disableModifiers);
        g.enableModifiers(SGR.BOLD);

        int W = this.W.get(), H = this.H.get();

        Main.drawRectangle(0, 0, W - 1, H - 1);
        Main.drawText(W / 2, 0, " " + getName() + " ");

        Main.drawRectangle(1, 1, W / 2 - 1, 8);

        if (selectedOption == Option.SEED) g.enableModifiers(SGR.REVERSE);
        Main.drawRectangle(8, 2, W / 2 - 2, 4);
        g.disableModifiers(SGR.REVERSE);
        Main.drawTextLeft(2, 3, "SEED:");
        if (SEED.isBlank()) g.setForegroundColor(new TextColor.RGB(100, 100, 100));
        Main.drawTextLeft(9, 3, String.valueOf(SEED.isBlank() ? suggestedSeed : SEED));
        if (SEED.isBlank()) g.setForegroundColor(TextColor.ANSI.WHITE);

        if (selectedOption == Option.ENDPOINTS) g.enableModifiers(SGR.REVERSE);
        Main.drawRectangle(17, 5, W / 2 - 2, 7);
        g.disableModifiers(SGR.REVERSE);
        Main.drawTextLeft(2, 6, "ENDING POINTS:");
        g.setCharacter(18, 6, '<');
        Main.drawText(Math.abs((17 + W) / 4), 6, String.valueOf(endingPoints));
        g.setCharacter(W / 2 - 3, 6, '>');

        if (selectedOption == Option.PLAY_HOST) g.enableModifiers(SGR.REVERSE);
        Main.drawText(W / 4, 8, " Host ");
        g.disableModifiers(SGR.REVERSE);

        Main.drawRectangle(1, 9, W / 2 - 1, 16);

        if (selectedOption == HeartsOptions.Option.HOST) g.enableModifiers(SGR.REVERSE);
        Main.drawRectangle(8, 10, W / 2 - 2, 12);
        g.disableModifiers(SGR.REVERSE);
        Main.drawTextLeft(2, 11, "HOST:");
        Main.drawTextLeft(9, 11, host);

        if (selectedOption == HeartsOptions.Option.PORT) g.enableModifiers(SGR.REVERSE);
        Main.drawRectangle(17, 13, W / 2 - 2, 15);
        g.disableModifiers(SGR.REVERSE);
        Main.drawTextLeft(2, 14, "PORT:");
        g.setCharacter(18, 14, '<');
        Main.drawText(Math.abs((17 + W) / 4), 14, String.valueOf(port));
        g.setCharacter(W / 2 - 3, 14, '>');

        if (selectedOption == HeartsOptions.Option.PLAY_JOIN) g.enableModifiers(SGR.REVERSE);
        Main.drawText(W / 4, 16, " Join ");
        g.disableModifiers(SGR.REVERSE);

        {
            int startY = 2;
            for (Map.Entry<String, String> entry : keybinds.entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue();

                Main.drawTextLeft(W / 2 + 1, startY, k);
                Main.drawTextRight(W - 2, startY, v);

                startY++;
            }
            Main.drawRectangle(W / 2, 1, W - 2, startY);
            Main.drawText(3 * W / 4, 1, " Keybinds ");
            Main.drawRectangle(W / 2, startY + 1, W - 2, H - 2);
            Main.drawText(3 * W / 4, startY + 1, " History (coming soon) ");
        }

        screen.refresh(Screen.RefreshType.AUTOMATIC);
    }

    @Override
    public void start() {}

    private void host() {
        Main.getInstance()
                .setCurrentScreen(
                        !SEED.isEmpty()
                                ? new HeartsLocal(
                                        Long.parseLong(SEED.substring(0, 18)), endingPoints)
                                : new HeartsLocal());
    }

    private void join() {
        Main.getInstance().setCurrentScreen(new HeartsRemote(host, port));
    }

    @Override
    public void resize() {
        try {
            build();
        } catch (IOException e) {
            System.err.println("Could not draw spider options screen: " + e.getMessage());
        }
    }

    @Override
    public Action handleInput(KeyStroke keyStroke) {
        try {
            if (keyStroke.getKeyType() == KeyType.Escape) {
                return Action.BACK_TO_MENU;
            } else if (keyStroke.getKeyType().name().contains("Arrow")) {
                List<Option> optionValues = Arrays.stream(Option.values()).toList();
                switch (keyStroke.getKeyType()) {
                    case ArrowUp:
                        selectedOption =
                                optionValues.get(
                                        Math.clamp(
                                                optionValues.indexOf(selectedOption) - 1,
                                                0,
                                                optionValues.size() - 1));
                        break;
                    case ArrowDown:
                        selectedOption =
                                optionValues.get(
                                        Math.clamp(
                                                optionValues.indexOf(selectedOption) + 1,
                                                0,
                                                optionValues.size() - 1));
                        break;
                    case ArrowLeft:
                        if (selectedOption == Option.ENDPOINTS && endingPoints > 0)
                            endingPoints -= 5;
                        if (selectedOption == Option.PORT && port > 0) port--;
                        break;
                    case ArrowRight:
                        if (selectedOption == Option.ENDPOINTS) endingPoints += 5;
                        if (selectedOption == Option.PORT) port++;
                        break;
                }
                build();
            } else if (keyStroke.getKeyType() == KeyType.Enter) {
                if (selectedOption == Option.PLAY_HOST) {
                    host();
                } else if (selectedOption == Option.PLAY_JOIN) {
                    join();
                }
            } else if (keyStroke.getKeyType() == KeyType.Character) {
                Character c = keyStroke.getCharacter();
                // Update vars and print chars
                if (selectedOption == Option.SEED && (Character.isDigit(c) || c == '-')) {
                    SEED += c;
                } else if (selectedOption == Option.HOST
                        && (Character.isDigit(c) || c == '.' || Character.isAlphabetic(c))) {
                    host += c;
                }
                build();
            } else if (keyStroke.getKeyType() == KeyType.Backspace) {
                if (selectedOption == Option.SEED) {
                    if (SEED.length() <= 1) SEED = "";
                    else SEED = SEED.substring(0, SEED.length() - 1);
                } else if (selectedOption == Option.HOST) {
                    if (host.length() <= 1) host = "";
                    else host = host.substring(0, host.length() - 1);
                }
                build();
            }
        } catch (IOException e) {
            System.out.println("Ignored exception: " + e.getMessage());
        }
        return Action.NONE;
    }
}
