package fuzzy.fake_players;

import fuzzy.fake_players.model.FakeWeaponType;

import java.util.HashMap;
import java.util.Map;

import static fuzzy.fake_players.model.FakeWeaponType.*;

public class FakeItems {

    public static final Map<String, Integer[]> robe_sets = new HashMap<String, Integer[]>() {{
        put("C", new Integer[]{
                2414, 		//Full Plate Helmet  -  None C
                439, 		//Karmian Tunic  -  Magic C
                471, 		//Karmian Stockings  -  Magic C
                2454, 		//Karmian Gloves  -  None C
                2430, 		//Karmian Boots  -  None C
        });
        put("B", new Integer[]{
                2415, 		//Avadon Circlet  -  None B
                2406, 		//Avadon Robe  -  Magic B
                5716, 		//Avadon Gloves  -  None B
                5732, 		//Avadon Boots  -  None B
        });
        put("A", new Integer[]{
                2419, 		//Majestic Circlet  -  None A
                2409, 		//Majestic Robe  -  Magic A
                5776, 		//Majestic Gauntlets  -  None A
                5788, 		//Majestic Boots  -  None A
        });
        put("S80", new Integer[]{
                15608, 		//Moirai Circlet  -  None S80
                15611, 		//Moirai Tunic  -  Magic S80
                15614, 		//Moirai Stockings  -  Magic S80
                15617, 		//Moirai Gloves  -  None S80
                15620, 		//Moirai Footwear  -  None S80
        });
    }};
    public static final Map<String, Integer[]> light_sets = new HashMap<String, Integer[]>() {{
        put("C", new Integer[]{
                2414, 		//Full Plate Helmet  -  None C
                398, 		//Plated Leather  -  Light C
                418, 		//Plated Leather Gaiters  -  Light C
                2455, 		//Plated Leather Gloves  -  None C
                2431, 		//Plated Leather Boots  -  None C
        });
        put("B", new Integer[]{
                2417, 		//Doom Helmet  -  None B
                2392, 		//Leather Armor of Doom  -  Light B
                5723, 		//Doom Gloves  -  None B
                5739, 		//Doom Boots  -  None B
        });
        put("A", new Integer[]{
                2419, 		//Majestic Circlet  -  None A
                2395, 		//Majestic Leather Armor  -  Light A
                5775, 		//Majestic Gauntlets  -  None A
                5787, 		//Majestic Boots  -  None A
        });
        put("S80", new Integer[]{
                15607, 		//Moirai Leather Helmet  -  None S80
                15610, 		//Moirai Leather Breastplate  -  Light S80
                15613, 		//Moirai Leather Leggings  -  Light S80
                15616, 		//Moirai Leather Gloves  -  None S80
                15619, 		//Moirai Leather Boots  -  None S80
        });
    }};
    public static final Map<String, Integer[]> heavy_sets = new HashMap<String, Integer[]>() {{
        put("C", new Integer[]{
                2414, 		//Full Plate Helmet  -  None C
                356, 		//Full Plate Armor  -  Heavy C
                2462, 		//Full Plate Gauntlets  -  None C
                2438, 		//Full Plate Boots  -  None C
        });
        put("B", new Integer[]{
                2416, 		//Blue Wolf Helmet  -  None B
                358, 		//Blue Wolf Breastplate  -  Heavy B
                2380, 		//Blue Wolf Gaiters  -  Heavy B
                5718, 		//Blue Wolf Gloves  -  None B
                5734, 		//Blue Wolf Boots  -  None B
        });
        put("A", new Integer[]{
                2419, 		//Majestic Circlet  -  None A
                2383, 		//Majestic Plate Armor  -  Heavy A
                5774, 		//Majestic Gauntlets  -  None A
                5786, 		//Majestic Boots  -  None A
        });
        put("S80", new Integer[]{
                15606, 		//Moirai Helmet  -  None S80
                15609, 		//Moirai Breastplate  -  Heavy S80
                15612, 		//Moirai Gaiters  -  Heavy S80
                15615, 		//Moirai Gauntlets  -  None S80
                15618, 		//Moirai Boots  -  None S80
        });
    }};
    public static final Map<String, Integer[]> jewels_sets = new HashMap<String, Integer[]>() {{
        put("C", new Integer[]{
                886, 		//Ring of Binding  -  None C
                888, 		//Blessed Ring  -  None C
                919, 		//Blessed Necklace  -  None C
                855, 		//Nassen's Earring  -  None C
                857, 		//Blessed Earring  -  None C
        });
        put("B", new Integer[]{
                901, 		//Ring of Holy Spirit  -  None B
                891, 		//Sage's Ring  -  None B
                870, 		//Earring of Holy Spirit  -  None B
                860, 		//Sage's Earring  -  None B
                932, 		//Necklace of Holy Spirit  -  None B
        });
        put("A", new Integer[]{
                862, 		//Majestic Earring  -  None A
                893, 		//Majestic Ring  -  None A
                924, 		//Majestic Necklace  -  None A
                868, 		//Earring of Phantom  -  None A
                899, 		//Ring of Phantom  -  None A
        });
        put("S80", new Integer[]{
                15724, 		//Moirai Earring  -  None S80
                15723, 		//Moirai Ring  -  None S80
                15725, 		//Moirai Necklace  -  None S80
                9455, 		//Dynasty Earrings  -  None S
                9457, 		//Dynasty Ring  -  None S
        });
    }};

    public static final Map<FakeWeaponType, Integer[]> weapons_sets = new HashMap<FakeWeaponType, Integer[]>() {{
        put(RAPIER, new Integer[]{
                9293,
                9317,
                9354,
                10461
        });
        put(ANCIENTS_WORD, new Integer[]{
                9297, 		//Saber Tooth  -  Ancient Sword C
                9311, 		//Innominate Victory  -  Ancient Sword B
                9357, 		//Durendal  -  Ancient Sword A
                10464, 		//Icarus Wingblade  -  Ancient Sword S80
        });
        put(CROSSBOW, new Integer[]{
                9259, 		//Ballista  -  Crossbow C
                9326, 		//Hell Hound  -  Crossbow B
                9363, 		//Screaming Vengeance  -  Crossbow A
                10469, 		//Icarus Shooter  -  Crossbow S80
        });
        put(MAGE_SWORD, new Integer[]{
                6313, 		//Homunkulus's Sword  -  Sword C
                7722, 		//Sword of Valhalla  -  Sword B
                5643, 		//Sword of Miracles  -  Sword A
                10440, 		//Icarus Spirit  -  Sword S80
        });
        put(POLE, new Integer[]{
                4852, 		//Orcish Poleaxe  -  Pole C
                4859, 		//Lance  -  Pole B
                8803, 		//Tiphon's Spear  -  Pole A
                10450, 		//Icarus Trident  -  Pole S80
        });
        put(DAGGER, new Integer[]{
                6358, 		//Crystal Dagger  -  Dagger C
                4778, 		//Kris  -  Dagger B
                8800, 		//Naga Storm  -  Dagger A
                10446, 		//Icarus Disperser  -  Dagger S80
        });
        put(BOW, new Integer[]{
                4815, 		//Elemental Bow  -  Bow C
                4829, 		//Bow of Peril  -  Bow B
                8808, 		//Shyeed's Bow  -  Bow A
                10445, 		//Icarus Spitter  -  Bow S80
        });
        put(SWORD, new Integer[]{
                4708, 		//Samurai Longsword  -  Sword C
                4717, 		//Sword of Damascus  -  Sword B
                5648, 		//Dark Legion's Edge  -  Sword A
                10434, 		//Icarus Sawsword  -  Sword S80
        });
        put(DUAL, new Integer[]{
                2582, 		//Katana*Katana  -  Dual Sword C
                2626, 		//Samurai Long Sword*Samurai Long Sword  -  Dual Sword B
                5706, 		//Damascus*Damascus  -  Dual Sword A
                10415, 		//Icarus Dual Sword  -  Dual Sword S80
        });
        put(BIG_SWORD, new Integer[]{
                6347, 		//Berserker Blade  -  Big Sword C
                4725, 		//Great Sword  -  Big Sword B
                8791, 		//Sword of Ipos  -  Big Sword A
                10437, 		//Icarus Heavy Arms  -  Big Sword S80
        });
        put(BLUNT, new Integer[]{
                4745, 		//Yaksa Mace  -  Blunt C
                4753, 		//Art of Battle Axe  -  Blunt B
                8794, 		//Barakiel's Axe  -  Blunt A
                10453, 		//Icarus Hammer  -  Blunt S80
        });
        put(FIST, new Integer[]{
                4800, 		//Knuckle Duster  -  Dual Fist C
                4806, 		//Bellion Cestus  -  Dual Fist B
                8810, 		//Sobekk's Hurricane  -  Dual Fist A
                10459, 		//Icarus Hand  -  Dual Fist S80
        });
        put(SHIELD, new Integer[]{
                2497, 		//Full Plate Shield  -  Shield C
                633, 		//Zubei's Shield  -  Shield B
                2498, 		//Shield of Nightmare  -  Shield A
                15621, 		//Moirai Shield  -  Shield S80
        });

    }};

}
