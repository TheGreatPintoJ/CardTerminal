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

    private final Supplier<Integer> W = () -> g.getSize().getColumns();
    private final Supplier<Integer> H = () -> g.getSize().getRows();

    private Option selectedOption = Option.SEED;

    private enum Option {
        SEED,
        ENDPOINTS,
        PLAY
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

        if (selectedOption == Option.SEED) g.enableModifiers(SGR.REVERSE);
        Main.drawRectangle(8, 1, (W - 2) / 2, 3);
        g.disableModifiers(SGR.REVERSE);
        Main.drawTextLeft(2, 2, "SEED:");
        if (SEED.isBlank()) g.setForegroundColor(new TextColor.RGB(100, 100, 100));
        Main.drawTextLeft(9, 2, String.valueOf(SEED.isBlank() ? suggestedSeed : SEED));
        if (SEED.isBlank()) g.setForegroundColor(TextColor.ANSI.WHITE);

        if (selectedOption == Option.ENDPOINTS) g.enableModifiers(SGR.REVERSE);
        Main.drawRectangle(17, 4, (W - 2) / 2, 6);
        g.disableModifiers(SGR.REVERSE);
        Main.drawTextLeft(2, 5, "ENDING POINTS:");
        g.setCharacter(18, 5, '<');
        Main.drawText(Math.abs((17 + W) / 4), 5, String.valueOf(endingPoints));
        g.setCharacter(W / 2 - 2, 5, '>');

        if (selectedOption == Option.PLAY) g.enableModifiers(SGR.REVERSE);
        Main.drawText(W / 2, H - 1, " Host ");
        g.disableModifiers(SGR.REVERSE);

        screen.refresh(Screen.RefreshType.AUTOMATIC);
    }

    @Override
    public void start() {
        Main.getInstance()
                .setCurrentScreen(
                        !SEED.isEmpty()
                                ? new HeartsLocal(
                                        Long.parseLong(SEED.substring(0, 18)), endingPoints)
                                : new HeartsLocal());
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
                Main.getInstance().setCurrentScreen(new MainMenu());
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
                        if (selectedOption == Option.ENDPOINTS) endingPoints--;
                        break;
                    case ArrowRight:
                        if (selectedOption == Option.ENDPOINTS) endingPoints++;
                        break;
                }
                build();
            } else if (keyStroke.getKeyType() == KeyType.Enter) {
                /*if (selectedOption == SpiderOptions.Option.AUTOPLAY) {
                    autoPlay = !autoPlay;
                    build();
                } else */
                if (selectedOption == Option.PLAY) {
                    start();
                }
            } else if (keyStroke.getKeyType() == KeyType.Character) {
                Character c = keyStroke.getCharacter();
                // Update vars and print chars
                if (selectedOption == Option.SEED && Character.isDigit(c)) {
                    SEED = String.valueOf(SEED) + c;
                }
                build();
            } else if (keyStroke.getKeyType() == KeyType.Backspace) {
                if (selectedOption == Option.SEED) {
                    if (SEED.length() == 1) SEED = "";
                    else SEED = SEED.substring(0, SEED.length() - 1);
                }
                build();
            }
        } catch (IOException e) {
            System.out.println("Ignored exception: " + e.getMessage());
        }
        return Action.NONE;
    }
}
