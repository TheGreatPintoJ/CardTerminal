package me.pintoadmin.cardTerminal;

public abstract class Game extends CustomScreen {
    public abstract Game newGame();

    public abstract Game restart();

    public abstract long getSeed();

    public abstract void redraw();

    public abstract void drawCard(boolean logged);
}
