# dummy_modbus_server.py
from pymodbus.server.sync import StartTcpServer
from pymodbus.device import ModbusDeviceIdentification
from pymodbus.datastore import ModbusSlaveContext, ModbusServerContext
from pymodbus.datastore.store import ModbusSequentialDataBlock

# Create a simple data store with default values
store = ModbusSlaveContext(
    di=ModbusSequentialDataBlock(0, [17]*100),
    co=ModbusSequentialDataBlock(0, [17]*100),
    hr=ModbusSequentialDataBlock(0, [17]*100),
    ir=ModbusSequentialDataBlock(0, [17]*100)
)
context = ModbusServerContext(slaves=store, single=True)

# Optional device identification
identity = ModbusDeviceIdentification()
identity.VendorName = 'MyCompany'
identity.ProductCode = 'DM'
identity.VendorUrl = 'http://example.com'
identity.ProductName = 'Dummy Modbus Server'
identity.ModelName = 'Dummy'
identity.MajorMinorRevision = '1.0'

if __name__ == "__main__":
    # Listen on all interfaces (0.0.0.0) and default Modbus TCP port (502)
    StartTcpServer(context, identity=identity, address=("0.0.0.0", 502))
