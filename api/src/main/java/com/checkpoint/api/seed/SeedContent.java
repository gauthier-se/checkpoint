package com.checkpoint.api.seed;

import java.util.List;

/**
 * Static pools of fake content used by the database seeder.
 * Pure data, no Spring dependencies.
 */
final class SeedContent {

    private SeedContent() {
    }

    /** Default password assigned to every seeded user (admin and fake users). */
    static final String SHARED_PASSWORD = "Password1!";

    static final List<String> PSEUDOS = List.of(
            "PixelHunter", "RetroKnight", "QuestBard", "DragonSlayer42", "NeonNomad",
            "ArcadeWizard", "ShadowBlade", "MoonRider", "VoxelKing", "GameSage",
            "LootGoblin", "CritFishel", "CozyGamer", "SpeedrunQueen", "BackstabBob",
            "ManaHoarder", "PyroPhoenix", "SilentSniper", "BossRusher", "GrindMaster",
            "PocketRPG", "DungeonDan", "EpicElise", "FrostbiteFox", "GalaxyGuru",
            "HexHero", "InfiniteIvy", "JoyConJoe", "KombatKira", "LavaLeo",
            "MysticMia", "NomadNova", "OrcRanger", "PaladinPaul", "QuestQuincy",
            "RoguelikeRyo", "StealthStella", "TitanTina", "UpwardUma", "VortexVic",
            "WizardWendy", "XenoXander", "YarnYasmin", "ZeldaZach", "AceArcher",
            "BraveBella", "CrystalCleo", "DuskDan", "EmberEva", "FableFinn",
            "GauntletGus", "HuntressHel", "IronIvor", "JadeJester", "KrakenKai",
            "LumiLou", "MossyMo", "NightNico", "OakOlly", "PinePip"
    );

    static final List<String> BIO_TEMPLATES = List.of(
            "Souls fan, currently lost in another playthrough.",
            "Cozy game enjoyer. Will rate your farming sim.",
            "JRPG addict: the longer the better.",
            "Speedrunning is a lifestyle.",
            "Indie game discoverer. Hidden gems > AAA.",
            "Couch co-op evangelist.",
            "Coffee, lo-fi, controller. Repeat.",
            "Backlog at 200 and counting. No regrets.",
            "Retro purist. Cartridges only.",
            "Roguelike connoisseur. One more run.",
            "FPS veteran since the LAN party era.",
            "Casual on weekdays, hardcore on weekends.",
            "Story over gameplay. Always.",
            "Platformer enthusiast: pixel-perfect or nothing.",
            "Strategy games are my therapy.",
            "Racing sim driver, real life passenger.",
            "Just here for the lore.",
            "Pokemon trainer for life.",
            "MMORPG refugee, looking for a new home.",
            "Currently 100%-ing a metroidvania."
    );

    static final List<String> REVIEW_SNIPPETS = List.of(
            "Genuinely surprised by how much I enjoyed this. The pacing is tight and the mechanics click together once you push past the early hours.",
            "Beautiful art direction carried me through some rough difficulty spikes. Would replay just for the soundtrack.",
            "Combat feels weighty and deliberate. Boss fights are the highlight: every encounter feels designed, not just thrown together.",
            "Story stuck the landing in a way I did not expect. The middle drags but the payoff is worth it.",
            "Mechanically polished but emotionally hollow. Fun for a weekend, forgettable a month later.",
            "If you bounced off the early game, push through. The systems open up beautifully around the 10-hour mark.",
            "Best in the genre this year. The level design alone is worth the price of admission.",
            "Charming, weird, and a little janky in places. The kind of game I will recommend to friends with a footnote.",
            "Disappointed. Promising premise, undercooked execution. Felt like a 30-hour tutorial.",
            "Love letter to the classics without leaning entirely on nostalgia. Confident new ideas in a familiar wrapper.",
            "Hours just disappear. The 'one more turn' loop is real and dangerous.",
            "Performance issues at launch made the first ten hours rough. Patches helped but the bad taste lingers.",
            "Stealth gameplay is the best I've seen in years. The AI actually reacts to what you do.",
            "The world feels lived-in. NPCs have routines, weather changes everything, and side quests connect in clever ways.",
            "Soundtrack is the real protagonist. Closed my eyes during the ending credits and just listened.",
            "Co-op was the right call. Solo experience would be a chore; with a friend it becomes one of my favorites of the year.",
            "Difficulty options are a masterclass: accessible without sanding away the tension.",
            "Crafting system is too fiddly. Spent more time in menus than fighting.",
            "Every dungeon teaches you something new. Pure metroidvania bliss.",
            "Sequel that justifies its existence. Bigger, sharper, and weirder."
    );

    static final List<String> LIST_TITLES = List.of(
            "Best RPGs of all time",
            "Hidden gems you missed",
            "Cozy games for rainy weekends",
            "Top metroidvanias",
            "Souls-likes ranked",
            "Underrated indies of the decade",
            "Games that made me cry",
            "Shortlist for game of the year",
            "Couch co-op staples",
            "First-person shooters that aged well",
            "Stealth games done right",
            "Roguelikes worth the run",
            "Games with the best soundtracks",
            "Sequels better than the originals",
            "Visual novels for newcomers"
    );

    static final List<String> LIST_DESCRIPTIONS = List.of(
            "A personal pick. Not exhaustive: just the ones I keep coming back to.",
            "Built this list over a couple of years. Open to suggestions.",
            "Curated for friends who keep asking what to play next.",
            "Ranked by replay value, not release order.",
            "Strictly games I have finished. No backlog placeholders."
    );

    static final List<String> COMMENT_SNIPPETS = List.of(
            "Totally agree with this take.",
            "Hard disagree: I think the second act is the weakest part.",
            "Have you tried the DLC? Changes my read on the ending entirely.",
            "Soundtrack alone makes it a 10.",
            "Played this last summer. Best decision of the year.",
            "Bouncing off it hard. Maybe I will revisit later.",
            "Spoiler tag would have been nice.",
            "100% this. Wish more people would talk about it.",
            "Underrated. Glad to see someone covering it.",
            "Going to give this another try based on your review.",
            "The pacing thing is real. Almost dropped it at hour 6.",
            "Disagree on the combat: felt floaty to me.",
            "Adding it to my backlog right now.",
            "First time I see this take, fascinating angle."
    );

    static final List<String> TAG_NAMES = List.of(
            "platinum", "speedrun", "100%", "casual", "completionist",
            "co-op", "solo", "story-driven", "competitive", "challenging",
            "comfort-pick", "first-playthrough", "replay", "demo-only", "abandoned",
            "favorite", "indie", "aaa", "retro", "modern"
    );

    /**
     * Real Unsplash photo IDs that resolve to portrait/avatar style images.
     * URLs are built via {@link #unsplashUrl(String)}.
     */
    static final List<String> UNSPLASH_PHOTO_IDS = List.of(
            "1494790108377-be9c29b29330",
            "1535713875002-d1d0cf377fde",
            "1438761681033-6461ffad8d80",
            "1472099645785-5658abf4ff4e",
            "1599566150163-29194dcaad36",
            "1607746882042-944635dfe10e",
            "1500648767791-00dcc994a43e",
            "1573496359142-b8d87734a5a2",
            "1488161628813-04466f872be2",
            "1502685104226-ee32379fefbe",
            "1521119989659-a83eee488004",
            "1546961342-a4a9e0ed03f6",
            "1531123897727-8f129e1688ce",
            "1554151228-14d9def656e4",
            "1545167622-3a6ac756afa4",
            "1463453091185-61582044d556",
            "1492562080023-ab3db95bfbce",
            "1517841905240-472988babdf9",
            "1573497019418-b400bb3ab074",
            "1521572163474-6864f9cf17ab"
    );

    static String unsplashUrl(String photoId) {
        return "https://images.unsplash.com/photo-" + photoId + "?w=200&h=200&fit=crop";
    }
}
