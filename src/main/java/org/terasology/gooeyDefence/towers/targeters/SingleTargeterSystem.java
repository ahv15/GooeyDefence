/*
 * Copyright 2017 MovingBlocks
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
package org.terasology.gooeyDefence.towers.targeters;

import org.joml.Vector3f;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.registry.In;
import org.terasology.gooeyDefence.EnemyManager;
import org.terasology.gooeyDefence.towers.TowerManager;
import org.terasology.gooeyDefence.towers.events.SelectEnemiesEvent;
import org.terasology.gooeyDefence.visuals.InWorldRenderer;

/**
 * Targets a single enemy within range.
 *
 * @see SingleTargeterComponent
 * @see TowerManager
 */
@RegisterSystem
public class SingleTargeterSystem extends BaseTargeterSystem {

    @In
    protected EnemyManager enemyManager;
    @In
    private InWorldRenderer inWorldRenderer;

    /**
     * Determine which enemies should be attacked.
     * Called against the targeter entity.
     * <p>
     * Filters on {@link LocationComponent} and {@link SingleTargeterComponent}
     *
     * @see SelectEnemiesEvent
     */
    @ReceiveEvent
    public void onDoSelectEnemies(SelectEnemiesEvent event, EntityRef entity, LocationComponent locationComponent, SingleTargeterComponent targeterComponent) {
        EntityRef target = getTarget(locationComponent.getWorldPosition(new Vector3f()), targeterComponent, enemyManager);

        if (target.exists()) {
            event.addToList(target);
            inWorldRenderer.shootBulletTowards(
                    target,
                    locationComponent.getWorldPosition(new Vector3f()));
        }
        targeterComponent.lastTarget = target;

    }


}
