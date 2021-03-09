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
package org.terasology.gooeyDefence.ui.componentParsers.effectors;

import org.terasology.engine.entitySystem.Component;
import org.terasology.gooeyDefence.towers.effectors.FireEffectorComponent;
import org.terasology.gooeyDefence.ui.towers.UIUpgrader;
import org.terasology.gooeyDefence.upgrading.UpgradingSystem;

import java.util.Map;

/**
 * Handles converting fields for the fire effector
 *
 * @see FireEffectorComponent
 * @see UIUpgrader
 * @see UpgradingSystem
 */
public class FireParser extends DamageParser {
    @Override
    public Class<? extends Component> getComponentClass() {
        return FireEffectorComponent.class;
    }

    @Override
    public Map<String, String> getFields() {
        Map<String, String> result = super.getFields();
        result.put("fireDuration", "Burn Duration");
        return result;
    }

    /**
     * Converts the duration field from milliseconds into seconds.
     *
     * @param isUpgrade True if the value is actually an upgrade value
     * @param value     The value to convert
     * @return The string version of the value.
     */
    public String fireDuration(boolean isUpgrade, int value) {
        return (isUpgrade ? "+" : "") + convertDuration(value);
    }
}
