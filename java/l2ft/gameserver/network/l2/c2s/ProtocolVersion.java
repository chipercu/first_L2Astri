package l2ft.gameserver.network.l2.c2s;

import java.io.IOException;

import l2ft.gameserver.Config;
import l2ft.gameserver.network.l2.s2c.KeyPacket;
import l2ft.gameserver.network.l2.s2c.SendStatus;

import l2ft.gameserver.network.l2.s2c.VersionCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.strixplatform.StrixPlatform;
import org.strixplatform.logging.Log;
import org.strixplatform.managers.ClientGameSessionManager;
import org.strixplatform.managers.ClientProtocolDataManager;
import org.strixplatform.utils.StrixClientData;

public class ProtocolVersion extends L2GameClientPacket {
    private static final Logger _log = LoggerFactory.getLogger(ProtocolVersion.class);

    //TODO[K] - Guard section start
    private byte[] data;
    private int dataChecksum;
    //TODO[K] - Guard section end

    private int _version;

    protected void readImpl() {
        _version = readD();
        //TODO[K] - Guard section start
        if (StrixPlatform.getInstance().isPlatformEnabled()) {
            try {
                if (getByteBuffer().remaining() >= StrixPlatform.getInstance().getProtocolVersionDataSize()) {
                    data = new byte[StrixPlatform.getInstance().getClientDataSize()];
                    readB(data);
                    dataChecksum = readD();
                }
            } catch (final Exception e) {
                Log.error("Client [IP=" + getClient().getIpAddr() + "] used unprotected client. Disconnect...");
                getClient().close(new VersionCheck(null));
                return;
            }
        }
        //TODO[K] - Guard section end
    }

    protected void runImpl() throws IOException {
        if (_version == -2) {
            _client.closeNow(false);
            return;
        } else if (_version == -3) {
            _log.info("Status request from IP : " + getClient().getIpAddr());
            getClient().close(new SendStatus());
            return;
        } else if (_version < Config.MIN_PROTOCOL_REVISION || _version > Config.MAX_PROTOCOL_REVISION) {
            _log.warn("Unknown protocol revision : " + _version + ", client : " + _client);
            getClient().close(new KeyPacket(null));
            return;
        }

        //TODO - Strix section start
        if (!StrixPlatform.getInstance().isPlatformEnabled()) {
            getClient().setRevision(_version);
            sendPacket(new VersionCheck(getClient().enableCrypt()));
            return;
        } else {
            if (data == null) {
                Log.error("Client [IP=" + getClient().getIpAddr() + "] used unprotected client. Disconnect...");
                getClient().close(new VersionCheck(null));
                return;
            } else {
                final StrixClientData clientData = ClientProtocolDataManager.getInstance().getDecodedData(data, dataChecksum);
                if (clientData != null) {
                    if (!ClientGameSessionManager.getInstance().checkServerResponse(clientData)) {
                        getClient().close(new VersionCheck(null, clientData));
                        return;
                    }
                    getClient().setStrixClientData(clientData);
                    getClient().setRevision(_version);
                    sendPacket(new VersionCheck(getClient().enableCrypt()));
                    return;
                }
                Log.error("Decode client data failed. See Strix-Platform log file. Disconected client " + getClient().getIpAddr());
                getClient().close(new VersionCheck(null));
            }
        }
        //TODO[K] - Strix section end

//        getClient().setRevision(_version);
//        sendPacket(new KeyPacket(_client.enableCrypt()));
    }

    public String getType() {
        return getClass().getSimpleName();
    }
}