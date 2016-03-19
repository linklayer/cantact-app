package org.cantact.proto;

import org.openide.util.NotImplementedException;

public class UdsClient {

    IsotpInterface isotpInterface;
    IsotpReceiveHandler isotpHandler;

    public UdsClient(int txId, int rxId) {
        isotpHandler = new IsotpReceiveHandler();
        isotpInterface = new IsotpInterface(txId, rxId, isotpHandler);
    }

    private int byteSize(int x) {
        if (x < 0) {
            throw new IllegalArgumentException();
        }
        int s = 1;
        while (s < 8 && x >= (1L << (s * 8))) {
            s++;
        }
        return s;
    }

    public void ObdRequest(int mode, int pid) {
        int[] data = new int[2];
        data[0] = mode;
        data[1] = pid;
        isotpInterface.send(data);
    }

    public void DiagnosticSessionControl(int diagnosticSessionType) {
        int[] data = new int[2];
        data[0] = UdsServices.DIAGNOSTIC_SESSION_CONTROL.serviceId();
        data[1] = diagnosticSessionType;
        isotpInterface.send(data);
    }

    public void EcuReset(int resetType) {
        int[] data = new int[2];
        data[0] = UdsServices.ECU_RESET.serviceId();
        data[1] = resetType;
        isotpInterface.send(data);
    }

    public void SecurityAccess(int securityAccessType,
            int[] securityAccessDataRecord) {
        int[] data = new int[2 + securityAccessDataRecord.length];
        data[0] = UdsServices.SECURITY_ACCESS.serviceId();
        data[1] = securityAccessType;
        System.arraycopy(securityAccessDataRecord, 0, data, 2,
                securityAccessDataRecord.length);
        isotpInterface.send(data);
    }

    public void CommunicationControl(int controlType, int communicationType) {
        int[] data = new int[3];
        data[0] = UdsServices.COMMUNICATION_CONTROL.serviceId();
        data[1] = controlType;
        data[2] = communicationType;
        isotpInterface.send(data);
    }

    public void TesterPresent() {
        int[] data = new int[2];
        data[0] = UdsServices.TESTER_PRESENT.serviceId();
        data[1] = 0;
        isotpInterface.send(data);
    }

    public void AccessTimingParameter(int timingParameterAccessType,
            int[] timingParameterRequestRecord) {
        int[] data = new int[2 + timingParameterRequestRecord.length];
        data[0] = UdsServices.ACCESS_TIMING_PARAMETER.serviceId();
        data[1] = timingParameterAccessType;
        System.arraycopy(timingParameterRequestRecord, 0, data, 2,
                timingParameterRequestRecord.length);
        isotpInterface.send(data);
    }

    public void SecuredDataTransmission() {
        throw new NotImplementedException();
    }

    public void ControlDtcSetting(int dtcSettingType,
            int[] dtcSettingControlOptionRecord) {
        int[] data = new int[2 + dtcSettingControlOptionRecord.length];
        data[0] = UdsServices.CONTROL_DTC_SETTING.serviceId();
        data[1] = dtcSettingType;
        System.arraycopy(dtcSettingControlOptionRecord, 0, data, 2,
                dtcSettingControlOptionRecord.length);
        isotpInterface.send(data);
    }

    public void ResponseOnEvent() {
        throw new NotImplementedException();
    }

    public void LinkControl(int linkControlType, int baudrateIdentifier) {
        int[] data = new int[3];
        data[0] = UdsServices.LINK_CONTROL.serviceId();
        data[1] = linkControlType;
        data[2] = baudrateIdentifier;
        isotpInterface.send(data);
    }

    public void ReadDataByIdentifier(int dataId) {
        int[] data = new int[3];
        data[0] = UdsServices.READ_DATA_BY_IDENTIFIER.serviceId();
        // byte 1 is upper nybble of dataId
        data[1] = dataId >> 8;
        // byte 2 is lower nybble of dataId
        data[2] = dataId & 0xFF;
        isotpInterface.send(data);
    }

    public void ReadMemoryByAddress(int memoryAddress, int memorySize) {
        int addrBytes = byteSize(memoryAddress);
        int sizeBytes = byteSize(memorySize);
        if (addrBytes > 0xF || sizeBytes > 0xF) {
            throw new IllegalArgumentException();
        }

        // 2 bytes are fixed, then add space for parameters
        int[] data = new int[2 + addrBytes + sizeBytes];
        data[0] = UdsServices.READ_MEMORY_BY_ADDRESS.serviceId();
        // upper nybble is memorySize size, lower nybble is memoryAddress size
        data[1] = (sizeBytes << 4) | addrBytes;
        // TODO: numbers to byte arrays
    }

    public void ReadScalingDataByIdentifier(int dataId) {
        int[] data = new int[3];
        data[0] = UdsServices.READ_SCALING_DATA_BY_IDENTIFIER.serviceId();
        // byte 1 is upper nybble of dataId
        data[1] = dataId >> 8;
        // byte 2 is lower nybble of dataId
        data[2] = dataId & 0xFF;
        isotpInterface.send(data);
    }

    public void ReadDataByPeriodicIdentifier() {
        throw new NotImplementedException();
    }

    public void DynamicallyDefineDataIdentifier() {
        throw new NotImplementedException();
    }

    public void WriteDataByIdentifier(int dataId, int[] dataRecord) {
        int[] data = new int[3 + dataRecord.length];
        data[0] = UdsServices.WRITE_DATA_BY_IDENTIFIER.serviceId();
        // byte 1 is upper nybble of dataId
        data[1] = dataId >> 8;
        // byte 2 is lower nybble of dataId
        data[2] = dataId & 0xFF;
        // remainder is data record
        System.arraycopy(dataRecord, 0, data, 3,
                dataRecord.length);
        isotpInterface.send(data);
    }

    public void WriteMemoryByAddress() {
        throw new NotImplementedException();
    }

    public void ClearDiagnosticInformation(int groupOfDtc) {
        int[] data = new int[4];
        data[0] = UdsServices.CLEAR_DIAGNOSTIC_INFORMATION.serviceId();
        // groupOfDtc is always 3 bytes
        data[1] = (groupOfDtc >> 16) & 0xFF;
        data[2] = (groupOfDtc >> 8) & 0xFF;
        data[3] = groupOfDtc & 0xFF;
        isotpInterface.send(data);
    }

    public void ReadDtcInformation() {
        throw new NotImplementedException();
    }

    public void InputOutputControlByIdentifier() {
        throw new NotImplementedException();
    }

    public void RoutineControl(int routineControlType, int routineId,
            int[] routineControlOptionRecord) {
        int[] data = new int[4 + routineControlOptionRecord.length];
        data[0] = UdsServices.ROUTINE_CONTROL.serviceId();
        data[1] = routineControlType;
        // byte 2 is upper nybble of routineId
        data[2] = routineId >> 8;
        // byte 3 is lower nybble of routineId
        data[3] = routineId & 0xFF;
        // remainder is data record     
        System.arraycopy(routineControlOptionRecord, 0, data, 4,
                routineControlOptionRecord.length);
        isotpInterface.send(data);
    }

    public void RequestDownload() {
        throw new NotImplementedException();
    }

    public void RequestUpload() {
        throw new NotImplementedException();
    }

    public void TransferData() {
        throw new NotImplementedException();
    }

    public void RequestTransferExit() {
        throw new NotImplementedException();
    }

    private class IsotpReceiveHandler implements IsotpInterface.IsotpCallback {

        @Override
        public void onIsotpReceived(int[] data) {
            int serviceId = data[0];
            if (serviceId == UdsServices.READ_DATA_BY_IDENTIFIER.serviceId()) {

            }
        }
    }

    public void setTxId(int id) {
        isotpInterface.setTxId(id);
    }

    public void setRxId(int id) {
        isotpInterface.setRxId(id);
    }

    public enum ObdModes {
        RESERVED_0,
        SHOW_CURRENT_DATA,
        SHOW_FREEZE_FRAME_DATA,
        SHOW_DTCS,
        CLEAR_DTCS,
        RESERVED_5,
        TEST_RESULTS,
        SHOW_PENDING_DTCS,
        CONTROL_OPERATION,
        VEHICLE_INFO,
        PERMANENT_DTCS
    }
    
    public enum UdsServices {

        DIAGNOSTIC_SESSION_CONTROL(0x10),
        ECU_RESET(0x11),
        SECURITY_ACCESS(0x27),
        COMMUNICATION_CONTROL(0x28),
        TESTER_PRESENT(0x3E),
        ACCESS_TIMING_PARAMETER(0x83),
        SECURED_DATA_TRANSMISSION(0x84),
        CONTROL_DTC_SETTING(0x85),
        RESPONSE_ON_EVENT(0x86),
        LINK_CONTROL(0x87),
        READ_DATA_BY_IDENTIFIER(0x22),
        READ_MEMORY_BY_ADDRESS(0x23),
        READ_SCALING_DATA_BY_IDENTIFIER(0x24),
        READ_DATA_BY_PERIODIC_IDENTIFIER(0x2A),
        DYNAMICALLY_DEFINE_DATA_IDENTIFIER(0x2C),
        WRITE_DATA_BY_IDENTIFIER(0x2E),
        WRITE_MEMORY_BY_ADDRESS(0x3D),
        CLEAR_DIAGNOSTIC_INFORMATION(0x14),
        READ_DTC_INFORMATION(0x19),
        INPUT_OUTPUT_CONTROL_BY_IDENTIFIER(0x2F),
        ROUTINE_CONTROL(0x31),
        REQUEST_DOWNLOAD(0x34),
        REQUEST_UPLOAD(0x35),
        TRANSFER_DATA(0x36),
        REQUEST_TRANSFER_EXIT(0x37);

        private final int serviceId;

        UdsServices(int serviceId) {
            this.serviceId = serviceId;
        }

        int serviceId() {
            return serviceId;
        }
    }
}
