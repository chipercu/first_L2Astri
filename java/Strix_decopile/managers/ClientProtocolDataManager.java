package Strix_decopile.managers;

import Strix_decopile.Utils.DataUtils;
import Strix_decopile.Utils.ServerResponse;
import Strix_decopile.Utils.StrixClientData;
import Strix_decopile.configs.MainConfig;
import Strix_decopile.logging.StrixLog;
import Strix_decopile.network.ReadDataBuffer;

/**
 * Created by a.kiperku
 * Date: 14.08.2023
 */


public class ClientProtocolDataManager {


    public ClientProtocolDataManager() {
    }

    public static ClientProtocolDataManager getInstance() {
        return ClientProtocolDataManager.LazyHolder.INSTANCE;
    }

    public StrixClientData getDecodedData(byte[] dataArray, int clientDataChecksum) {
        try {
            if (dataArray != null && dataArray.length >= 260) {
                StrixClientData clientData = new StrixClientData();
                DataUtils.getDecodedDataFromKey(dataArray, DataUtils.getRealDataChecksum(clientDataChecksum));
                int decodedDataChecksum = DataUtils.getDataChecksum(dataArray, false);
                if (decodedDataChecksum != DataUtils.getRealDataChecksum(clientDataChecksum)) {
                    StrixLog.error("Received client data not valide. Client checksum: " + DataUtils.getRealDataChecksum(clientDataChecksum) + " Decoded checksum: " + decodedDataChecksum);
                    clientData.setServerResponse(ServerResponse.RESPONSE_FAILED_CLIENT_DATA_CHECKSUM_CHECK);
                    return clientData;
                } else {
                    if (MainConfig.STRIX_PLATFORM_DEBUG_ENABLED) {
                        String data = "";

                        for(int i = 0; i < 192; ++i) {
                            data = data + (char)dataArray[i];
                        }

                        StrixLog.debug("ClientProtocolDataManager: first 192 byte " + data);
                    }

                    ReadDataBuffer dataBuffer = new ReadDataBuffer(dataArray);
                    clientData.setClientHWID(dataBuffer.ReadS());
                    clientData.setVMPKey(dataBuffer.ReadS());
                    clientData.setHWIDChecksum(dataBuffer.ReadQ());
                    clientData.setDetectionResponse(dataBuffer.ReadQ());
                    clientData.setLaunchStateResponse(dataBuffer.ReadQ());
                    clientData.setSessionId(dataBuffer.ReadQ());
                    clientData.setFilesChecksum(dataBuffer.ReadQ());
                    clientData.setClientSideVersion(dataBuffer.ReadH());
                    clientData.setActiveWindowCount(dataBuffer.ReadH());
                    ClientGameSessionManager.getInstance().checkClientData(clientData);
                    return clientData;
                }
            } else {
                StrixLog.error("Received client data nulled or not use Strix-Platform modules(Clear pacth or Strix-Platform not loaded)");
                return null;
            }
        } catch (Exception var7) {
            StrixLog.error("Cannot decode Strix data from client. Please send this error and all needed info to Strix-Platform support! Exception: " + var7.getLocalizedMessage());
            return null;
        }
    }

    private static class LazyHolder {
        private static final ClientProtocolDataManager INSTANCE = new ClientProtocolDataManager();

        private LazyHolder() {
        }
    }
}