package l2ft.gameserver.network.security;

import l2ft.gameserver.network.l2.c2s.L2GameClientPacket;
import l2ft.gameserver.network.l2.s2c.SendStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.strixplatform.StrixPlatform;
import org.strixplatform.managers.ClientGameSessionManager;
import org.strixplatform.managers.ClientProtocolDataManager;
import org.strixplatform.utils.StrixClientData;

public class ProtocolVersion extends L2GameClientPacket
{
    private static final Logger _log = LoggerFactory.getLogger(ProtocolVersion.class);
    private int version;

    //TODO[K] - Guard section start
    private byte[] data;
    private int dataChecksum;
    //TODO[K] - Guard section end

    public ProtocolVersion()
    {}

    protected void readImpl()
    {
        version = readD();
        //TODO[K] - Guard section start
        if(StrixPlatform.getInstance().isPlatformEnabled())
        {
            try
            {
                if(_buf.remaining() >= StrixPlatform.getInstance().getProtocolVersionDataSize())
                {
                    data = new byte[StrixPlatform.getInstance().getClientDataSize()];
                    readB(data);
                    dataChecksum = readD();
                }
            }
            catch(final Exception e)
            {
                _log.error("Client [IP=" + getClient().getIpAddr() + "] used unprotected client. Disconnect...");
                getClient().close(new KeyPacket(null));
                return;
            }
        }
        //TODO[K] - Guard section end
    }

    protected void runImpl()
    {
        if (version == -2)
        {
            getClient().closeNow(false);
            return;
        }
        else if (version == -3)
        {
            getClient().close(new SendStatus());
            return;
        }
        else if(version < 267 || version > 280)
        {
            _log.warn("Unknown protocol revision : " + version + ", client : " + getClient());
            getClient().close(new KeyPacket(null));
            return;
        }

        //TODO[K] - Strix section start
        if(!StrixPlatform.getInstance().isPlatformEnabled())
        {
            getClient().setRevision(version);
            sendPacket(new KeyPacket(getClient().enableCrypt()));
            return;
        }
        else
        {
            if(data == null)
            {
                _log.error("Client [IP=" + getClient().getIpAddr() + "] used unprotected client. Disconnect...");
                getClient().close(new KeyPacket(null));
                return;
            }
            else
            {
                final StrixClientData clientData = ClientProtocolDataManager.getInstance().getDecodedData(data, dataChecksum);
                if(clientData != null)
                {
                    if(!ClientGameSessionManager.getInstance().checkServerResponse(clientData))
                    {
                        getClient().close(new KeyPacket(null, clientData));
                        return;
                    }
                    getClient().setStrixClientData(clientData);
                    getClient().setRevision(version);
                    sendPacket(new KeyPacket(getClient().enableCrypt()));
                    return;
                }
                _log.error("Decode client data failed. See Strix-Platform log file. Disconected client " + getClient().getIpAddr());
                getClient().close(new KeyPacket(null));
            }
        }
        //TODO[K] - Strix section end
    }

    public String getType()
    {
        return getClass().getSimpleName();
    }
}
