package br.com;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginVideo extends JavaPlugin {

    @Override
    public void onEnable() {
        Bukkit.getConsoleSender().sendMessage("§a[PluginVideo] Plugin ativado!");
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage("§c[PluginVideo] Plugin desativado!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("zuluinit")) {
            if (sender instanceof Player) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "npc create zulu");
                sender.sendMessage("§aNPC criado com sucesso!");
            }
            return true;
        }
        return false;
    }
}
