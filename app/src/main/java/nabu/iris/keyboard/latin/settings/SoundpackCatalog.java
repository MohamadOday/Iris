package nabu.iris.keyboard.latin.settings;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SoundpackCatalog {
    public static final Map<String, String> CATALOG_NAMES = new HashMap<>();
    public static final Map<String, String> CATALOG_TYPES = new HashMap<>();

    public static class CatalogEntry {
        public final String id;
        public final String name;
        public final String downloadUrl;
        public final String type;

        public CatalogEntry(String id, String name, String downloadUrl, String type) {
            this.id = id;
            this.name = name;
            this.downloadUrl = downloadUrl;
            this.type = type;
        }
    }

    public static final List<CatalogEntry> ENTRIES = new ArrayList<>();

    private static void add(String rawId, String name) {
        String cleanId = rawId.replace("-", "_");
        CATALOG_NAMES.put(cleanId, name);
        CATALOG_NAMES.put(rawId, name);

        String type = "Mechanical Switch";
        String lower = name.toLowerCase();
        if (lower.contains("click") || lower.contains("blue") || lower.contains("jade") || lower.contains("navy") || lower.contains("boxjade")) {
            type = "Clicky";
        } else if (lower.contains("thock") || lower.contains("panda") || lower.contains("brown") || lower.contains("tactile") || lower.contains("topre") || lower.contains("boba")) {
            type = "Tactile";
        } else if (lower.contains("red") || lower.contains("black") || lower.contains("cream") || lower.contains("linear") || lower.contains("yellow") || lower.contains("silver") || lower.contains("tealios") || lower.contains("alpaca")) {
            type = "Linear";
        } else if (lower.contains("model m") || lower.contains("buckling") || lower.contains("beam") || lower.contains("typewriter") || lower.contains("teleprinter") || lower.contains("model_f")) {
            type = "Buckling Spring";
        } else if (lower.contains("moan") || lower.contains("voice") || lower.contains("amogus") || lower.contains("sans") || lower.contains("zelda") || lower.contains("roblox") || lower.contains("osu") || lower.contains("gunshot") || lower.contains("boom") || lower.contains("bruh") || lower.contains("spongebob") || lower.contains("fallout") || lower.contains("goose") || lower.contains("mario") || lower.contains("animal") || lower.contains("omori") || lower.contains("opera")) {
            type = "Meme & Novelty";
        }

        CATALOG_TYPES.put(cleanId, type);
        CATALOG_TYPES.put(rawId, type);

        String downloadUrl = "https://mechvibes.com/sound-packs/" + rawId;
        ENTRIES.add(new CatalogEntry(cleanId, name, downloadUrl, type));
    }

    static {
        // Curated & Standard Packs
        add("cherrymx_blue_pbt", "Cherry MX Blue");
        add("cherrymx_brown_pbt", "Cherry MX Brown");
        add("cherrymx_red_pbt", "Cherry MX Red");
        add("cherrymx_black_abs", "Cherry MX Black");
        add("holy_pandas", "Holy Pandas");
        add("nk_creams", "NovelKeys Creams");
        add("ibm_model_m_ssk", "IBM Model M SSK");
        add("topre_realforce_87u", "Topre Realforce");
        add("nk_sherbets", "NK Sherbets");
        add("alps_blue", "Alps Blue Keyboard");

        // Complete Mechvibes Catalog
        add("sound-pack-1200000000001", "CherryMX Black - ABS keycaps");
        add("sound-pack-1200000000002", "CherryMX Black - PBT keycaps");
        add("sound-pack-1200000000003", "CherryMX Blue - ABS keycaps");
        add("sound-pack-1200000000004", "CherryMX Blue - PBT keycaps");
        add("sound-pack-1200000000005", "CherryMX Brown - ABS keycaps");
        add("sound-pack-1200000000006", "CherryMX Brown - PBT keycaps");
        add("sound-pack-1200000000007", "CherryMX Red - ABS keycaps");
        add("sound-pack-1200000000008", "CherryMX Red - PBT keycaps");
        add("sound-pack-1200000000009", "EG Crystal Purple");
        add("sound-pack-1200000000010", "EG Oreo");
        add("sound-pack-1200000000011", "NK Cream (original by Ryan)");
        add("custom-sound-pack-1200000000014", "Opera GX");
        add("sound-pack-1200000000012", "Topre Purple Hybrid - PBT keycaps");
        add("custom-sound-pack-1203000000018", "8 bit");
        add("custom-sound-pack-1203000000026", "AMOGUS");
        add("custom-sound-pack-1203000000023", "Ace Attorney Blip - Female");
        add("custom-sound-pack-1203000000024", "Ace Attorney Blip - Male");
        add("custom-sound-pack-1203000000025", "Ahegao");
        add("traveler-sound-pack-301", "Alpaca Traveler");
        add("custom-sound-pack-1203000000027", "Animal Crossing: New Leaf");
        add("custom-sound-pack-1203000000028", "Animalese");
        add("custom-sound-pack-1203000000084", "Apex Pro TKL by Akira");
        add("custom-sound-pack-1203000000030", "Banana Split Lubed by Akira");
        add("custom-sound-pack-1203000000031", "Banana Split Stock by Akira");
        add("traveler-sound-pack-302", "Black Ink (Full Travel)");
        add("traveler-sound-pack-303", "Blue Alps (Full Travel)");
        add("travler-sound-pack-304", "Box Navy (Full Travel)");
        add("custom-sound-pack-1203000000032", "Bruh");
        add("travler-sound-pack-305", "Buckling (Full Travel)");
        add("custom-sound-pack-1203000000034", "Bug Fables");
        add("custom-sound-pack-1203000000058", "Cherry MX Speed Silver by Akira");
        add("custom-sound-pack-1203000000036", "Chile");
        add("custom-sound-pack-1203000000037", "Chrono Trigger Keyboard");
        add("custom-sound-pack-1203000000038", "Creams");
        add("custom-sound-pack-1203000000039", "Cry Of Fear");
        add("custom-sound-pack-1203000000041", "Dallas Screaming at You");
        add("custom-sound-pack-1203000000081", "Never Gonna Give You Up - Rick Astley");
        add("custom-sound-pack-1203000000042", "Fallout Terminal");
        add("custom-sound-pack-1203000000048", "GOOSE");
        add("custom-sound-pack-1203000000044", "Gateron Blacks - Revolt");
        add("custom-sound-pack-1203000000045", "Gateron Browns - Revolt");
        add("custom-sound-pack-1203000000046", "Gateron Red - Revolt");
        add("custom-sound-pack-1203000000021", "Glorious Panda");
        add("custom-sound-pack-1203000000079", "Gunshot");
        add("sound-pack-v2-example-01-holy-pandas", "Holy Pandas");
        add("custom-sound-pack-1203000000019", "HyperX Aqua");
        add("custom-sound-pack-1203000000051", "Isabelle Animal Crossing");
        add("custom-sound-pack-1203000000053", "Kailh Box White");
        add("custom-sound-pack-1203000000054", "Koalas");
        add("custom-sound-pack-1203000000091", "Lincoln Typewriter");
        add("custom-sound-pack-1203000000056", "Mettaton");
        add("custom-sound-pack-1203000000057", "Minimal Tick by Art");
        add("custom-sound-pack-1203000000016", "Model F XT");
        add("custom-sound-pack-1203000000063", "OMORI Text");
        add("custom-sound-pack-1203000000069", "Osu Nagatoro");
        add("custom-sound-pack-1203000000074", "PMD menus");
        add("custom-sound-pack-1203000000093", "Papyrus");
        add("custom-sound-pack-1203000000071", "Penumbra");
        add("custom-sound-pack-1203000000073", "Phoenix Wright Blip");
        add("custom-sound-pack-1203000000075", "Pudgy OWA OWA");
        add("custom-sound-pack-1203000000077", "Razer Huntsman TE - Revolt");
        add("custom-sound-pack-1203000000076", "Razer Green (Blackwidow Elite)");
        add("travler-sound-pack-307", "Red Ink (Full Travel)");
        add("custom-sound-pack-1203000000062", "Regina");
        add("custom-sound-pack-1203000000064", "Roblox Oof - Revolt");
        add("custom-sound-pack-1203000000072", "Sans Undertale");
        add("custom-sound-pack-1203000000020", "Sexy Voice");
        add("custom-sound-pack-1203000000022", "Sine Bumps");
        add("custom-sound-pack-1203000000082", "Spongebob Laugh - Revolt");
        add("custom-sound-pack-1203000000083", "SteelSeries Apex Pro V2");
        add("custom-sound-pack-1203000000087", "Syndicate Keys");
        add("custom-sound-pack-1203000000088", "Tealios V2 on PBT");
        add("custom-sound-pack-1203000000089", "Teleprinter");
        add("custom-sound-pack-1203000000043", "The Hit");
        add("custom-sound-pack-1203000000090", "To The Past");
        add("custom-sound-pack-1203000000092", "Trails in the Sky");
        add("custom-sound-pack-1203000000047", "Trust GXT 865 ASTA");
        add("custom-sound-pack-1203000000080", "Typewriter 1.0 Beta");
        add("custom-sound-pack-1203000000094", "Vine Boom");
        add("custom-sound-pack-1203000000055", "Voice");
        add("travler-sound-pack-306", "Full Travel Soundpack");
        add("custom-sound-pack-1203000000096", "Zelda: Link to the Past");
        add("custom-sound-pack-1203000000029", "Anime Moan");
        add("custom-sound-pack-1203000000052", "Box Jade");
        add("custom-sound-pack-1203000000040", "Custom Sound");
        add("custom-sound-pack-1203000000049", "Hitmarker");
        add("custom-sound-pack-1203000000050", "HL Keys");
        add("custom-sound-pack-1203000000066", "Opera GX");
        add("custom-sound-pack-1203000000067", "osu!");
        add("custom-sound-pack-1203000000068", "osu! JD");
        add("custom-sound-pack-1203000000078", "Rosenclick");
        add("custom-sound-pack-1203000000086", "Super Paper Mario");
        add("custom-sound-pack-1203000000033", "Unicomp Classic");
    }

    public static String resolveName(String id) {
        if (id == null) return "iOS (Apple Inc. - Sampled)";
        if (id.equals("default")) return "iOS (Apple Inc. - Sampled)";
        if (id.equals("default_deep")) return "iOS Deep (Apple Inc. - Sampled)";

        String cleanId = id.replace("-", "_");
        if (CATALOG_NAMES.containsKey(cleanId)) {
            return CATALOG_NAMES.get(cleanId);
        }
        if (CATALOG_NAMES.containsKey(id)) {
            return CATALOG_NAMES.get(id);
        }

        // Format clean title
        String clean = id.replace("custom_sound_pack_", "")
                .replace("sound_pack_", "")
                .replace("custom-sound-pack-", "")
                .replace("sound-pack-", "")
                .replace("traveler-", "")
                .replace("-", " ")
                .replace("_", " ");

        String[] words = clean.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.length() > 0) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
            }
        }
        String res = sb.toString().trim();
        return res.isEmpty() ? id : res;
    }
}
