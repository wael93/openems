package io.openems.core.controller;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;
import io.openems.api.channel.IsChannel;
import io.openems.api.channel.IsRequired;
import io.openems.api.controller.Controller;
import io.openems.api.controller.ControllerThingMapping;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.IsThingMapping;
import io.openems.api.controller.ThingMap;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InjectionException;
import io.openems.api.thing.Thing;
import io.openems.core.databus.Databus;
import io.openems.core.thing.ThingFactory;
import io.openems.core.utilities.InjectionUtils;

public class ControllerFactory {
	private static Logger log = LoggerFactory.getLogger(ControllerFactory.class);

	@SuppressWarnings("unchecked")
	public static List<ControllerThingMapping> generateMappings(Controller controller, Databus databus)
			throws InjectionException, ConfigException {
		List<ControllerThingMapping> result = new LinkedList<>();

		/*
		 * Search Fields with @IsThingMapping Annotation
		 */
		for (Field controllerField : controller.getClass().getDeclaredFields()) {
			if (controllerField.isAnnotationPresent(IsThingMapping.class)) {
				@SuppressWarnings("null")
				IsThingMapping isThingMapping = controllerField.getAnnotation(IsThingMapping.class);
				// marker to tell if only one mapped Thing or a list of Things is expected
				boolean isListExpected = false;
				/*
				 * Get the ThingMap class
				 */
				Class<? extends ThingMap> thingMapClass;
				try {
					thingMapClass = (Class<? extends ThingMap>) controllerField.getType();
					if (List.class.isAssignableFrom(thingMapClass)) {
						// Field is a list. Get the generic type
						ParameterizedType type = (ParameterizedType) controllerField.getGenericType();
						thingMapClass = (Class<? extends ThingMap>) Class
								.forName(type.getActualTypeArguments()[0].getTypeName());
						isListExpected = true;
					}
				} catch (ClassNotFoundException e) {
					throw new InjectionException("Unable to find ThingMap [" + isThingMapping + "].");
				}
				/*
				 * Get the referenced Thing class
				 */
				if (!thingMapClass.isAnnotationPresent(IsThingMap.class)) {
					throw new InjectionException("ThingMap [" + controller.getClass().getSimpleName()
							+ "] has no defined target Thing! 'IsThingMap'-annotation is missing.");
				}
				@SuppressWarnings("null")
				IsThingMap isThingMap = thingMapClass.getAnnotation(IsThingMap.class);
				Class<? extends Thing> thingClass = isThingMap.type();
				/*
				 * Get Channel-Methods of referenced Thing class
				 * channelId -> Getter-Method for Channel in Thing
				 */
				Map<String, Method> channelMethods = new HashMap<>();
				for (Method method : thingClass.getDeclaredMethods()) {
					// get all methods of this class
					if (Channel.class.isAssignableFrom(method.getReturnType())) {
						// method returns a Channel; now check for the annotation
						IsChannel annotation = InjectionUtils.getIsChannelMethods(thingClass, method.getName());
						if (annotation != null) {
							channelMethods.put(annotation.id(), method);
						}
					}
				}
				/*
				 * Create ThingMap instance(s) for each matching Thing
				 */
				Map<String, Thing> matchingThings = ThingFactory.getThingsByClass(databus, thingClass);
				Map<String, ThingMap> thingMaps = new HashMap<>();
				for (String thingId : matchingThings.keySet()) {
					ThingMap thingMap = getThingMapInstance(thingMapClass, thingId);
					thingMaps.put(thingId, thingMap);
					if (!isListExpected) {
						break;
					}
				}
				/*
				 * Get IsRequired channel Fields in Thing
				 * channelId -> Field in ThingMap
				 */
				Map<String, Field> thingMapFields = new HashMap<>();
				for (Field mapField : thingMapClass.getDeclaredFields()) {
					if (mapField.isAnnotationPresent(IsRequired.class)) {
						@SuppressWarnings("null")
						IsRequired isRequired = mapField.getAnnotation(IsRequired.class);
						thingMapFields.put(isRequired.channelId(), mapField);
					}
				}
				/*
				 * Thing.@IsChannel -> ThingMap.@IsRequired
				 */
				for (Entry<String, ThingMap> thingMapEntry : thingMaps.entrySet()) {
					String thingId = thingMapEntry.getKey();
					ThingMap thingMap = thingMapEntry.getValue();
					for (Entry<String, Field> thingMapFieldEntry : thingMapFields.entrySet()) {
						String channelId = thingMapFieldEntry.getKey();
						Field targetField = thingMapFieldEntry.getValue();
						Method sourceMethod = channelMethods.get(channelId);
						if (sourceMethod == null) {
							throw new ConfigException("No matching source Method found for ThingMap ["
									+ thingMap.getClass().getCanonicalName() + "], ChannelId [" + channelId
									+ "], Field [" + targetField.getName() + "]");
						} else {
							log.debug("Match ThingId [" + thingId + "], ChannelId [" + channelId + "]: Method ["
									+ sourceMethod.getName() + "] -> Field [" + targetField.getName() + "]");
							try {
								Thing sourceThing = matchingThings.get(thingId);
								Channel channel = (Channel) sourceMethod.invoke(sourceThing);
								channel.setChannelId(channelId);
								targetField.set(thingMap, channel);
								channel.setAsRequired();
							} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException
									| NullPointerException e) {
								e.printStackTrace();
								throw new InjectionException("Unable to set IsRequired field for ThingId [" + thingId
										+ "], ThingMap [" + thingMap.getClass().getSimpleName() + "], Channel ["
										+ channelId + "]: " + e.getMessage());
							}
						}
					}
				}
				/*
				 * ThingMap -> Controller.@IsThingMapping
				 */
				if (isListExpected) {
					try {
						List<ThingMap> thingMapsList = new ArrayList<>();
						thingMapsList.addAll(thingMaps.values());
						controllerField.set(controller, thingMapsList);
					} catch (IllegalArgumentException | IllegalAccessException e) {
						throw new InjectionException(
								"Unable to set IsThingMapping to Field [" + controller.getClass().getSimpleName() + "."
										+ controllerField.getName() + "]: " + e.getMessage());
					}
				} else {
					try {
						ThingMap thingMap = thingMaps.values().iterator().next();
						controllerField.set(controller, thingMap);
					} catch (IllegalArgumentException | IllegalAccessException | NoSuchElementException e) {
						throw new InjectionException(
								"Unable to set IsThingMapping to Field [" + controller.getClass().getSimpleName() + "."
										+ controllerField.getName() + "]: " + e.getMessage());
					}
				}
				// TODO else
			}
		}
		return result;
	}

	/**
	 * Creates a ThingMap instance of the given {@link Class}. {@link Object} arguments are optional.
	 *
	 * @param clazz
	 * @param args
	 * @return
	 * @throws ConfigException
	 */
	private static ThingMap getThingMapInstance(Class<?> clazz, Object... args) throws ConfigException {
		try {
			return (ThingMap) InjectionUtils.getInstance(clazz, args);
		} catch (ClassCastException e) {
			e.printStackTrace();
			throw new ConfigException("Class [" + clazz.getName() + "] is not a ThingMap");
		}
	}
}
