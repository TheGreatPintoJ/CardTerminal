package me.pintoadmin.cardTerminal;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.*;
import com.fasterxml.jackson.databind.node.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class StatsManager {
    private static Path file;

    public static void addGame(String game, Object stats) {
        createIfNotExists();
        try {
            JsonMapper mapper = new JsonMapper();
            JsonNode rootNode = mapper.readTree(file.toFile());
            ObjectNode root =
                    rootNode != null && rootNode.isObject()
                            ? (ObjectNode) rootNode
                            : mapper.createObjectNode();

            ArrayNode games = root.withArray(game);
            games.add(mapper.valueToTree(stats));

            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), root);
        } catch (IOException e) {
            System.out.println("Could not save file: " + e.getMessage());
        }
    }

    public static double getAverageTime(String gameName) {
        try {
            JsonMapper mapper = new JsonMapper();
            JsonNode root = mapper.readTree(file.toFile());
            ArrayNode games = (ArrayNode) root.get(gameName);
            if (games == null || games.isEmpty()) return 0.0;

            Set<Long> times = new HashSet<>();
            for (JsonNode game : games) {
                times.add(game.get("time").asLong());
            }
            return times.stream().mapToLong(Long::longValue).average().orElse(0.0);
        } catch (IOException e) {
            System.out.println("Could not save file: " + e.getMessage());
            return 0.0;
        }
    }

    public static double getAverageMoves(String gameName) {
        try {
            JsonMapper mapper = new JsonMapper();
            JsonNode root = mapper.readTree(file.toFile());
            ArrayNode games = (ArrayNode) root.get(gameName);
            if (games == null || games.isEmpty()) return 0.0;

            Set<Integer> times = new HashSet<>();
            for (JsonNode game : games) {
                times.add(game.get("moves").asInt());
            }
            return times.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        } catch (IOException e) {
            System.out.println("Could not save file: " + e.getMessage());
            return 0.0;
        }
    }

    public static List<Map<String, Object>> getGames(String gameName) {
        try {
            JsonMapper mapper = new JsonMapper();
            JsonNode root = mapper.readTree(file.toFile());
            ArrayNode gameArr = (ArrayNode) root.get(gameName);
            if (gameArr == null || gameArr.isEmpty()) return new ArrayList<>();

            List<Map<String, Object>> games = new LinkedList<>();
            for (JsonNode game : gameArr) {
                Map<String, Object> thisGame = new LinkedHashMap<>();
                thisGame.put("seed", game.get("seed").asText());
                thisGame.put("moves", game.get("moves").asInt());
                thisGame.put("elapsed", game.get("elapsed").asLong());
                thisGame.put("time", game.get("time").asText());
                games.add(thisGame);
            }

            return games;
        } catch (IOException e) {
            System.out.println("Could not save file: " + e.getMessage());
            return List.of();
        }
    }

    public static void createIfNotExists() {
        if (file == null) file = getDataDir().resolve("stats.json");
        try {
            if (!Files.exists(file) || !Files.isRegularFile(file)) {
                Files.createDirectories(getDataDir());
                Files.createFile(file);
                System.out.println("Created stats file at: " + file.toAbsolutePath());
            }
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException e) {
            System.out.println("Could not create file: " + e.getMessage());
        }
    }

    public static Path getDataDir() {
        Path dir = Path.of("").toAbsolutePath();
        switch (Main.getOS()) {
            case MAC ->
                    dir =
                            Path.of(
                                    System.getProperty("user.home"),
                                    "Library",
                                    "Application Support");
            case LINUX -> {
                String xdgData = System.getenv("XDG_DATA_HOME");
                dir =
                        (xdgData != null && !xdgData.isBlank())
                                ? Path.of(xdgData)
                                : Path.of(System.getProperty("user.home"), ".local", "share");
            }
            case WINDOWS -> dir = Path.of(System.getenv("LOCALAPPDATA"));
        }
        return dir.resolve("CardTerminal");
    }
}
