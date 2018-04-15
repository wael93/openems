package io.openems.edge.controller.debuglog;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.fenecon.openemsv1.OpenemsV1;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.DebugLog", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
public class DebugLog extends AbstractOpenemsComponent implements Controller {

	private final Logger log = LoggerFactory.getLogger(DebugLog.class);

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	private volatile List<OpenemsComponent> components = new CopyOnWriteArrayList<>();

	@Activate
	void activate(Config config) {
		super.activate(config.id(), config.enabled());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private int lastSetValue = 0;

	@Override
	public void run() {
		StringBuilder b = new StringBuilder();
		/*
		 * Asks each component for its debugLog()-ChannelIds. Prints an aggregated log
		 * of those channelIds and their current values.
		 */
		this.components.forEach(component -> {
			if (!component.isActive()) {
				System.out.println(component.id() + " is inactive");
				return;
			}
			b.append(component.id());
			String debugLog = component.debugLog();
			if (debugLog != null) {
				b.append("[" + debugLog + "]");
			}
			b.append(" ");

			if (component.id().equals("test0")) {
				Channel<?> channel = component.channel(OpenemsV1.ChannelId.SET_MIN_SOC);
				WriteChannel<?> writeChannel = (WriteChannel<?>) channel;
				try {
					writeChannel.setNextWriteValue(Long.valueOf(++this.lastSetValue));
				} catch (OpenemsException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				System.out.println(channel);
			}
		});
		log.info(b.toString());
	}
}