package me.pintoadmin.cardTerminal.games;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Supplier;
import me.pintoadmin.cardTerminal.Main;
import me.pintoadmin.cardTerminal.screens.GameOptions;

public class SolitaireOptions extends GameOptions {
    private final Screen screen = Main.screen;
    private final TextGraphics g = Main.g;

    private final Supplier<Integer> W = () -> g.getSize().getColumns();
    private final Supplier<Integer> H = () -> g.getSize().getRows();

    private long SEED = 0;
    private GameType type = GameType.KLONDIKE1;

    public enum GameType {
        KLONDIKE1,
        KLONDIKE3,
        VEGAS,
    }

    public SolitaireOptions(String name) {
        super(name);
        resize();
    }

    private void build() throws IOException {
        screen.clear();
        Arrays.stream(SGR.values()).toList().forEach(g::disableModifiers);

        int W = this.W.get(), H = this.H.get();

        Main.drawRectangle(0, 0, W - 1, H - 1);
        Main.drawText(W / 2, 0, " " + getName() + " ");
    }

    public void selectType(GameType type) {
        this.type = type;
    }

    @Override
    public void setSeed(long seed) {
        SEED = seed;
    }

    @Override
    public void start() {
        Main.getInstance().setCurrentScreen(SEED != 0 ? new Solitaire(SEED) : new Solitaire());
    }

    @Override
    public void resize() {
        try {
            build();
        } catch (IOException e) {
            System.err.println("Could not draw solitaire options screen: " + e.getMessage());
        }
    }

    @Override
    public void handleInput(KeyStroke keyStroke) throws Exception {}
}
