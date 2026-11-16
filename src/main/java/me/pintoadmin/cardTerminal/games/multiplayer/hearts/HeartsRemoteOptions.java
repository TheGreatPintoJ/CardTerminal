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

public class HeartsRemoteOptions extends GameOptions {
    private final Screen screen = Main.screen;
    private final TextGraphics g = Main.g;

    private long suggestedSeed = new Random().nextLong();

    private String host = "";
    private int port = 51337;

    private final Supplier<Integer> W = () -> g.getSize().getColumns();
    private final Supplier<Integer> H = () -> g.getSize().getRows();

    private Option selectedOption = Option.HOST;

    private enum Option {
        HOST,
        PORT,
        PLAY
    }

    public HeartsRemoteOptions(String name) {
        super(name);
    }

    @Override
    public void setSeed(long seed) {}

    private void build() throws IOException {
        screen.clear();
        Arrays.stream(SGR.values()).toList().forEach(g::disableModifiers);
        g.enableModifiers(SGR.BOLD);

        int W = this.W.get(), H = this.H.get();

        Main.drawRectangle(0, 0, W - 1, H - 1);
        Main.drawText(W / 2, 0, " " + getName() + " ");

        if (selectedOption == Option.HOST) g.enableModifiers(SGR.REVERSE);
        Main.drawRectangle(8, 1, (W - 2) / 2, 3);
        g.disableModifiers(SGR.REVERSE);
        Main.drawTextLeft(2, 2, "HOST:");
        Main.drawTextLeft(9, 2, host);

        if (selectedOption == Option.PORT) g.enableModifiers(SGR.REVERSE);
        Main.drawRectangle(17, 4, (W - 2) / 2, 6);
        g.disableModifiers(SGR.REVERSE);
        Main.drawTextLeft(2, 5, "PORT:");
        g.setCharacter(18, 5, '<');
        Main.drawText(Math.abs((17 + W) / 4), 5, String.valueOf(port));
        g.setCharacter(W / 2 - 2, 5, '>');

        if (selectedOption == Option.PLAY) g.enableModifiers(SGR.REVERSE);
        Main.drawText(W / 2, H - 1, " Join ");
        g.disableModifiers(SGR.REVERSE);

        screen.refresh(Screen.RefreshType.AUTOMATIC);
    }

    @Override
    public void start() {
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
                        if (selectedOption == Option.PORT) port--;
                        break;
                    case ArrowRight:
                        if (selectedOption == Option.PORT) port++;
                        break;
                }
                build();
            } else if (keyStroke.getKeyType() == KeyType.Enter) {
                if (selectedOption == Option.PLAY) {
                    start();
                }
            } else if (keyStroke.getKeyType() == KeyType.Character) {
                Character c = keyStroke.getCharacter();
                // Update vars and print chars
                if (selectedOption == Option.HOST && (Character.isDigit(c) || c == '.')) {
                    host += c;
                }
                build();
            } else if (keyStroke.getKeyType() == KeyType.Backspace) {
                if (selectedOption == Option.HOST) {
                    if (host.length() == 1) host = "";
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
