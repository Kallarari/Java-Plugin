package br.com;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;

public final class PluginVideo extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("NPC Teams Plugin habilitado!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("spawnteams")) {
            if (!sender.hasPermission("npcteams.spawn")) {
                sender.sendMessage("§cVocê não tem permissão para usar este comando!");
                return true;
            }

            if (args.length < 1) {
                sender.sendMessage("§eUso: /spawnteams <localização|aqui>");
                return true;
            }

            Location spawnLocation = parseLocation(sender, args[0]);
            if (spawnLocation == null) {
                sender.sendMessage("§cLocalização inválida!");
                return true;
            }

            spawnTeams(spawnLocation);
            sender.sendMessage("§aTimes de NPCs spawnados com sucesso!");
            return true;
        }
        return false;
    }

    private Location parseLocation(CommandSender sender, String arg) {
        if (arg.equalsIgnoreCase("aqui") && sender instanceof org.bukkit.entity.Player) {
            return ((org.bukkit.entity.Player) sender).getLocation();
        }
        return null;
    }

    private void spawnTeams(Location centerLocation) {
        NPCRegistry registry = CitizensAPI.getNPCRegistry();
        org.bukkit.scoreboard.Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        // Configura times
        Team redTeam = setupTeam(scoreboard, "Red", "§c");
        Team blueTeam = setupTeam(scoreboard, "Blue", "§9");

        // Spawn NPCs do time vermelho
        for (int i = 0; i < 5; i++) {
            Location spawnLoc = centerLocation.clone().add(i * 3, 0, 0);
            NPC npc = createNPC(registry, "RedFighter-" + (i+1), "Steve", spawnLoc);
            setupNPC(npc, redTeam, blueTeam);
        }

        // Spawn NPCs do time azul
        for (int i = 0; i < 5; i++) {
            Location spawnLoc = centerLocation.clone().add(0, 0, i * 3);
            NPC npc = createNPC(registry, "BlueFighter-" + (i+1), "Alex", spawnLoc);
            setupNPC(npc, blueTeam, redTeam);
        }
    }

    private Team setupTeam(org.bukkit.scoreboard.Scoreboard scoreboard, String name, String color) {
        Team team = scoreboard.getTeam(name) != null ? scoreboard.getTeam(name) : scoreboard.registerNewTeam(name);
        team.setAllowFriendlyFire(false);
        team.setCanSeeFriendlyInvisibles(true);
        team.setPrefix(color);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        return team;
    }

    private NPC createNPC(NPCRegistry registry, String name, String skin, Location location) {
        NPC npc = registry.createNPC(EntityType.PLAYER, name);
        npc.spawn(location);

        SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
        skinTrait.setSkinName(skin);

        return npc;
    }

    private void setupNPC(NPC npc, Team ownTeam, Team enemyTeam) {
        ownTeam.addEntry(npc.getUniqueId().toString());

        if (npc.getEntity() instanceof org.bukkit.entity.LivingEntity) {
            org.bukkit.entity.LivingEntity entity = (org.bukkit.entity.LivingEntity) npc.getEntity();
            entity.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            entity.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
            entity.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        }

        if (isSentinelEnabled()) {
            configureSentinel(npc, ownTeam, enemyTeam);
        }
    }

    private boolean isSentinelEnabled() {
        return Bukkit.getPluginManager().getPlugin("Sentinel") != null;
    }

    private void configureSentinel(NPC npc, Team ownTeam, Team enemyTeam) {
        try {
            // Acessa a instância do Sentinel via reflection
            Class<?> sentinelClass = Class.forName("com.massivecraft.sentinel.Sentinel");
            Object sentinelInstance = sentinelClass.getMethod("get").invoke(null);

            String npcId = npc.getUniqueId().toString();

            // Configura comportamento de combate
            setSentinelProperty(sentinelInstance, npcId, "Aggressive", true);
            setSentinelProperty(sentinelInstance, npcId, "AttackRate", 12);
            setSentinelProperty(sentinelInstance, npcId, "Damage", 6.0);
            setSentinelProperty(sentinelInstance, npcId, "ChaseRange", 30);
            setSentinelProperty(sentinelInstance, npcId, "Strafe", true);

            // Configura relações de time
            configureTeamRelations(sentinelClass, sentinelInstance, npcId, ownTeam, enemyTeam);

        } catch (Exception e) {
            getLogger().warning("Erro ao configurar Sentinel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setSentinelProperty(Object sentinelInstance, String npcId, String property, Object value) {
        try {
            String methodName = "set" + property;
            sentinelInstance.getClass().getMethod(methodName, String.class, value.getClass())
                    .invoke(sentinelInstance, npcId, value);
        } catch (Exception e) {
            getLogger().warning("Erro ao configurar " + property + ": " + e.getMessage());
        }
    }

    private void configureTeamRelations(Class<?> sentinelClass, Object sentinelInstance,
                                        String npcId, Team ownTeam, Team enemyTeam) throws Exception {
        // Adiciona inimigos
        for (String enemy : enemyTeam.getEntries()) {
            sentinelClass.getMethod("addEnemy", String.class, String.class)
                    .invoke(sentinelInstance, npcId, enemy);
        }

        // Adiciona aliados
        for (String ally : ownTeam.getEntries()) {
            if (!ally.equals(npcId)) {
                sentinelClass.getMethod("addAlly", String.class, String.class)
                        .invoke(sentinelInstance, npcId, ally);
            }
        }
    }
}