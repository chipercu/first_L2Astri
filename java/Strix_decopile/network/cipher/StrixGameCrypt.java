package Strix_decopile.network.cipher;

import Strix_decopile.configs.MainConfig;
import Strix_decopile.logging.StrixLog;

/**
 * Created by a.kiperku
 * Date: 14.08.2023
 */



public class StrixGameCrypt {
    private final byte[] inKey = new byte[16];
    private final byte[] outKey = new byte[16];
    private boolean isEnabled = false;
    private final GuardCipher cryptIn = new GuardCipher();
    private final GuardCipher cryptOut = new GuardCipher();

    public StrixGameCrypt() {
    }

    public void setKey(byte[] key) {
        System.arraycopy(key, 0, this.inKey, 0, 16);
        System.arraycopy(key, 0, this.outKey, 0, 16);
        if (MainConfig.STRIX_PLATFORM_ENABLED) {
            this.cryptIn.setKey(key);
            this.cryptOut.setKey(key);
        }

    }

    public boolean decrypt(byte[] raw, int offset, int size) {
        if (!this.isEnabled) {
            return true;
        } else {
            int i;
            if (MainConfig.STRIX_PLATFORM_ENABLED) {
                if (this.cryptIn.keySeted) {
                    this.cryptIn.chiper(raw, offset, size);
                    return true;
                } else {
                    StrixLog.audit("Key not setted. Nulled received packet. Maybe used network hook.");

                    for(i = 0; i < size; ++i) {
                        raw[offset + i] = 0;
                    }

                    return false;
                }
            } else {
                i = 0;

                int old;
                for(old = 0; old < size; ++old) {
                    int temp2 = raw[offset + old] & 255;
                    raw[offset + old] = (byte)(temp2 ^ this.inKey[old & 15] ^ i);
                    i = temp2;
                }

                old = this.inKey[8] & 255;
                old |= this.inKey[9] << 8 & '\uff00';
                old |= this.inKey[10] << 16 & 16711680;
                old |= this.inKey[11] << 24 & -16777216;
                old += size;
                this.inKey[8] = (byte)(old & 255);
                this.inKey[9] = (byte)(old >> 8 & 255);
                this.inKey[10] = (byte)(old >> 16 & 255);
                this.inKey[11] = (byte)(old >> 24 & 255);
                return true;
            }
        }
    }

    public boolean encrypt(byte[] raw, int offset, int size) {
        if (!this.isEnabled) {
            this.isEnabled = true;
            return true;
        } else {
            int i;
            if (MainConfig.STRIX_PLATFORM_ENABLED) {
                if (this.cryptOut.keySeted) {
                    this.cryptOut.chiper(raw, offset, size);
                    return true;
                } else {
                    StrixLog.audit("Key not setted. Nulled send packet. Maybe used network hook.");

                    for(i = 0; i < size; ++i) {
                        raw[offset + i] = 0;
                    }

                    return false;
                }
            } else {
                i = 0;

                int old;
                for(old = 0; old < size; ++old) {
                    int temp2 = raw[offset + old] & 255;
                    i ^= temp2 ^ this.outKey[old & 15];
                    raw[offset + old] = (byte)i;
                }

                old = this.outKey[8] & 255;
                old |= this.outKey[9] << 8 & '\uff00';
                old |= this.outKey[10] << 16 & 16711680;
                old |= this.outKey[11] << 24 & -16777216;
                old += size;
                this.outKey[8] = (byte)(old & 255);
                this.outKey[9] = (byte)(old >> 8 & 255);
                this.outKey[10] = (byte)(old >> 16 & 255);
                this.outKey[11] = (byte)(old >> 24 & 255);
                return true;
            }
        }
    }
}

