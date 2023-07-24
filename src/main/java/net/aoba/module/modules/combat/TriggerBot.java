package net.aoba.module.modules.combat;

import net.aoba.core.osettings.osettingtypes.BooleanOSetting;
import net.aoba.core.osettings.osettingtypes.DoubleOSetting;
import org.lwjgl.glfw.GLFW;
import net.aoba.module.Module;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class TriggerBot extends Module {
	private DoubleOSetting radius;
	private BooleanOSetting targetAnimals;
	private BooleanOSetting targetMonsters;
	private BooleanOSetting targetPlayers;

	
	public TriggerBot() {
		this.setName("Triggerbot");
		this.setBind(new KeyBinding("key.triggerbot", GLFW.GLFW_KEY_UNKNOWN, "key.categories.aoba"));
		this.setCategory(Category.Combat);
		this.setDescription("Attacks anything you are looking at.");

		radius = new DoubleOSetting("triggerbot_radius", "Radius", 5f, null, 0.1f, 10f, 0.1f);
		targetAnimals = new BooleanOSetting("triggerbot_target_animals", "Target animals.", false, null);
		targetMonsters = new BooleanOSetting("triggerbot_target_monsters", "Target monsters.", true, null);
		targetPlayers = new BooleanOSetting("triggerbot_target_players", "Target players.", true, null);
		this.addSetting(radius);
		this.addSetting(targetAnimals);
		this.addSetting(targetMonsters);
		this.addSetting(targetPlayers);
	}

	@Override
	public void onDisable() {

	}

	@Override
	public void onEnable() {

	}

	@Override
	public void onToggle() {

	}

	@Override
	public void onUpdate() {
		// Get the current target that the player is looking at.
		HitResult ray = MC.crosshairTarget;
		
		// If the target is an Entity, attack it.
		if(ray != null && ray.getType()==HitResult.Type.ENTITY) {
			EntityHitResult entityResult = (EntityHitResult) ray;
			Entity ent = entityResult.getEntity();
			if(ent instanceof AnimalEntity && !this.targetAnimals.getValue()) return;
			if(ent instanceof PlayerEntity && !this.targetPlayers.getValue()) return;
			if(ent instanceof Monster && !this.targetMonsters.getValue()) return;
			
			if(MC.player.getAttackCooldownProgress(0) == 1) {
				MC.interactionManager.attackEntity(MC.player, entityResult.getEntity());
				MC.player.swingHand(Hand.MAIN_HAND);
			}
		}
	}

	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks) {

	}

	@Override
	public void onSendPacket(Packet<?> packet) {

	}

	@Override
	public void onReceivePacket(Packet<?> packet) {

	}
}
