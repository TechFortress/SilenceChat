package me.robomwm.DisableChat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.newSetFromMap;

/**
 * Created by robom on 5/21/2016.
 */
public class DisableChat extends JavaPlugin implements Listener
{
    Set<Player> chatDisabled = newSetFromMap(new ConcurrentHashMap<>());
    String disabledMessage = "Global chat has been " + ChatColor.RED + "disabled.";
    String enabledMessage = "Global chat has been" + ChatColor.GREEN + "enabled.";

    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(this, this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (!(sender instanceof Player))
            return false;

        Player player = (Player)sender;
        if (cmd.getName().equalsIgnoreCase("chat"))
        {
            //If option is explicitly stated, respect that choice
            if (args.length > 0)
            {
                switch (args[0].toLowerCase())
                {
                    case "disable":
                    case "off":
                    case "false":
                        disableChat(player);
                        return true;
                    case "enable":
                    case "on":
                    case "true":
                        enableChat(player);
                        return true;
                }
            }

            //Otherwise, toggle
            if (chatDisabled.contains(player))
                enableChat(player);
            else
                disableChat(player);
            return true;
        }
        return false; //shouldn't ever get here
    }

    void disableChat(Player player)
    {
        chatDisabled.add(player);
        player.sendMessage(disabledMessage);
    }

    void enableChat(Player player)
    {
        chatDisabled.remove(player);
        player.sendMessage(enabledMessage);
    }

    /**
     * Feature: disable global chat.
     */
    @EventHandler(ignoreCancelled = true)
    void onAsyncPlayerChat(AsyncPlayerChatEvent event)
    {
        if (chatDisabled.isEmpty()) //If nobody disabled chat, do nothing
            return;

        //Don't allow players that have disabled chat to send chat (globally)
        if (chatDisabled.contains(event.getPlayer()))
        {
            event.getPlayer().sendMessage(ChatColor.RED + "You have disabled global chat. Use " + ChatColor.GOLD + "/chat " + ChatColor.RED + "to re-enable.");
            event.setCancelled(true);
            return;
        }

        //Otherwise, proceed to remove recipients that have disabled chat
        Set<Player> recipients = event.getRecipients();
        for (Player target : chatDisabled)
            recipients.remove(target);

        /*chatDisabled.forEach(recipients::remove); //woah wutz dis*/
    }

    /**
     * Feature: disable global /me messages.
     * Considered part of global chat.
     * Since we want to let vanilla handle /me, we work with it here.
     * (Or I could just disable use of /me, idk.)
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
    {
        if (chatDisabled.isEmpty()) //If nobody disabled chat, do nothing
            return;

        if (!event.getMessage().toLowerCase().startsWith("/me ")) //TODO: make configurable
            return;

        Player player = event.getPlayer();

        //Don't allow players that have disabled chat to send action messages
        if (chatDisabled.contains(player))
        {
            event.getPlayer().sendMessage(ChatColor.RED + "You have disabled global chat. Use " + ChatColor.GOLD + "/chat " + ChatColor.RED + "to re-enable.");
            event.setCancelled(true);
            return;
        }

        String meMessage = "* " + player.getName() + event.getMessage().substring(3);

        //Iterate through all online players, and send them the action message if they haven't disabled global chat
        for (Player target : Bukkit.getOnlinePlayers())
        {
            if (!chatDisabled.contains(target))
            {
                target.sendMessage(meMessage);
            }
        }

        event.setCancelled(true);
    }

    /**
     * Remove player from disabledChat when they disconnect
     */
    @EventHandler
    void onPlayerQuit(PlayerQuitEvent event)
    {
        chatDisabled.remove(event.getPlayer());
    }
}
