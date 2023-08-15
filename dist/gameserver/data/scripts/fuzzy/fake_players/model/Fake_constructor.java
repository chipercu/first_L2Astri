package fuzzy.fake_players.model;

import l2ft.commons.util.Rnd;
import l2ft.gameserver.Config;
import l2ft.gameserver.data.xml.holder.SkillAcquireHolder;
import l2ft.gameserver.model.Player;
import l2ft.gameserver.model.SkillLearn;
import l2ft.gameserver.model.actor.instances.player.Bonus;
import l2ft.gameserver.model.actor.instances.player.ShortCut;
import l2ft.gameserver.model.base.AcquireType;
import l2ft.gameserver.model.base.ClassId;
import l2ft.gameserver.model.base.Experience;
import l2ft.gameserver.model.items.ItemInstance;
import l2ft.gameserver.network.l2.GameClient;
import l2ft.gameserver.network.l2.s2c.CharacterCreateSuccess;
import l2ft.gameserver.network.l2.s2c.CharacterSelectionInfo;
import l2ft.gameserver.tables.SkillTable;
import l2ft.gameserver.templates.PlayerTemplate;
import l2ft.gameserver.templates.item.CreateItem;
import l2ft.gameserver.templates.item.ItemTemplate;
import l2ft.gameserver.utils.ItemFunctions;
import l2ft.gameserver.utils.Location;

/**
 * Created by a.kiperku
 * Date: 15.08.2023
 */

public class Fake_constructor {

    private String name;
    private String title;
    private int level;
    private ClassId classId;
    private String sex;


    public Fake_constructor(String name, String title, int level, ClassId classId, String sex) {
        this.name = name;
        this.title = title;
        this.level = level;
        this.classId = classId;
        this.sex = sex;
    }

    public void initNewChar(Player spawner) {
        int _sex = 0;
        if (sex.equals("мужской")){
            _sex = 1;
        }

        Player newChar = Player.create(classId.getId(), _sex, "fake", name, Rnd.get(3), Rnd.get(3), Rnd.get(3));
        if (newChar == null)
            return;

        PlayerTemplate template = newChar.getTemplate();

        Player.restoreCharSubClasses(newChar);

        if (Config.STARTING_ADENA > 0)
            newChar.addAdena(Config.STARTING_ADENA);

        if (Config.STARTING_LVL != 0)
            newChar.addExpAndSp(Experience.LEVEL[Config.STARTING_LVL] - newChar.getExp(), 0, 0, 0, false, false);

        newChar.setLoc(template.spawnLoc);

        if (Config.CHAR_TITLE)
            newChar.setTitle(Config.ADD_CHAR_TITLE);
        else
            newChar.setTitle("");

        if (Config.SERVICES_RATE_TYPE != Bonus.NO_BONUS && Config.SERVICES_RATE_CREATE_PA != 0) {
            newChar.setVar("createCharBonus", (int) (System.currentTimeMillis() / 1000L * (60 * 60 * 24 * Config.SERVICES_RATE_CREATE_PA)), -1);
        }

        for (CreateItem i : template.getItems()) {
            ItemInstance item = ItemFunctions.createItem(i.getItemId());
            newChar.getInventory().addItem(item);

            if (i.getShortcut() - 1 > -1) // tutorial book
                newChar.registerShortCut(new ShortCut(Math.min(i.getShortcut() - 1, 11), 0, ShortCut.TYPE_ITEM, item.getObjectId(), -1, 1));

            if (i.isEquipable() && item.isEquipable() && (newChar.getActiveWeaponItem() == null || item.getTemplate().getType2() != ItemTemplate.TYPE2_WEAPON))
                newChar.getInventory().equipItem(item);
        }

        ClassId nclassId = ClassId.VALUES[classId.getId()];
        if (Config.ALLOW_START_ITEMS) {
            if (nclassId.isMage()) {
                for (int i = 0; i < Config.START_ITEMS_MAGE.length; i++) {
                    ItemInstance item = ItemFunctions.createItem(Config.START_ITEMS_MAGE[i]);
                    item.setCount(Config.START_ITEMS_MAGE_COUNT[i]);
                    newChar.getInventory().addItem(item);
                }
            } else {
                for (int i = 0; i < Config.START_ITEMS_FITHER.length; i++) {
                    ItemInstance item = ItemFunctions.createItem(Config.START_ITEMS_FITHER[i]);
                    item.setCount(Config.START_ITEMS_FITHER_COUNT[i]);
                    newChar.getInventory().addItem(item);
                }
            }
        }
        // Adventurer's Scroll of Escape
        ItemInstance item = ItemFunctions.createItem(10650);
        item.setCount(5);
        newChar.getInventory().addItem(item);

        // Scroll of Escape: Kamael Village
        item = ItemFunctions.createItem(9716);
        item.setCount(10);
        newChar.getInventory().addItem(item);
        long exp_add = Experience.LEVEL[level] - newChar.getExp();
        newChar.addExpAndSp(exp_add, 1000000000);

        for (SkillLearn skill : SkillAcquireHolder.getInstance().getAvailableSkills(newChar, AcquireType.NORMAL))
            newChar.addSkill(SkillTable.getInstance().getInfo(skill.getId(), skill.getLevel()), true);

        if (newChar.getSkillLevel(1001) > 0) // Soul Cry
            newChar.registerShortCut(new ShortCut(1, 0, ShortCut.TYPE_SKILL, 1001, 1, 1));
        if (newChar.getSkillLevel(1177) > 0) // Wind Strike
            newChar.registerShortCut(new ShortCut(1, 0, ShortCut.TYPE_SKILL, 1177, 1, 1));
        if (newChar.getSkillLevel(1216) > 0) // Self Heal
            newChar.registerShortCut(new ShortCut(2, 0, ShortCut.TYPE_SKILL, 1216, 1, 1));

        // add attack, take, sit shortcut
        newChar.registerShortCut(new ShortCut(0, 0, ShortCut.TYPE_ACTION, 2, -1, 1));
        newChar.registerShortCut(new ShortCut(3, 0, ShortCut.TYPE_ACTION, 5, -1, 1));
        newChar.registerShortCut(new ShortCut(10, 0, ShortCut.TYPE_ACTION, 0, -1, 1));
        // понял как на панельке отобразить. нц софт 10-11 панели сделали(by VISTALL)
        // fly transform
        newChar.registerShortCut(new ShortCut(0, ShortCut.PAGE_FLY_TRANSFORM, ShortCut.TYPE_SKILL, 911, 1, 1));
        newChar.registerShortCut(new ShortCut(3, ShortCut.PAGE_FLY_TRANSFORM, ShortCut.TYPE_SKILL, 884, 1, 1));
        newChar.registerShortCut(new ShortCut(4, ShortCut.PAGE_FLY_TRANSFORM, ShortCut.TYPE_SKILL, 885, 1, 1));
        // air ship
        newChar.registerShortCut(new ShortCut(0, ShortCut.PAGE_AIRSHIP, ShortCut.TYPE_ACTION, 70, 0, 1));

        newChar.setCurrentHpMp(newChar.getMaxHp(), newChar.getMaxMp());
        newChar.setCurrentCp(0); // retail
        newChar.setOnlineStatus(true);
        newChar.setOfflineMode(false);
        newChar.setIsOnline(true);
        newChar.updateOnlineStatus();
        newChar.setLoc(Location.findAroundPosition(spawner.getLoc(), 300, spawner.getGeoIndex()));
        newChar.spawnMe();


        newChar.store(false);
        newChar.getInventory().store();

    }




    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public ClassId getClassId() {
        return classId;
    }

    public void setClassId(ClassId classId) {
        this.classId = classId;
    }

    @Override
    public String toString() {
        return "Fake_constructor{" +
                "name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", level=" + level +
                ", classId=" + classId.name() +
                ", sex='" + sex + '\'' +
                '}';
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }
}
