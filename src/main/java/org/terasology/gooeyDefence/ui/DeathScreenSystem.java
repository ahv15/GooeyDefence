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
package org.terasology.gooeyDefence.ui;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.gooeyDefence.components.ShrineComponent;
import org.terasology.gooeyDefence.events.health.EntityDeathEvent;
import org.terasology.registry.In;
import org.terasology.rendering.nui.NUIManager;
import org.terasology.rendering.nui.WidgetUtil;
import org.terasology.rendering.nui.layers.ingame.DeathScreen;
import org.terasology.rendering.nui.widgets.UIButton;
import org.terasology.rendering.nui.widgets.UILabel;

/**
 * Handles overwriting the death screen to allow
 */
@RegisterSystem
public class DeathScreenSystem extends BaseComponentSystem {

    @In
    private NUIManager nuiManager;


    /**
     * Used to display the death screen, and apply modifications to it.
     * <p>
     * Sent when an entity dies
     * Filters on {@link ShrineComponent}
     *
     * @see EntityDeathEvent
     */
    @ReceiveEvent(components = ShrineComponent.class)
    public void onEntityDeath(EntityDeathEvent event, EntityRef entity) {
        DeathScreen deathScreen = nuiManager.pushScreen("Engine:DeathScreen", DeathScreen.class);
    }

}
