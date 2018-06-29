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
package org.terasology.gooeyDefence.towerBlocks.targeters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.gooeyDefence.DefenceField;
import org.terasology.gooeyDefence.EnemyManager;
import org.terasology.gooeyDefence.components.enemies.PathComponent;
import org.terasology.gooeyDefence.events.combat.SelectEnemiesEvent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.registry.In;

import java.util.Optional;
import java.util.Set;

/**
 * Targets the first enemy within range.
 *
 * @see SingleTargeterComponent
 */
@RegisterSystem
public class SingleTargeterSystem extends BaseComponentSystem {
    private static final Logger logger = LoggerFactory.getLogger(SingleTargeterSystem.class);
    @In
    private EnemyManager enemyManager;

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
        Set<EntityRef> targets = enemyManager.getEnemiesInRange(
                locationComponent.getWorldPosition(),
                targeterComponent.getRange());

        Optional<EntityRef> firstEnemy = targets.stream().min((first, second) -> {
            PathComponent firstComponent = DefenceField.getComponentExtending(first, PathComponent.class);
            PathComponent secondComponent = DefenceField.getComponentExtending(second, PathComponent.class);
            return firstComponent.getStep() - secondComponent.getStep();
        });
        firstEnemy.ifPresent(event::addToList);
    }
}
