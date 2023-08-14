package Strix_decopile.network;

/**
 * Created by a.kiperku
 * Date: 14.08.2023
 */


import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ReadDataBuffer {
    private final byte[] dataArray;
    private final int dataLenght;
    private int dataBufferPosition;

    public ReadDataBuffer(byte[] dataArray) {
        this.dataArray = dataArray;
        this.dataLenght = dataArray.length;
        this.dataBufferPosition = 0;
    }

    public char ReadC() {
        if (this.dataBufferPosition + 1 > this.dataLenght) {
            return '\u0000';
        } else {
            ByteBuffer bb = ByteBuffer.wrap(this.dataArray, this.dataBufferPosition, 1);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            char value = bb.getChar();
            ++this.dataBufferPosition;
            return value;
        }
    }

    public short ReadH() {
        if (this.dataBufferPosition + 2 > this.dataLenght) {
            return 0;
        } else {
            ByteBuffer bb = ByteBuffer.wrap(this.dataArray, this.dataBufferPosition, 2);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            short value = bb.getShort();
            this.dataBufferPosition += 2;
            return value;
        }
    }

    public int ReadD() {
        if (this.dataBufferPosition + 4 > this.dataLenght) {
            return 0;
        } else {
            ByteBuffer bb = ByteBuffer.wrap(this.dataArray, this.dataBufferPosition, 4);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            int value = bb.getInt();
            this.dataBufferPosition += 4;
            return value;
        }
    }

    public long ReadQ() {
        if (this.dataBufferPosition + 8 > this.dataLenght) {
            return 0L;
        } else {
            ByteBuffer bb = ByteBuffer.wrap(this.dataArray, this.dataBufferPosition, 8);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            long value = bb.getLong();
            this.dataBufferPosition += 8;
            return value;
        }
    }

    public String ReadS() {
        StringBuilder str;
        for(str = new StringBuilder(); (char)this.dataArray[this.dataBufferPosition] != 0; this.dataBufferPosition += 2) {
            str.append((char)this.dataArray[this.dataBufferPosition]);
        }

        this.dataBufferPosition += 2;
        return str.toString();
    }
}
