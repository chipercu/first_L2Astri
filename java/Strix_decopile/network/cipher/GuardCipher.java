package Strix_decopile.network.cipher;

/**
 * Created by a.kiperku
 * Date: 14.08.2023
 */


import Strix_decopile.configs.MainConfig;

import java.nio.ByteBuffer;

public class GuardCipher {
    private byte[] state = new byte[256];
    private int x;
    private int y;
    public boolean keySeted = false;

    public GuardCipher() {
    }

    public void setKey(byte[] key) throws NullPointerException {
        int index1;
        for(index1 = 0; index1 < 256; ++index1) {
            this.state[index1] = (byte)index1;
        }

        this.x = 0;
        this.y = 0;
        index1 = 0;
        int index2 = 0;
        if (key != null && key.length != 0) {
            byte[] keyInit = (byte[])key.clone();
            if (MainConfig.STX_PF_XOR_KEY != null && MainConfig.STX_PF_XOR_KEY.length() > 0) {
                byte[] xorKey = MainConfig.STX_PF_XOR_KEY.getBytes();

                for(int i = 0; i < 8; ++i) {
                    keyInit[i] ^= xorKey[i % 4];
                }
            }

            for(int i = 0; i < 256; ++i) {
                index2 = (keyInit[index1] & 255) + (this.state[i] & 255) + index2 & 255;
                byte tmp = this.state[i];
                this.state[i] = this.state[index2];
                this.state[index2] = tmp;
                index1 = (index1 + 1) % keyInit.length;
            }

            this.keySeted = true;
        } else {
            throw new NullPointerException();
        }
    }

    public void chiper(byte[] buf, int offset, int size) {
        for(int i = 0; i < size; ++i) {
            this.x = this.x + 1 & 255;
            this.y = (this.state[this.x] & 255) + this.y & 255;
            byte tmp = this.state[this.x];
            this.state[this.x] = this.state[this.y];
            this.state[this.y] = tmp;
            int xorIndex = (this.state[this.x] & 255) + (this.state[this.y] & 255) & 255;
            buf[offset + i] ^= this.state[xorIndex];
        }

    }

    public void chiper(ByteBuffer buffer, int offset, int size) {
        for(int i = 0; i < size; ++i) {
            this.x = this.x + 1 & 255;
            this.y = (this.state[this.x] & 255) + this.y & 255;
            byte tmp = this.state[this.x];
            this.state[this.x] = this.state[this.y];
            this.state[this.y] = tmp;
            int xorIndex = (this.state[this.x] & 255) + (this.state[this.y] & 255) & 255;
            tmp = (byte)(buffer.get(offset + i) ^ this.state[xorIndex]);
            buffer.put(offset + i, tmp);
        }

    }
}
