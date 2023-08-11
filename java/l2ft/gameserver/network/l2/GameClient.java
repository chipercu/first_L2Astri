package l2ft.gameserver.network.l2;

import l2ft.commons.dbutils.DbUtils;
import l2ft.gameserver.Config;
import l2ft.gameserver.cache.Msg;
import l2ft.gameserver.dao.AccountPointsDAO;
import l2ft.gameserver.dao.CharacterDAO;
import l2ft.gameserver.database.DatabaseFactory;
import l2ft.gameserver.model.CharSelectInfoPackage;
import l2ft.gameserver.model.GameObjectsStorage;
import l2ft.gameserver.model.Player;
import l2ft.gameserver.network.authcomm.AuthServerCommunication;
import l2ft.gameserver.network.authcomm.SessionKey;
import l2ft.gameserver.network.authcomm.gspackets.PlayerLogout;
import l2ft.gameserver.network.l2.s2c.L2GameServerPacket;
import l2ft.commons.net.nio.impl.MMOClient;
import l2ft.commons.net.nio.impl.MMOConnection;
import l2ft.gameserver.network.security.SecondaryPasswordAuth;
import l2ft.gameserver.utils.HWID;
import org.apache.log4j.Logger;
import org.strixplatform.network.IStrixClientData;
import org.strixplatform.network.cipher.StrixGameCrypt;
import org.strixplatform.utils.StrixClientData;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;


/**
 * Represents a client connected on Game Server
 */
public final class GameClient extends MMOClient<MMOConnection<GameClient>> implements IStrixClientData {
    private static final Logger _log = Logger.getLogger(GameClient.class);
    private static final String NO_IP = "?.?.?.?";
    public StrixGameCrypt gameCrypt = null;
    private StrixClientData clientData;
    public GameClientState _state;

    private SecondaryPasswordAuth _secondaryAuth;

    public static enum GameClientState {
        CONNECTED,
        AUTHED,
        IN_GAME,
        DISCONNECTED
    }

    /**
     * Данные аккаунта
     */
    private String _login;
    private double _bonus = 1.0;
    private double _ncbonus = 1.0;
    private int _bonusExpire;

    private Player _activeChar;
    private SessionKey _sessionKey;
    private String _ip = NO_IP;
    private int revision = 0;

    public HWID.HardwareID HWIDS = null, ALLOW_HWID = null;

    private List<Integer> _charSlotMapping = new ArrayList<>();

    public GameClient(MMOConnection<GameClient> con) {
        super(con);

        _state = GameClientState.CONNECTED;
        gameCrypt = new StrixGameCrypt();
        _ip = con.getSocket().getInetAddress().getHostAddress();
    }

    @Override
    protected void onDisconnection() {
        final Player player;
        setState(GameClientState.DISCONNECTED);
        player = getActiveChar();
        setActiveChar(null);

        if (player != null) {
            player.setNetConnection(null);
            player.scheduleDelete();
        }

        if (getSessionKey() != null) {
            if (isAuthed()) {
                AuthServerCommunication.getInstance().removeAuthedClient(getLogin());
                AuthServerCommunication.getInstance().sendPacket(new PlayerLogout(getLogin()));
            } else {
                AuthServerCommunication.getInstance().removeWaitingClient(getLogin());
            }
        }
    }

    @Override
    protected void onForcedDisconnection() {
        // TODO Auto-generated method stub

    }

    public void markRestoredChar(int charslot) throws Exception {
        int objid = getObjectIdForSlot(charslot);
        if (objid < 0)
            return;

        if (_activeChar != null && _activeChar.getObjectId() == objid)
            _activeChar.setDeleteTimer(0);

        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = DatabaseFactory.getInstance().getConnection();
            statement = con.prepareStatement("UPDATE characters SET deletetime=0 WHERE obj_id=?");
            statement.setInt(1, objid);
            statement.execute();
        } catch (Exception e) {
            _log.error("", e);
        } finally {
            DbUtils.closeQuietly(con, statement);
        }
    }

    public void markToDeleteChar(int charslot) throws Exception {
        int objid = getObjectIdForSlot(charslot);
        if (objid < 0)
            return;

        if (_activeChar != null && _activeChar.getObjectId() == objid)
            _activeChar.setDeleteTimer((int) (System.currentTimeMillis() / 1000));

        Connection con = null;
        PreparedStatement statement = null;
        try {
            con = DatabaseFactory.getInstance().getConnection();
            statement = con.prepareStatement("UPDATE characters SET deletetime=? WHERE obj_id=?");
            statement.setLong(1, (int) (System.currentTimeMillis() / 1000L));
            statement.setInt(2, objid);
            statement.execute();
        } catch (Exception e) {
            _log.error("data error on update deletime char:", e);
        } finally {
            DbUtils.closeQuietly(con, statement);
        }
    }

    public void deleteChar(int charslot) throws Exception {
        //have to make sure active character must be nulled
        if (_activeChar != null)
            return;

        int objid = getObjectIdForSlot(charslot);
        if (objid == -1)
            return;

        _charSlotMapping.remove(charslot);
        CharacterDAO.getInstance().deleteCharByObjId(objid);
    }

    public Player loadCharFromDisk(int charslot) {
        int objectId = getObjectIdForSlot(charslot);
        if (objectId == -1)
            return null;

        Player character = null;
        Player oldPlayer = GameObjectsStorage.getPlayer(objectId);

        if (oldPlayer != null) {
            if (oldPlayer.isInOfflineMode() || oldPlayer.isLogoutStarted()) {
                // оффтрейдового чара проще выбить чем восстанавливать
                oldPlayer.kick();
                return null;
            } else {
                oldPlayer.sendPacket(Msg.ANOTHER_PERSON_HAS_LOGGED_IN_WITH_THE_SAME_ACCOUNT);

                GameClient oldClient = oldPlayer.getNetConnection();
                if (oldClient != null) {
                    oldClient.setActiveChar(null);
                    oldClient.closeNow(false);
                }
                oldPlayer.setNetConnection(this);
                character = oldPlayer;
            }
        }
        if (character == null)
            character = Player.restore(objectId);

        if (character != null)
            setActiveChar(character);
        else
            _log.warn("could not restore obj_id: " + objectId + " in slot:" + charslot);

        return character;
    }

    public int getObjectIdForSlot(int charslot) {
        if (charslot < 0 || charslot >= _charSlotMapping.size()) {
            _log.warn(getLogin() + " tried to modify Character in slot " + charslot + " but no characters exits at that slot.");
            return -1;
        }
        return _charSlotMapping.get(charslot);
    }

    public Player getActiveChar() {
        return _activeChar;
    }

    /**
     * @return Returns the sessionId.
     */
    public SessionKey getSessionKey() {
        return _sessionKey;
    }

    public String getLogin() {
        return _login;
    }

    public void setLoginName(String loginName) {
        _login = loginName;

        if (Config.SECOND_AUTH_ENABLED){
            _secondaryAuth = new SecondaryPasswordAuth(this);
        }
    }

    public void setActiveChar(Player player) {
        _activeChar = player;
        if (player != null)
            player.setNetConnection(this);
    }

    public void setSessionId(SessionKey sessionKey) {
        _sessionKey = sessionKey;
    }

    public void setCharSelection(CharSelectInfoPackage[] chars) {
        _charSlotMapping.clear();
        for (CharSelectInfoPackage element : chars) {
            int objectId = element.getObjectId();
            _charSlotMapping.add(objectId);
        }
    }

    public void setCharSelection(int c) {
        _charSlotMapping.clear();
        _charSlotMapping.add(c);
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }

    @Override
    public boolean encrypt(final ByteBuffer buf, final int size) {
        gameCrypt.encrypt(buf.array(), buf.position(), size);
        buf.position(buf.position() + size);
        return true;
    }

    @Override
    public boolean decrypt(ByteBuffer buf, int size) {
        boolean ret = true;
        this.gameCrypt.decrypt(buf.array(), buf.position(), size);
        return ret;
    }

    public void sendPacket(L2GameServerPacket gsp) {
        if (isConnected())
            getConnection().sendPacket(gsp);
    }

    public void sendPacket(L2GameServerPacket... gsp) {
        if (isConnected())
            getConnection().sendPacket(gsp);
    }

    public void sendPackets(List<L2GameServerPacket> gsp) {
        if (isConnected())
            getConnection().sendPackets(gsp);
    }

    public void close(L2GameServerPacket gsp) {
        if (isConnected())
            getConnection().close(gsp);
    }

    public String getIpAddr() {
        return _ip;
    }

    public byte[] enableCrypt() {
        final byte[] key = BlowFishKeygen.getRandomKey();
        gameCrypt.setKey(key);
        return key;
    }

    public double getBonus() {
        return _bonus;
    }

    public double getNcBonus() {
        return _ncbonus;
    }

    public int getBonusExpire() {
        return _bonusExpire;
    }

    public void setBonus(double bonus) {
        _bonus = bonus;
    }

    public void setNcBonus(double ncbonus) {
        _ncbonus = ncbonus;
    }

    public void setBonusExpire(int bonusExpire) {
        _bonusExpire = bonusExpire;
    }

    public GameClientState getState() {
        return _state;
    }

    public void setState(GameClientState state) {
        _state = state;
    }


    public int getPointG() {
        return AccountPointsDAO.getInstance().getPoint(getLogin());
    }

    public void setPointG(int point) {
        AccountPointsDAO.getInstance().setPoint(getLogin(), point);
    }

    public boolean hasHWID() {
        return HWIDS != null;
    }

    public void setHWID(final String hwid) {
        HWIDS = new HWID.HardwareID(hwid);
    }

    public boolean checkHWIDBanned() {
        return HWID.checkHWIDBanned(new HWID.HardwareID(getHWID()));
    }

    @SuppressWarnings("deprecation")
    public void checkHwid(String allowedHwid) {
        if (!allowedHwid.equalsIgnoreCase("") && !getHWID().equalsIgnoreCase(allowedHwid))
            closeNow(false);
    }

    public SecondaryPasswordAuth getSecondaryAuth() {
        return _secondaryAuth;
    }

    public String getHWID() {
        return HWIDS != null ? HWIDS.Full : null;
    }

    private int _failedPackets = 0;
    private int _unknownPackets = 0;

    public void onPacketReadFail() {
        if (_failedPackets++ >= 10) {
            _log.warn("Too many client packet fails, connection closed : " + this);
            closeNow(true);
        }
    }

    public void onUnknownPacket() {
        if (_unknownPackets++ >= 10) {
            _log.warn("Too many client unknown packets, connection closed : " + this);
            closeNow(true);
        }
    }

    /*@Override
    public boolean equals(Object obj)
    {
        boolean e = false;
        if(obj instanceof GameClient)
        {
            Player p =((GameClient)obj).getActiveChar();
            Player ap = getActiveChar();
            if(p != null && ap != null)
                e = p.getObjectId() == ap.getObjectId();
        }
        return e;
    } */

    //TODO[K] - Guard section start
    @Override
    public void setStrixClientData(final StrixClientData clientData) {
        this.clientData = clientData;
    }

    @Override
    public StrixClientData getStrixClientData() {
        return clientData;
    }
    //TODO[K] - Guard section end

    @Override
    public String toString() {
        return _state + " IP: " + getIpAddr() + (_login == null ? "" : " Account: " + _login) + (_activeChar == null ? "" : " Player : " + _activeChar);
    }
}
