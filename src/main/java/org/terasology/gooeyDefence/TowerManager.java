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
package org.terasology.gooeyDefence;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.gooeyDefence.components.towers.TowerComponent;
import org.terasology.gooeyDefence.events.OnFieldReset;
import org.terasology.gooeyDefence.events.combat.ApplyEffectEvent;
import org.terasology.gooeyDefence.events.combat.RemoveEffectEvent;
import org.terasology.gooeyDefence.events.combat.SelectEnemiesEvent;
import org.terasology.gooeyDefence.events.tower.TowerChangedEvent;
import org.terasology.gooeyDefence.events.tower.TowerCreatedEvent;
import org.terasology.gooeyDefence.events.tower.TowerDestroyedEvent;
import org.terasology.gooeyDefence.towerBlocks.EffectCount;
import org.terasology.gooeyDefence.towerBlocks.EffectDuration;
import org.terasology.gooeyDefence.towerBlocks.base.TowerCore;
import org.terasology.gooeyDefence.towerBlocks.base.TowerEffector;
import org.terasology.gooeyDefence.towerBlocks.base.TowerTargeter;
import org.terasology.gooeyDefence.worldGeneration.providers.RandomFillingProvider;
import org.terasology.logic.delay.DelayManager;
import org.terasology.logic.delay.PeriodicActionTriggeredEvent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.geom.Vector2i;
import org.terasology.math.geom.Vector3i;
import org.terasology.registry.In;
import org.terasology.utilities.procedural.Noise;
import org.terasology.utilities.procedural.WhiteNoise;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@RegisterSystem
public class TowerManager extends BaseComponentSystem {
    private static final Logger logger = LoggerFactory.getLogger(TowerManager.class);

    @In
    private DelayManager delayManager;
    @In
    private EntityManager entityManager;
    @In
    private WorldProvider worldProvider;
    @In
    private BlockManager blockManager;

    private Block air;
    private Block fieldBlock;
    private Block shrineBlock;
    private Set<EntityRef> towerEntities = new HashSet<>();

    /**
     * Creates the periodic event id for the targeter on a tower
     *
     * @param tower    The tower the targeter is on
     * @param targeter The targeter the event is sending for
     * @return The id for that periodic action event.
     * @see PeriodicActionTriggeredEvent
     */
    private static String buildEventId(EntityRef tower, EntityRef targeter) {
        return "towerDefence|" + targeter.getId();
    }

    /**
     * Checks that the periodic event is intended for the given tower.
     *
     * @param tower   The tower to check for
     * @param eventId The id of the periodic event
     * @return True if the event belongs to the tower
     * @see PeriodicActionTriggeredEvent
     */
    private static boolean isEventIdCorrect(EntityRef tower, String eventId) {
        return eventId.startsWith("towerDefence");
    }

    /**
     * Gets the ID of the targeter from the periodic event id.
     *
     * @param eventId The id of the periodic event
     * @return The ID of the targeter entity ref
     */
    private static long getTargeterId(String eventId) {
        String id = eventId.substring(eventId.indexOf('|') + 1);
        return Long.parseLong(id);
    }

    /**
     * Get the drain caused by all the targeters on a tower
     *
     * @param towerComponent The TowerComponent of the tower entity
     * @return The total drain. Zero if the tower has no targeters
     */
    public static int getTargeterDrain(TowerComponent towerComponent) {
        return towerComponent.targeter.
                stream()
                .mapToInt(entity -> DefenceField.getComponentExtending(entity, TowerTargeter.class).getDrain())
                .sum();
    }

    /**
     * Get the drain caused by all the effector on a tower
     *
     * @param towerComponent The TowerComponent of the tower entity
     * @return The total drain. Zero if the tower has no effector
     */
    public static int getEffectorDrain(TowerComponent towerComponent) {
        return towerComponent.effector.
                stream()
                .mapToInt(entity -> DefenceField.getComponentExtending(entity, TowerEffector.class).getDrain())
                .sum();
    }

    /**
     * Get the power generated by all the cores on a tower
     *
     * @param towerComponent The TowerComponent of the tower entity
     * @return The total power. Zero if the tower has no cores
     */
    public static int getTotalCorePower(TowerComponent towerComponent) {
        return towerComponent.cores.
                stream()
                .mapToInt(entity -> DefenceField.getComponentExtending(entity, TowerCore.class).getPower())
                .sum();
    }

    /**
     * Checks if the power produced by the cores is more than or equal to the power consumed
     *
     * @param towerComponent The tower to check
     * @return True, if the tower produces enough power
     */
    public static boolean hasEnoughPower(TowerComponent towerComponent) {
        return getTotalCorePower(towerComponent) >= getTargeterDrain(towerComponent) + getEffectorDrain(towerComponent);
    }

    @Override
    public void initialise() {
        air = blockManager.getBlock(BlockManager.AIR_ID);
        fieldBlock = blockManager.getBlock("GooeyDefence:PlainWorldGen");
        shrineBlock = blockManager.getBlock("GooeyDefence:Shrine");
    }

    /**
     * Remove all scheduled delays before the game is shutdown.
     */
    @Override
    public void shutdown() {
        for (EntityRef tower : towerEntities) {
            TowerComponent towerComponent = tower.getComponent(TowerComponent.class);
            for (EntityRef targeter : towerComponent.targeter) {
                delayManager.cancelPeriodicAction(tower, buildEventId(tower, targeter));
            }
            tower.destroy();
        }
    }

    /**
     * Destroys all the tower blocks
     * <p>
     * Sent when the field should be reset
     *
     * @see OnFieldReset
     */
    @ReceiveEvent
    public void onFieldReset(OnFieldReset event, EntityRef entity) {
        for (EntityRef towerEntity : towerEntities) {
            TowerComponent component = towerEntity.getComponent(TowerComponent.class);
            clearBlocks(component.cores);
            clearBlocks(component.effector);
            clearBlocks(component.targeter);
            clearBlocks(component.plains);
            towerEntity.destroy();
        }
        towerEntities.clear();

        clearField(DefenceField.outerRingSize());
        createRandomFill(DefenceField.outerRingSize());
    }

    /**
     * Replaces block entities with air, and destroys the entity
     *
     * @param blocks The blocks to replace
     */
    private void clearBlocks(Collection<EntityRef> blocks) {
        blocks.forEach(EntityRef::destroy);
        blocks.clear();
    }

    /**
     * Clears all non world gen block from the field.
     * <p>
     * These are:
     * - "GooeyDefence:ShrineBlock"
     * - "Engine:Air"
     *
     * @param size The size of the field to clear.
     */
    private void clearField(int size) {
        Vector3i pos = Vector3i.zero();
        Block block;
        for (int x = -size; x <= size; x++) {
            /* We use circle eq "x^2 + y^2 = r^2" to work out where we need to start */
            int width = (int) Math.floor(Math.sqrt(size * size - x * x));
            for (int z = -width; z <= width; z++) {
                /* We use sphere eq "x^2 + y^2 + z^2 = r^2" to work out how high we need to go */
                int height = (int) Math.floor(Math.sqrt(size * size - z * z - x * x) - 0.001f);
                for (int y = 0; y <= height; y++) {
                    pos.set(x, y, z);
                    block = worldProvider.getBlock(pos);
                    if (block != air && block != shrineBlock) {
                        worldProvider.setBlock(pos, air);
                    }
                }
            }
        }
    }

    /**
     * Randomly fills in an area, according to the same rules as the world gen
     *
     * @param size The size of the area to fill
     * @see RandomFillingProvider
     */
    private void createRandomFill(int size) {
        Noise noise = new WhiteNoise(System.currentTimeMillis());
        Vector2i pos2i = Vector2i.zero();
        Vector3i pos3i = Vector3i.zero();

        for (int x = -size; x <= size; x++) {
            int width = (int) Math.floor(Math.sqrt(size * size - x * x));
            for (int y = -width; y <= width; y++) {
                pos2i.setX(x);
                pos2i.setY(y);
                if (RandomFillingProvider.shouldSpawnBlock(pos2i, noise)) {
                    pos3i.setX(pos2i.x);
                    pos3i.setZ(pos2i.y);
                    worldProvider.setBlock(pos3i, fieldBlock);
                }
            }
        }
    }

    /**
     * Called when a tower is created.
     * Adds the tower to the list and sets the periodic actions for it's attacks
     * <p>
     * Filters on {@link TowerComponent}
     *
     * @see TowerCreatedEvent
     */
    @ReceiveEvent
    public void onTowerCreated(TowerCreatedEvent event, EntityRef towerEntity, TowerComponent towerComponent) {
        towerEntities.add(towerEntity);
        for (EntityRef targeter : towerComponent.targeter) {
            TowerTargeter targeterComponent = DefenceField.getComponentExtending(targeter, TowerTargeter.class);
            delayManager.addPeriodicAction(towerEntity,
                    buildEventId(towerEntity, targeter),
                    targeterComponent.getAttackSpeed(),
                    targeterComponent.getAttackSpeed());
        }
    }

    /**
     * Called when a block is added to a tower.
     * Cancels the old periodic actions and schedules new ones.
     * <p>
     * Filters on {@link TowerComponent}
     *
     * @see TowerChangedEvent
     */
    @ReceiveEvent
    public void onTowerChanged(TowerChangedEvent event, EntityRef towerEntity, TowerComponent towerComponent) {
        for (EntityRef targeter : towerComponent.targeter) {
            if (event.getChangedBlocks().contains(targeter)) {
                TowerTargeter targeterComponent = DefenceField.getComponentExtending(targeter, TowerTargeter.class);
                delayManager.addPeriodicAction(towerEntity,
                        buildEventId(towerEntity, targeter),
                        targeterComponent.getAttackSpeed(),
                        targeterComponent.getAttackSpeed());
            }
        }
    }

    /**
     * Called when a tower is destroyed.
     * Removes all the periodic actions and the tower from the store.
     * <p>
     * Filters on {@link TowerComponent}
     */
    @ReceiveEvent
    public void onTowerDestroyed(TowerDestroyedEvent event, EntityRef towerEntity, TowerComponent towerComponent) {
        for (EntityRef targeter : towerComponent.targeter) {
            handleTargeterRemoval(towerEntity, targeter);
        }
        towerEntities.remove(towerEntity);
    }

    /**
     * Called every attack cycle per targeter.
     * Checks if the tower can fire, and if so, fires that targeter.
     * <p>
     * Filters on {@link TowerComponent}
     *
     * @see PeriodicActionTriggeredEvent
     */
    @ReceiveEvent
    public void onPeriodicActionTriggered(PeriodicActionTriggeredEvent event, EntityRef entity, TowerComponent component) {
        if (DefenceField.isFieldActivated() && isEventIdCorrect(entity, event.getActionId())) {
            if (hasEnoughPower(component)) {
                EntityRef targeter = entityManager.getEntity(getTargeterId(event.getActionId()));
                handleTowerShooting(component, targeter);
            }
        }
    }

    /**
     * Handles the removal of a targeter from a tower.
     * Does this by calling the tower to end the effects on the
     *
     * @param tower
     * @param targeter
     */
    private void handleTargeterRemoval(EntityRef tower, EntityRef targeter) {

        delayManager.cancelPeriodicAction(tower, buildEventId(tower, targeter));

        TowerComponent towerComponent = tower.getComponent(TowerComponent.class);
        TowerTargeter targeterComponent = DefenceField.getComponentExtending(targeter, TowerTargeter.class);
        for (EntityRef enemy : targeterComponent.getAffectedEnemies()) {
            endEffects(towerComponent.effector, enemy, targeterComponent.getMultiplier());
        }
    }

    /**
     * Handles the steps involved in making a targeter shoot.
     *
     * @param towerComponent The TowerComponent of the tower entity shooting.
     * @param targeter       The targeter that's shooting
     */
    private void handleTowerShooting(TowerComponent towerComponent, EntityRef targeter) {
        Set<EntityRef> currentTargets = getTargetedEnemies(targeter);
        TowerTargeter towerTargeter = DefenceField.getComponentExtending(targeter, TowerTargeter.class);

        applyEffectsToTargets(towerComponent.effector, currentTargets, towerTargeter);

        towerTargeter.setAffectedEnemies(currentTargets);
    }

    /**
     * Calls on the targeter to obtain the enemies it's targeting.
     *
     * @param targeter The targeter to call on
     * @return All entities targeted by that targeter.
     * @see TowerTargeter
     */
    private Set<EntityRef> getTargetedEnemies(EntityRef targeter) {
        SelectEnemiesEvent shootEvent = new SelectEnemiesEvent();
        targeter.send(shootEvent);
        return shootEvent.getTargets();
    }

    /**
     * Applies all the effects on a tower to the targeted enemies
     *
     * @param effectors      The effectors on the tower
     * @param currentTargets The current targets of the tower
     * @param towerTargeter  The targeter shooting
     * @see TowerEffector
     */
    private void applyEffectsToTargets(Set<EntityRef> effectors, Set<EntityRef> currentTargets, TowerTargeter towerTargeter) {
        Set<EntityRef> exTargets = Sets.difference(towerTargeter.getAffectedEnemies(), currentTargets);

        /* Apply effects to targeted enemies */
        currentTargets.forEach(target ->
                applyEffects(effectors,
                        target,
                        towerTargeter.getMultiplier(),
                        !towerTargeter.getAffectedEnemies().contains(target)));

        /* Process all the enemies that are no longer targeted */
        for (EntityRef exTarget : exTargets) {
            endEffects(effectors, exTarget, towerTargeter.getMultiplier());
        }
    }

    /**
     * Applies all the effects on a tower to an enemy.
     *
     * @param effectors   The effectors to use to apply the effects
     * @param target      The target enemy
     * @param multiplier  The multiplier from the targeter
     * @param isTargetNew Indicates if the enemy is newly targeted. Used to filter effectors
     */
    private void applyEffects(Set<EntityRef> effectors, EntityRef target, float multiplier, boolean isTargetNew) {
        ApplyEffectEvent event = new ApplyEffectEvent(target, multiplier);

        for (EntityRef effector : effectors) {
            TowerEffector effectorComponent = DefenceField.getComponentExtending(effector, TowerEffector.class);
            switch (effectorComponent.getEffectCount()) {
                case CONTINUOUS:
                    if (isTargetNew) {
                        effector.send(event);
                    }
                    break;
                case PER_SHOT:
                    effector.send(event);
                    break;
                default:
                    throw new EnumConstantNotPresentException(EffectCount.class, effectorComponent.getEffectCount().toString());
            }
        }
    }

    /**
     * Calls on each effector to end the effect on a target, where applicable.
     *
     * @param effectors  The effectors to check through
     * @param oldTarget  The target to remove the effects from
     * @param multiplier The effect multiplier to apply to the event
     */
    private void endEffects(Set<EntityRef> effectors, EntityRef oldTarget, float multiplier) {
        RemoveEffectEvent event = new RemoveEffectEvent(oldTarget, multiplier);
        for (EntityRef effector : effectors) {
            TowerEffector effectorComponent = DefenceField.getComponentExtending(effector, TowerEffector.class);
            switch (effectorComponent.getEffectDuration()) {
                case LASTING:
                    effector.send(event);
                    break;
                case INSTANT:
                case PERMANENT:
                    break;
                default:
                    throw new EnumConstantNotPresentException(EffectDuration.class, effectorComponent.getEffectCount().toString());
            }
        }
    }
}
