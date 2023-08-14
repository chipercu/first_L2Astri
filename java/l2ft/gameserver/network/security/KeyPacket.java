package l2ft.gameserver.network.security;

import l2ft.gameserver.network.l2.s2c.L2GameServerPacket;

public final class KeyPacket extends L2GameServerPacket
{
    private final byte[] key;


    public KeyPacket(byte data[])
    {
        this.key = data;
    }


    public void writeImpl()
    {
        writeC(0x2e);
        if(key == null || key.length == 0)
        {
            writeC(0x00);
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
