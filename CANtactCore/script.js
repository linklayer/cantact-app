DeviceManager = Java.type('org.cantact.core.DeviceManager');
CanFrame = Java.type('org.cantact.core.CanFrame');
IsotpInterface = Java.type('org.cantact.proto.IsotpInterface');
IsotpCallback = Java.type('org.cantact.proto.IsotpInterface.IsotpCallback');
print(CanFrame);

cb = new IsotpCallback(function(data){print(data[1]);});

itp2 = new IsotpInterface(2, 1, cb);
var canReceived = function(f) {
    if (itp2.isDataReady()) {
	print(itp2.getReceivedData())
    }

}
itp = new IsotpInterface(1, 2, cb);
itp.send([1,2,3])
print('test from js!');
