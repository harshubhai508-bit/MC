package com.example.strongholdfinder;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EyeOfEnderEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StrongholdFinderClient implements ClientModInitializer {
    private MinecraftClient mc = MinecraftClient.getInstance();
    private Map<UUID, Boolean> trackedEyes = new HashMap<>();
    private final List<ThrowRecord> throws = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) {
                trackedEyes.clear();
                throws.clear();
                return;
            }

            Iterator<Entity> it = client.world.iterateEntities().iterator();
            while (it.hasNext()) {
                Entity e = it.next();
                if (e instanceof EyeOfEnderEntity) {
                    UUID id = e.getUuid();
                    if (!trackedEyes.containsKey(id)) {
                        trackedEyes.put(id, false);
                    }
                }
            }

            for (Map.Entry<UUID, Boolean> entry : new ArrayList<>(trackedEyes.entrySet())) {
                UUID id = entry.getKey();
                boolean recorded = entry.getValue();
                if (recorded) continue;

                Entity eye = findEntityByUUID(id);
                if (eye == null) {
                    trackedEyes.remove(id);
                    continue;
                }

                HitResult target = client.crosshairTarget;
                if (target instanceof EntityHitResult) {
                    EntityHitResult ehr = (EntityHitResult) target;
                    if (ehr.getEntity().getUuid().equals(id)) {
                        ClientPlayerEntity player = client.player;
                        double px = player.getX();
                        double pz = player.getZ();
                        float yaw = player.getYaw();
                        throws.add(new ThrowRecord(px, pz, yaw));
                        trackedEyes.put(id, true);

                        if (throws.size() >= 2) {
                            Vec3d est = estimateStronghold(throws);
                            if (est != null) {
                                int estX = (int) Math.round(est.x);
                                int estZ = (int) Math.round(est.z);
                                int netherX = estX / 8;
                                int netherZ = estZ / 8;
                                String msg = String.format("[Stronghold Finder] Estimated Staircase: Overworld: %d ~ %d | Nether: %d ~ %d | Throws: %d",
                                        estX, estZ, netherX, netherZ, throws.size());
                                client.player.sendMessage(new net.minecraft.text.LiteralText(msg), false);
                            }
                        }
                    }
                }
            }

            trackedEyes.keySet().removeIf(k -> findEntityByUUID(k) == null);
        });
    }

    private Entity findEntityByUUID(UUID id) {
        for (Entity e : mc.world.getEntities()) {
            if (e.getUuid().equals(id)) return e;
        }
        return null;
    }

    private Vec3d estimateStronghold(List<ThrowRecord> recs) {
        if (recs.size() < 2) return null;
        ThrowRecord a = recs.get(recs.size() - 2);
        ThrowRecord b = recs.get(recs.size() - 1);

        double ax = a.x;
        double az = a.z;
        double ayaw = Math.toRadians(-a.yaw);
        double adx = Math.cos(ayaw);
        double adz = Math.sin(ayaw);

        double bx = b.x;
        double bz = b.z;
        double byaw = Math.toRadians(-b.yaw);
        double bdx = Math.cos(byaw);
        double bdz = Math.sin(byaw);

        double det = adx * (-bdz) - adz * (-bdx);
        if (Math.abs(det) < 1e-6) return null;

        double rhs1 = bx - ax;
        double rhs2 = bz - az;

        double t = (rhs1 * (-bdz) - rhs2 * (-bdx)) / det;
        double ix = ax + t * adx;
        double iz = az + t * adz;

        return new Vec3d(ix, 0, iz);
    }

    private static class ThrowRecord {
        final double x;
        final double z;
        final float yaw;
        ThrowRecord(double x, double z, float yaw) {
            this.x = x;
            this.z = z;
            this.yaw = yaw;
        }
    }
}
