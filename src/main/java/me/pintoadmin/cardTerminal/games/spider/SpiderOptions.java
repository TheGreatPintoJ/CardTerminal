package me.pintoadmin.cardTerminal.games.spider;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;
import java.io.*;
import java.util.*;
import java.util.function.*;
import me.pintoadmin.cardTerminal.*;
import me.pintoadmin.cardTerminal.screens.*;

public class SpiderOptions extends GameOptions {
    private final Screen screen = Main.screen;
    private final TextGraphics g = Main.g;

    private final Supplier<Integer> W = () -> g.getSize().getColumns();
    private final Supplier<Integer> H = () -> g.getSize().getRows();

    private long suggestedSeed = new Random().nextLong();
    private long SEED = 0;
    private final Supplier<String> seedString = () -> String.valueOf(SEED);
    private Spider.GameType type = Spider.GameType.SUIT1;

    private SpiderOptions.Option selectedOption = SpiderOptions.Option.SEED;

    private enum Option {
        SEED,
        TYPE,
        //        AUTOPLAY,
        PLAY
    }

    public SpiderOptions(String name) {
        super(name);
    }

    private void build() throws IOException {
        screen.clear();
        Arrays.stream(SGR.values()).toList().forEach(g::disableModifiers);
        g.enableModifiers(SGR.BOLD);

        int W = this.W.get(), H = this.H.get();

        Main.drawRectangle(0, 0, W - 1, H - 1);
        Main.drawText(W / 2, 0, " " + getName() + " ");

        if (selectedOption == SpiderOptions.Option.SEED) g.enableModifiers(SGR.REVERSE);
        Main.drawRectangle(8, 1, (W - 2) / 2, 3);
        g.disableModifiers(SGR.REVERSE);
        Main.drawTextLeft(2, 2, "SEED:");
        if (SEED == 0) g.setForegroundColor(new TextColor.RGB(100, 100, 100));
        Main.drawTextLeft(9, 2, String.valueOf(SEED == 0 ? suggestedSeed : SEED));
        if (SEED == 0) g.setForegroundColor(TextColor.ANSI.WHITE);

        if (selectedOption == SpiderOptions.Option.TYPE) g.enableModifiers(SGR.REVERSE);
        Main.drawRectangle(8, 4, (W - 2) / 2, 6);
        g.disableModifiers(SGR.REVERSE);
        Main.drawTextLeft(2, 5, "TYPE:");
        g.setCharacter(9, 5, '<');
        Main.drawText(Math.abs((8 + W) / 4), 5, type.name());
        g.setCharacter(W / 2 - 2, 5, '>');

        //        if (selectedOption == SpiderOptions.Option.AUTOPLAY)
        // g.enableModifiers(SGR.REVERSE);
        //        Main.drawRectangle((W / 2) - 5, 7, W / 2 - 1, 9);
        //        Main.drawText((W / 2) - 3, 8, autoPlay ? " X " : "   ");
        //        g.disableModifiers(SGR.REVERSE);
        //        Main.drawTextLeft(2, 8, "AutoPlay:");

        if (selectedOption == SpiderOptions.Option.PLAY) g.enableModifiers(SGR.REVERSE);
        Main.drawText(W / 2, H - 1, " Play ");
        g.disableModifiers(SGR.REVERSE);

        Main.drawRectangle(W / 2, 1, W - 2, H - 2);
        Main.drawText(3 * W / 4, 1, " History (coming soon) ");

        screen.refresh(Screen.RefreshType.AUTOMATIC);
    }

    @Override
    public void setSeed(long seed) {
        SEED = seed;
    }

    @Override
    public void start() {
        Main.getInstance().setCurrentScreen(SEED != 0 ? new Spider(SEED, type) : new Spider());
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
    public void handleInput(KeyStroke keyStroke) throws Exception {
        if (keyStroke.getKeyType() == KeyType.Escape) {
            Main.getInstance().setCurrentScreen(new MainMenu());
            throw new Exception("override");
        } else if (keyStroke.getKeyType().name().contains("Arrow")) {
            List<SpiderOptions.Option> optionValues =
                    Arrays.stream(SpiderOptions.Option.values()).toList();
            List<Spider.GameType> typeValues = Arrays.stream(Spider.GameType.values()).toList();
            try {
                switch (keyStroke.getKeyType()) {
                    case ArrowUp:
                        selectedOption = optionValues.get(optionValues.indexOf(selectedOption) - 1);
                        break;
                    case ArrowDown:
                        selectedOption = optionValues.get(optionValues.indexOf(selectedOption) + 1);
                        break;
                    case ArrowLeft:
                        if (selectedOption == SpiderOptions.Option.TYPE)
                            type = typeValues.get(typeValues.indexOf(type) - 1);
                        break;
                    case ArrowRight:
                        if (selectedOption == SpiderOptions.Option.TYPE)
                            type = typeValues.get(typeValues.indexOf(type) + 1);
                        break;
                }
            } catch (IndexOutOfBoundsException ignored) {
            }
            build();
        } else if (keyStroke.getKeyType() == KeyType.Enter) {
            /*if (selectedOption == SpiderOptions.Option.AUTOPLAY) {
                autoPlay = !autoPlay;
                build();
            } else */ if (selectedOption == SpiderOptions.Option.PLAY) {
                /*if (autoPlay)
                    AutoPlay.main(
                            new String[] {
                                    String.valueOf(SEED != 0 ? SEED : suggestedSeed),
                                    String.valueOf(60),
                                    type.name()
                            });
                else*/
                Main.getInstance()
                        .setCurrentScreen(
                                SEED != 0
                                        ? new Spider(SEED, type)
                                        : new Spider(suggestedSeed, type));
            }
        } else if (keyStroke.getKeyType() == KeyType.Character) {
            Character c = keyStroke.getCharacter();
            // Update vars and print chars
            if (selectedOption == SpiderOptions.Option.SEED && Character.isDigit(c)) {
                SEED = Long.parseLong(String.valueOf(SEED) + c);
            }
            build();
        } else if (keyStroke.getKeyType() == KeyType.Backspace) {
            if (selectedOption == SpiderOptions.Option.SEED) {
                String s = seedString.get();
                if (s.length() == 1) SEED = 0;
                else SEED = Long.parseLong(s.substring(0, s.length() - 1));
            }
            build();
        }
    }
}
