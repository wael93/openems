package io.openems.impl.device.socomec;

import io.openems.api.channel.Channel;
import io.openems.api.device.nature.Meter;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusChannel;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.internal.ElementBuilder;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.ModbusRange;
import io.openems.impl.protocol.modbus.internal.channel.ModbusChannelBuilder;

public class SocomecMeter extends ModbusDeviceNature implements Meter {

	private final ModbusChannel _activeNegativeEnergy = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _activePositiveEnergy = new ModbusChannelBuilder().nature(this).unit("kWh").build();
	private final ModbusChannel _activePower = new ModbusChannelBuilder().nature(this).unit("W").multiplier(10).build();
	private final ModbusChannel _apparentEnergy = new ModbusChannelBuilder().nature(this).unit("kVAh").build();
	private final ModbusChannel _apparentPower = new ModbusChannelBuilder().nature(this).unit("VA").multiplier(10)
			.build();
	private final ModbusChannel _reactiveNegativeEnergy = new ModbusChannelBuilder().nature(this).unit("kvarh").build();
	private final ModbusChannel _reactivePositiveEnergy = new ModbusChannelBuilder().nature(this).unit("kvarh").build();
	private final ModbusChannel _reactivePower = new ModbusChannelBuilder().nature(this).unit("var").multiplier(10)
			.build();

	public SocomecMeter(String thingId) {
		super(thingId);
	}

	@Override
	public Channel activeNegativeEnergy() {
		return _activeNegativeEnergy;
	}

	@Override
	public Channel activePositiveEnergy() {
		return _activePositiveEnergy;
	}

	@Override
	public Channel activePower() {
		return _activePower;
	}

	@Override
	public Channel apparentEnergy() {
		return _apparentEnergy;
	}

	@Override
	public Channel apparentPower() {
		return _apparentPower;
	}

	@Override
	public Channel reactiveNegativeEnergy() {
		return _reactiveNegativeEnergy;
	}

	@Override
	public Channel reactivePositiveEnergy() {
		return _reactivePositiveEnergy;
	}

	@Override
	public Channel reactivePower() {
		return _reactivePower;
	}

	@SuppressWarnings("null")
	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		return new ModbusProtocol( //
				new ModbusRange(0xc568, //
						new ElementBuilder().address(0xc568).channel(_activePower).doubleword().signed().build(), //
						new ElementBuilder().address(0xc56A).channel(_reactivePower).doubleword().signed().build(), //
						new ElementBuilder().address(0xc56C).channel(_apparentPower).doubleword().build()), //
				new ModbusRange(0xc652, //
						new ElementBuilder().address(0xc652).channel(_activePositiveEnergy).doubleword().build(),
						new ElementBuilder().address(0xc654).channel(_reactivePositiveEnergy).doubleword().build(),
						new ElementBuilder().address(0xc656).channel(_apparentEnergy).doubleword().signed().build(),
						new ElementBuilder().address(0xc658).channel(_activeNegativeEnergy).doubleword().build(),
						new ElementBuilder().address(0xc65a).channel(_reactiveNegativeEnergy).doubleword().build()));
	}

}
