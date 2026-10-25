package com.pvptiers.mod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/** Client only, no Minecraft server plugin required. Sync is via a PUBLIC GitHub Gist.
 * This mod does not send player data to game servers. Only the owner's Gist credentials allow official edits.
 */
public final class PvpTiersClient implements ClientModInitializer {
    public static final String OWNER = "Asthi0LT3";
    public static final String[] TIERS = {"LT5", "HT5", "LT4", "HT4", "LT3", "HT3", "LT2", "HT2", "LT1", "HT1"};
    import net.minecraft.resources.Identifier;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("pvptiers-client.json");
    private static final HttpClient HTTP = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final AtomicBoolean BUSY = new AtomicBoolean();
    public static volatile Config cfg = new Config();
    private long ticks = 0;

    public static final class Config {
        public String ownerName = OWNER;
        /** Public Gist ID, the same on all client installations. Leave blank for local-only. */
        public String gistId = "";
        /** Only on Asthi0LT3's PC! Never share this credential or embed it in a distributed JAR. */
        public String ownerGistToken = "";
        public boolean showIcons = true;
        public Map<String, RankData> players = new HashMap<>();
    }

    public static final class RankData {
        public String name = "";
        public String primary = "";
        public Map<String, String> tiers = new HashMap<>();
    }

    public static boolean isOwner() {
        Minecraft client = Minecraft.getInstance();
        return client != null && client.getUser() != null && OWNER.equalsIgnoreCase(client.getUser().getName());
    }

    @Override public void onInitializeClient() {
        loadConfig();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, reg) -> dispatcher.register(
            ClientCommandManager.literal("pvptier")
              .then(ClientCommandManager.literal("help").executes(c -> help(c.getSource())))
              .then(ClientCommandManager.literal("modes").executes(c -> { msg(c.getSource(), "Modes: " + String.join(", ", PvpMode.ids())); return 1; }))
              .then(ClientCommandManager.literal("tiers").executes(c -> { msg(c.getSource(), "Tiers: " + String.join(", ", TIERS)); return 1; }))
              .then(ClientCommandManager.literal("whoami").executes(c -> { msg(c.getSource(), "Minecraft: " + Minecraft.getInstance().getUser().getName() + "; tier owner: " + isOwner()); return 1; }))
              .then(ClientCommandManager.literal("sync").executes(c -> { refreshAsync(); msg(c.getSource(), "Checking shared tiers..."); return 1; }))
              .then(ClientCommandManager.literal("view")
                .then(ClientCommandManager.argument("player", StringArgumentType.word()).executes(PvpTiersClient::view)))
              .then(ClientCommandManager.literal("set")
                .then(ClientCommandManager.argument("player", StringArgumentType.word())
                  .then(ClientCommandManager.argument("mode", StringArgumentType.word())
                    .then(ClientCommandManager.argument("tier", StringArgumentType.word()).executes(PvpTiersClient::set)))))
              .then(ClientCommandManager.literal("remove")
                .then(ClientCommandManager.argument("player", StringArgumentType.word())
                  .then(ClientCommandManager.argument("mode", StringArgumentType.word()).executes(PvpTiersClient::remove))))
              .executes(c -> help(c.getSource()))
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ++ticks;
            if (ticks == 60 || ticks % 1200 == 0) refreshAsync();
        });
    }

    private static int help(FabricClientCommandSource src) {
        msg(src, "/pvptier modes | tiers | view <name> | sync | whoami");
        msg(src, "Asthi0LT3 only: /pvptier set <name> <mode> <tier>, /pvptier remove <name> <mode>");
        return 1;
    }
    private static int view(CommandContext<FabricClientCommandSource> c) {
        String name = StringArgumentType.getString(c, "player");
        RankData data = cfg.players.get(key(name));
        if (data == null || data.tiers == null || data.tiers.isEmpty()) msg(c.getSource(), name + " has no tiers.");
        else msg(c.getSource(), name + ": " + data.tiers + " (name tag: " + data.primary + ")");
        return 1;
    }
    private static int set(CommandContext<FabricClientCommandSource> c) {
        if (!isOwner()) { msg(c.getSource(), "Only Asthi0LT3 can assign official tiers."); return 0; }
        String name = StringArgumentType.getString(c, "player");
        PvpMode mode = PvpMode.parse(StringArgumentType.getString(c, "mode"));
        String tier = StringArgumentType.getString(c, "tier").toUpperCase(Locale.ROOT);
        if (!validName(name) || mode == null || !validTier(tier)) {msg(c.getSource(), "Invalid name/mode/tier.");return 0;}
        RankData data = cfg.players.computeIfAbsent(key(name), n -> new RankData());
        data.name = name; data.tiers.put(mode.id(), tier); data.primary = mode.id();
        saveConfig(); publishAsync();
        msg(c.getSource(), "Set " + name + " " + mode.display + " " + tier + ". " + syncNotice());
        return 1;
    }
    private static int remove(CommandContext<FabricClientCommandSource> c) {
        if (!isOwner()) {msg(c.getSource(), "Only Asthi0LT3 can remove official tiers.");return 0;}
        String name = StringArgumentType.getString(c, "player");
        PvpMode mode = PvpMode.parse(StringArgumentType.getString(c, "mode"));
        if (mode == null) {msg(c.getSource(), "Invalid mode.");return 0;}
        RankData data = cfg.players.get(key(name));
        if (data == null || data.tiers.remove(mode.id()) == null) {msg(c.getSource(), "No tier to remove.");return 0;}
        if (mode.id().equals(data.primary)) data.primary = data.tiers.keySet().stream().sorted().findFirst().orElse("");
        saveConfig(); publishAsync();msg(c.getSource(), "Removed " + name + " " + mode.display + ". " + syncNotice());
        return 1;
    }
    private static boolean validTier(String tier) {for (String t: TIERS) if (t.equals(tier))return true;return false;}
    private static boolean validName(String value) {return value != null && value.matches("[A-Za-z0-9_]{3,16}");}
    private static String key(String name) {return name.toLowerCase(Locale.ROOT);}
    public static RankData getRank(String player) {return cfg.players == null ? null : cfg.players.get(key(player));}
    private static String syncNotice() {return cfg.gistId.isBlank() || cfg.ownerGistToken.isBlank() ? "LOCAL ONLY: add Gist ID and owner token to config to share." : "Publishing to your Gist...";}
    private static void msg(FabricClientCommandSource src, String text) {src.sendFeedback(Component.literal("[PvP Tiers] " + text));}
    private static void chat(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) mc.execute(() -> {if(mc.player != null) mc.player.displayClientMessage(Component.literal("[PvP Tiers] " + text), false);});
    }
    private static synchronized void loadConfig() {
        try { if (Files.isRegularFile(CONFIG_FILE)) {
            Config parsed = GSON.fromJson(Files.readString(CONFIG_FILE), Config.class);
            if (parsed != null) cfg = parsed;
        }}catch(Exception e){System.err.println("[PvP Tiers] Config read error: " + e);}
        if(cfg.players==null)cfg.players=new HashMap<>();
        if(cfg.gistId==null)cfg.gistId="";if(cfg.ownerGistToken==null)cfg.ownerGistToken="";
        cfg.ownerName = OWNER;
        saveConfig();
    }
    private static synchronized void saveConfig() {
        try {Files.createDirectories(CONFIG_FILE.getParent());Files.writeString(CONFIG_FILE,GSON.toJson(cfg),StandardCharsets.UTF_8);}
        catch(Exception e){System.err.println("[PvP Tiers] Config save error: " + e);}
    }
    /** Clients periodically read the same authoritative GitHub Gist. The token is NEVER needed on viewer computers. */
    private static void refreshAsync() {
        String gist = cfg.gistId;
        if (gist == null || !gist.matches("[a-fA-F0-9]{20,}")) return;
        if(!BUSY.compareAndSet(false,true))return;
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.github.com/gists/" + gist))
                    .header("Accept","application/vnd.github+json").header("User-Agent","Asthi-PvPTiers/1.1")
                    .timeout(java.time.Duration.ofSeconds(12)).GET().build();
                HttpResponse<String> response=HTTP.send(request,HttpResponse.BodyHandlers.ofString());
                if(response.statusCode()!=200)throw new IllegalStateException("Gist HTTP " + response.statusCode());
                JsonObject gistObj=JsonParser.parseString(response.body()).getAsJsonObject();
                String text=gistObj.getAsJsonObject("files").getAsJsonObject("tiers.json").get("content").getAsString();
                Config shared=GSON.fromJson(text,Config.class);
                if(shared==null||shared.players==null)throw new IllegalStateException("tiers.json missing players");
                Minecraft.getInstance().execute(() -> {cfg.players=shared.players;saveConfig();});
            }catch(Exception e){System.err.println("[PvP Tiers] Sync failed: " + e.getMessage());}
            finally{BUSY.set(false);}
        });
    }
    /** GitHub write is authorized by the private owner's token, not just a modifiable local username check. */
    private static void publishAsync() {
        if(cfg.gistId.isBlank()||cfg.ownerGistToken.isBlank())return;
        if(!isOwner())return;
        String gist=cfg.gistId, token=cfg.ownerGistToken;
        Config snapshot=new Config();snapshot.players=new HashMap<>(cfg.players);
        // Never publish the local token or config. Only the rank database is public.
        String payload="{\"files\":{\"tiers.json\":{\"content\":"+GSON.toJson(GSON.toJson(snapshot))+"}}}";
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest req=HttpRequest.newBuilder(URI.create("https://api.github.com/gists/"+gist))
                    .header("Authorization","Bearer " +token).header("Accept","application/vnd.github+json")
                    .header("User-Agent","Asthi-PvPTiers/1.1").header("Content-Type","application/json")
                    .timeout(java.time.Duration.ofSeconds(15))
                    .method("PATCH",HttpRequest.BodyPublishers.ofString(payload)).build();
                HttpResponse<String> resp=HTTP.send(req,HttpResponse.BodyHandlers.ofString());
                if(resp.statusCode()<200||resp.statusCode()>=300)throw new IllegalStateException("GitHub HTTP " + resp.statusCode());
                chat("Official tiers published. Other mod users will refresh automatically.");
            }catch(Exception e){chat("Could not publish tiers: " + e.getMessage());}
        });
    }
}
