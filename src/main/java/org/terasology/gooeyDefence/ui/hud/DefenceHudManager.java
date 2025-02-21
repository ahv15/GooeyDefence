// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.gooeyDefence.ui.hud;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.registry.In;
import org.terasology.engine.rendering.nui.NUIManager;
import org.terasology.module.health.ui.HealthHud;
import org.terasology.gooeyDefence.DefenceUris;
import org.terasology.gooeyDefence.StatSystem;
import org.terasology.gooeyDefence.waves.OnWaveEnd;
import org.terasology.joml.geom.Rectanglef;
import org.terasology.nui.databinding.ReadOnlyBinding;
import org.terasology.nui.widgets.UIIconBar;

/**
 * Manages displaying and setting all the hud elements.
 *
 * @see DefenceHud
 */
@RegisterSystem
public class DefenceHudManager extends BaseComponentSystem {
    @In
    private NUIManager nuiManager;

    @In
    private StatSystem statSystem;

    private DefenceHud defenceHud;

    @Override
    public void postBegin() {
        defenceHud = nuiManager.getHUD().addHUDElement(DefenceUris.DEFENCE_HUD, DefenceHud.class, new Rectanglef(0, 0, 1, 1));
        HealthHud healthHud = nuiManager.getHUD().getHUDElement(DefenceUris.HEALTH_HUD, HealthHud.class);
        UIIconBar healthBar = healthHud.find("healthBar", UIIconBar.class);

        defenceHud.updateCurrentWave();

        healthBar.bindMaxValue(new ReadOnlyBinding<Float>() {
            @Override
            public Float get() {
                return (float) statSystem.getMaxHealth();
            }
        });
        healthBar.bindValue(new ReadOnlyBinding<Float>() {
            @Override
            public Float get() {
                return (float) statSystem.getShrineHealth();
            }
        });
    }

    /**
     * Updates the wave displayed in the HUD
     * <p>
     * Called when a wave is ended
     *
     * @see OnWaveEnd
     */
    @ReceiveEvent
    public void onWaveEnd(OnWaveEnd event, EntityRef entity) {
        defenceHud.updateCurrentWave();
    }
}
