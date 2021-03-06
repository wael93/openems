package io.openems.edge.ess.mr.gridcon.enums;

import io.openems.edge.ess.mr.gridcon.GridconPCS;

public enum InverterCount {
	ONE(1), //
	TWO(2), //
	THREE(3);

	private final int count;
	private final int maxApparentPower;

	private InverterCount(int count) {
		this.count = count;
		this.maxApparentPower = count * GridconPCS.MAX_POWER_PER_INVERTER;
	}

	public int getCount() {
		return this.count;
	}

	public int getMaxApparentPower() {
		return maxApparentPower;
	}
}
