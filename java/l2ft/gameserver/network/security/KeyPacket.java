package l2ft.gameserver.network.security;

import Strix_decopile.StrixPlatform;
import Strix_decopile.Utils.StrixClientData;
import l2ft.gameserver.network.l2.s2c.L2GameServerPacket;

public final class KeyPacket extends L2GameServerPacket
{
    private final byte[] key;

    //TODO[K] - Guard section start
    private final StrixClientData clientData;
    // TODO[K] - Strix section end

    public KeyPacket(byte data[])
    {
        this.key = data;
        //TODO[K] - Guard section start
        this.clientData = null;
        // TODO[K] - Strix section end
    }

    //TODO[K] - Guard section start
    public KeyPacket(final byte[] key, final StrixClientData clientData)
    {
        this.key = key;
        this.clientData = clientData;
    }
    // TODO[K] - Strix section end

    public void writeImpl()
    {
        writeC(0x2e);
        if(key == null || key.length == 0)
        {
            writeC(0x00);
            //TODO[K] - Guard section start
            if(StrixPlatform.getInstance().isBackNotificationEnabled() && clientData != null)
            {
                writeC(clientData.getServerResponse().ordinal());
                //body.writeQ(); Resolved to send ban time expire.
            }
            // TODO[K] - Strix section end
            return;
        }
        writeC(0x01);
        writeB(key);
        writeD(0x01);
        writeD(0x00);
        writeC(0x00);
        writeD(0x00); // Seed (obfuscation key)
        //body.writeC(0x00);    // 1 - Classic client, 0 - Other(old and new) client
        //body.writeC(0x00);    // 1 - Arena(if classic active)
    }

    public String getType()
    {
        return getClass().getSimpleName();
    }
}
