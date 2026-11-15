package me.pintoadmin.cardTerminal.games.multiplayer.hearts;

public class HeartsRemote extends HeartsLocal {
    private final String host;
    private final int port;

    public HeartsRemote(String host, int port) {
        super();
        this.host = host;
        this.port = port;
    }

    public HeartsRemote(String host, int port, long SEED, int endingPoints) {
        super(SEED, endingPoints);
        this.host = host;
        this.port = port;
    }

    @Override
    public void updateLocals() {}
}
