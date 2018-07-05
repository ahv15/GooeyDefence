/*
 * Copyright 2018 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.gooeyDefence.upgrading;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.metadata.ComponentFieldMetadata;
import org.terasology.entitySystem.metadata.ComponentLibrary;
import org.terasology.entitySystem.metadata.ComponentMetadata;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.registry.In;
import org.terasology.registry.Share;

import java.util.List;
import java.util.Map;

/**
 * Handles applying an upgrade to a component
 */
@Share(UpgradingSystem.class)
@RegisterSystem
public class UpgradingSystem extends BaseComponentSystem {
    private static final Logger logger = LoggerFactory.getLogger(UpgradingSystem.class);
    @In
    private EntityManager entityManager;
    private ComponentLibrary componentLibrary;

    @Override
    public void postBegin() {
        componentLibrary = entityManager.getComponentLibrary();
    }

    /**
     * Test handler.
     * <p>
     * Filters on {@link BlockUpgradesComponent}
     *
     * @see ActivateEvent
     */
    @ReceiveEvent
    public void onActivate(ActivateEvent event, EntityRef entity, BlockUpgradesComponent upgraderComponent) {
        for (UpgradeList upgradeList : upgraderComponent.getUpgrades()) {
            /* Print out the name and apply the upgrade */
            logger.info("Applying upgrade " + upgradeList.getUpgradeName());
            List<UpgradeInfo> stages = upgradeList.getStages();
            if (!stages.isEmpty()) {
                applyUpgrade(entity, stages.remove(0));
            }
        }
        logger.info(entity.toFullDescription());
    }

    /**
     * Applies a given upgrade to the entity.
     *
     * @param entity  The entity to upgrade
     * @param upgrade The upgrade to apply
     * @see UpgradeInfo
     */
    public void applyUpgrade(EntityRef entity, UpgradeInfo upgrade) {
        /* Get needed data */
        BlockUpgradesComponent upgradesComponent = entity.getComponent(BlockUpgradesComponent.class);
        ComponentMetadata<?> componentMeta = componentLibrary.resolve(upgradesComponent.getComponentName());
        Class<? extends Component> componentClass = componentMeta.getType();
        Component component = entity.getComponent(componentClass);

        /* Apply upgrade for each field */
        for (Map.Entry<String, Number> entry : upgrade.getValues().entrySet()) {
            ComponentFieldMetadata<?, ?> fieldMeta = componentMeta.getField(entry.getKey());
            setField(fieldMeta, component, entry.getValue());
        }
    }

    /**
     * Sets a Number field to the given value.
     * Requires the field to be of a primitive number type.
     *
     * @param field     The field to set
     * @param component The component containing the field to set
     * @param value     The value to set the field to
     */
    private void setField(ComponentFieldMetadata field, Component component, Number value) {
        switch (field.getType().getSimpleName()) {
            case "int":
                field.setValue(component, (int) field.getValue(component) + value.intValue());
                break;
            case "double":
                field.setValue(component, (double) field.getValue(component) + value.doubleValue());
                break;
            case "float":
                field.setValue(component, (float) field.getValue(component) + value.floatValue());
                break;
            case "long":
                field.setValue(component, (long) field.getValue(component) + value.longValue());
                break;
            case "short":
                field.setValue(component, (short) field.getValue(component) + value.shortValue());
                break;
            case "byte":
                field.setValue(component, (byte) field.getValue(component) + value.byteValue());
                break;
            default:
                //TODO: work out what to throw here
                throw new Error("Can't set field of type: "
                        + field.getField().getGenericType().getTypeName()
                        + ". Type must be a Number primitive");
        }
    }
}
