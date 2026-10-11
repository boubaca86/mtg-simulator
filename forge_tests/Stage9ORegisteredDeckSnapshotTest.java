package forge.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Test-only Stage9O: frozen actual registered Forge starting decklist count
 * projection, not the current mutable zone contents, never library order.
 */
public final class Stage9ORegisteredDeckSnapshotTest extends AITest {
    private static final class Snapshot {
        private final Map<Player, Map<String, Integer>> byOwner = new java.util.IdentityHashMap<>();

        Snapshot(Game game) {
            for (Player player : game.getRegisteredPlayers()) {
                final TreeMap<String, Integer> counts = new TreeMap<>();
                if (player.getRegisteredPlayer().getDeck().getMain() == null) {
                    throw new IllegalStateException("No registered main deck");
                }
                for (Map.Entry<PaperCard, Integer> entry :
                        player.getRegisteredPlayer().getDeck().getMain()) {
                    if (entry.getValue() <= 0 || entry.getKey() == null) {
                        throw new IllegalStateException("Invalid registered card count");
                    }
                    counts.merge(entry.getKey().getName(), entry.getValue(), Integer::sum);
                }
                if (counts.isEmpty()) {
                    throw new IllegalStateException("Refusing empty registered deck snapshot");
                }
                byOwner.put(player, Collections.unmodifiableMap(counts));
            }
            if (byOwner.size() != 2) {
                throw new IllegalStateException("Exactly two players required");
            }
        }

        Map<String, Integer> owned(Player player) {
            Map<String, Integer> result = byOwner.get(player);
            if (result == null) {
                throw new IllegalArgumentException("Unknown registered player");
            }
            return result;
        }

        String observe(Player self, Player opponent) {
            if (self == opponent || !byOwner.containsKey(self) || !byOwner.containsKey(opponent)) {
                throw new IllegalArgumentException("Invalid actor perspective");
            }
            JsonObject view = new JsonObject();
            view.addProperty("objective", "Win the game under referee-enforced Magic rules");
            JsonObject own = new JsonObject();
            JsonArray hand = new JsonArray();
            for (Card c : self.getCardsIn(ZoneType.Hand)) {
                hand.add(c.isFaceDown() ? "FACE_DOWN_UNKNOWN" : c.getName());
            }
            own.add("hand", hand);
            JsonArray deck = new JsonArray();
            for (Map.Entry<String, Integer> entry : owned(self).entrySet()) {
                JsonArray item = new JsonArray();
                item.add(entry.getKey());
                item.add(entry.getValue());
                deck.add(item);
            }
            own.add("decklist_multiset", deck);
            JsonArray ownTable = new JsonArray();
            for (Card c : self.getCardsIn(ZoneType.Battlefield)) {
                ownTable.add(tableCard(c));
            }
            own.add("table", ownTable);
            view.add("own_cards", own);
            JsonArray opponentTable = new JsonArray();
            for (Card c : opponent.getCardsIn(ZoneType.Battlefield)) {
                opponentTable.add(tableCard(c));
            }
            view.add("opponent_table", opponentTable);
            JsonObject life = new JsonObject();
            life.addProperty("self", self.getLife());
            life.addProperty("opponent", opponent.getLife());
            view.add("life_totals", life);
            return view.toString();
        }
    }

    private static JsonObject tableCard(Card c) {
        JsonObject record = new JsonObject();
        if (c.isFaceDown()) {
            record.addProperty("appearance", "face_down");
        } else {
            record.addProperty("appearance", "face_up");
            record.addProperty("card", c.getName());
        }
        return record;
    }

    private static Game registeredGame() {
        Deck deckA = new Deck("Registered A");
        deckA.getMain().add(known("Forest"), 4);
        deckA.getMain().add(known("Shock"), 2);
        deckA.getMain().add(known("Llanowar Elves"), 1);
        Deck deckB = new Deck("Registered B");
        deckB.getMain().add(known("Island"), 3);
        deckB.getMain().add(known("Counterspell"), 2);
        deckB.getMain().add(known("Grizzly Bears"), 2);
        List<RegisteredPlayer> players = new ArrayList<>();
        players.add(new RegisteredPlayer(deckA).setPlayer(new LobbyPlayerAi("actorA", null)));
        players.add(new RegisteredPlayer(deckB).setPlayer(new LobbyPlayerAi("actorB", null)));
        GameRules rules = new GameRules(GameType.Constructed);
        Match match = new Match(rules, players, "Stage9O");
        Game game = new Game(players, rules, match);
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(1));
        game.getPhaseHandler().onStackResolved();
        return game;
    }

    private static PaperCard known(String name) {
        PaperCard card = FModel.getMagicDb().getCommonCards().getCard(name);
        if (card == null) {
            throw new IllegalStateException("Card absent in pinned Forge: " + name);
        }
        return card;
    }

    private static void seedLibrary(Player player, List<String> names) {
        for (String name : names) {
            Card c = Card.fromPaperCard(known(name), player);
            c.setGameTimestamp(player.getGame().getNextTimestamp());
            player.getZone(ZoneType.Library).add(c);
        }
    }

    private static List<String> flattenRegisteredMain(Player player) {
        List<String> items = new ArrayList<>();
        for (Map.Entry<PaperCard, Integer> entry : player.getRegisteredPlayer().getDeck().getMain()) {
            for (int i = 0; i < entry.getValue(); ++i) {
                items.add(entry.getKey().getName());
            }
        }
        return items;
    }

    private static final class Worker implements AutoCloseable {
        final Process process;
        final BufferedReader input;
        final BufferedWriter output;
        Worker() throws Exception {
            String script = System.getenv("STAGE9O_WORKER");
            Assert.assertNotNull(script, "Worker path required");
            ProcessBuilder builder = new ProcessBuilder("python3", "-I", "-S", "-u", script);
            builder.environment().clear();
            builder.environment().put("PYTHONIOENCODING", "utf-8");
            builder.redirectError(ProcessBuilder.Redirect.INHERIT);
            process = builder.start();
            input = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            output = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        }
        JsonObject ask(String payload) throws Exception {
            Assert.assertTrue(payload.length() < 16384);
            output.write(payload);
            output.newLine();
            output.flush();
            String reply = input.readLine();
            Assert.assertNotNull(reply);
            Assert.assertTrue(reply.length() < 2048);
            JsonObject decoded = JsonParser.parseString(reply).getAsJsonObject();
            Assert.assertEquals(decoded.get("intent").getAsString(), "pass");
            Assert.assertEquals(decoded.get("pid").getAsLong(), process.pid());
            return decoded;
        }
        @Override public void close() throws Exception {
            output.close();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                Assert.fail("actor did not exit");
            }
            Assert.assertEquals(process.exitValue(), 0);
            input.close();
        }
    }

    @Test(timeOut = 120000)
    public void actualRegisteredDecksProjectExactMultiplicityAndSeparateOwnership() {
        Game game = registeredGame();
        Snapshot snapshot = new Snapshot(game);
        Player a = game.getPlayers().get(0), b = game.getPlayers().get(1);
        Assert.assertEquals(snapshot.owned(a).get("Forest"), Integer.valueOf(4));
        Assert.assertEquals(snapshot.owned(a).get("Shock"), Integer.valueOf(2));
        Assert.assertEquals(snapshot.owned(a).get("Llanowar Elves"), Integer.valueOf(1));
        Assert.assertEquals(snapshot.owned(b).get("Island"), Integer.valueOf(3));
        Assert.assertEquals(snapshot.owned(b).get("Counterspell"), Integer.valueOf(2));
        Assert.assertEquals(snapshot.owned(b).get("Grizzly Bears"), Integer.valueOf(2));
        Assert.assertEquals(snapshot.owned(a).size(), 3);
        Assert.assertEquals(snapshot.owned(b).size(), 3);
        Assert.assertFalse(snapshot.observe(a, b).contains("Counterspell"));
        Assert.assertFalse(snapshot.observe(b, a).contains("Llanowar Elves"));
    }

    @Test(timeOut = 120000)
    public void snapshotIsFrozenAfterRegistrationAndIgnoresLaterDeckMutation() {
        Game game = registeredGame();
        Snapshot snapshot = new Snapshot(game);
        Player a = game.getPlayers().get(0);
        String prior = snapshot.observe(a, game.getPlayers().get(1));
        try {
            snapshot.owned(a).put("Leak", 999);
            Assert.fail("frozen snapshot writable");
        } catch (UnsupportedOperationException expected) {
            Assert.assertEquals(snapshot.owned(a).get("Forest"), Integer.valueOf(4));
        }
        a.getRegisteredPlayer().getDeck().getMain().add(known("Mountain"), 50);
        Assert.assertEquals(snapshot.observe(a, game.getPlayers().get(1)), prior);
    }

    @Test(timeOut = 120000)
    public void realDrawFromRegisteredLibraryChangesOnlyOwnHandNotDecklist() {
        Game game = registeredGame();
        Snapshot snapshot = new Snapshot(game);
        Player a = game.getPlayers().get(0), b = game.getPlayers().get(1);
        List<String> registered = flattenRegisteredMain(a);
        Assert.assertEquals(registered.size(), 7);
        seedLibrary(a, registered);
        String bBefore = snapshot.observe(b, a);
        JsonArray listBefore = JsonParser.parseString(snapshot.observe(a, b)).getAsJsonObject()
                .getAsJsonObject("own_cards").getAsJsonArray("decklist_multiset");
        int startingLibrary = a.getCardsIn(ZoneType.Library).size();
        a.drawCard();
        Assert.assertEquals(a.getCardsIn(ZoneType.Library).size(), startingLibrary - 1);
        Assert.assertEquals(a.getCardsIn(ZoneType.Hand).size(), 1);
        Assert.assertEquals(snapshot.observe(b, a), bBefore, "other AI cannot see actual private draw");
        JsonArray listAfter = JsonParser.parseString(snapshot.observe(a, b)).getAsJsonObject()
                .getAsJsonObject("own_cards").getAsJsonArray("decklist_multiset");
        Assert.assertEquals(listAfter, listBefore, "starting deck count cannot shrink after draw");
    }

    @Test(timeOut = 120000)
    public void differentExactLibraryOrdersYieldByteIdenticalActorObservations() {
        Game one = registeredGame(), two = registeredGame();
        Player a1 = one.getPlayers().get(0), b1 = one.getPlayers().get(1);
        Player a2 = two.getPlayers().get(0), b2 = two.getPlayers().get(1);
        List<String> original = flattenRegisteredMain(a1);
        List<String> reversed = new ArrayList<>(original);
        Collections.reverse(reversed);
        seedLibrary(a1, original);
        seedLibrary(a2, reversed);
        seedLibrary(b1, Arrays.asList("Island", "Counterspell", "Grizzly Bears"));
        seedLibrary(b2, Arrays.asList("Grizzly Bears", "Counterspell", "Island"));
        Assert.assertEquals(new Snapshot(one).observe(a1, b1), new Snapshot(two).observe(a2, b2));
        Assert.assertEquals(new Snapshot(one).observe(b1, a1), new Snapshot(two).observe(b2, a2));
    }

    @Test(timeOut = 120000)
    public void twoIndependentActorsOnlyReceiveTheirOwnRegisteredStartingDeck() throws Exception {
        Game game = registeredGame();
        Snapshot snapshot = new Snapshot(game);
        Player a = game.getPlayers().get(0), b = game.getPlayers().get(1);
        try (Worker first = new Worker(); Worker second = new Worker()) {
            Assert.assertNotEquals(first.process.pid(), second.process.pid());
            String viewA = snapshot.observe(a, b), viewB = snapshot.observe(b, a);
            JsonObject replyA = first.ask(viewA), replyB = second.ask(viewB);
            Assert.assertFalse(viewA.contains("Counterspell"));
            Assert.assertFalse(viewB.contains("Llanowar Elves"));
            Assert.assertNotEquals(replyA.get("observation_sha256").getAsString(),
                    replyB.get("observation_sha256").getAsString());
        }
    }

    @Test(timeOut = 120000)
    public void hiddenOpponentZoneAndFacedownOriginalDoNotLeakWithRegisteredDeck() {
        Game game = registeredGame();
        Snapshot snap = new Snapshot(game);
        Player a = game.getPlayers().get(0), b = game.getPlayers().get(1);
        String before = snap.observe(a, b);
        Card hidden = Card.fromPaperCard(known("Grizzly Bears"), b);
        hidden.setGameTimestamp(game.getNextTimestamp());
        b.getZone(ZoneType.Hand).add(hidden);
        seedLibrary(b, Arrays.asList("Counterspell", "Island"));
        Assert.assertEquals(snap.observe(a, b), before);
        Card facedown = Card.fromPaperCard(known("Grizzly Bears"), b);
        facedown.setGameTimestamp(game.getNextTimestamp());
        facedown.turnFaceDown(true);
        b.getZone(ZoneType.Battlefield).add(facedown);
        String visible = snap.observe(a, b);
        JsonArray table = JsonParser.parseString(visible).getAsJsonObject().getAsJsonArray("opponent_table");
        Assert.assertEquals(table.size(), 1);
        Assert.assertEquals(table.get(0).getAsJsonObject().size(), 1);
        Assert.assertFalse(visible.contains("Grizzly Bears"));
    }
}
