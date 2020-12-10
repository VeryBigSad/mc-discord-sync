package io.github.thedoctorone;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Activity.ActivityType;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandException;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

public class DiscordCommunication extends ListenerAdapter {
    static JDA MCD;
    private Main main;
    private ChatCommands chatCommands;
    private SyncFileOperation sfo;
//    private DiscordCommandSender dcs;
    private RemoteConsoleCommandSender dcd;
    private boolean firstConnection = true;
    private boolean sendByDiscordFullReload = false;

    private String channelId;
    private Logger lg;
    private Server server;
    private String serverStartMessage;
    private String TOKEN;
    private String permId;
//    private String synced_role_id;
    private String commandsChannelId;

    DiscordCommunication(Main main, SyncFileOperation sfo) {
        this.main = main;
        this.sfo = sfo;
//        dcs = new DiscordCommandSender(this, this.main);
    }

    public void setChatCommands(ChatCommands chatCommands) {
        this.chatCommands = chatCommands;
    }

    public void executeBot(Server server, Logger lg, String TOKEN, String channelId, String permId, String serverStartMessage, String commandsChannelId) throws LoginException {
        this.TOKEN = TOKEN;
        this.serverStartMessage = serverStartMessage;
        this.server = server;
        this.lg = lg;
        this.channelId = channelId;
        this.commandsChannelId = commandsChannelId;
        this.permId = permId;
//        this.synced_role_id = synced_role_id;
        runBot(this.TOKEN);
    }

    public void reloadBot(String TOKEN) throws LoginException {
        MCD.shutdownNow();
        this.TOKEN = TOKEN;
        runBot(this.TOKEN);
    }

    public void runBot(String TOKEN) throws LoginException {
        MCD = new JDABuilder(AccountType.BOT).setToken(TOKEN).setStatus(OnlineStatus.DO_NOT_DISTURB).build();
        MCD.addEventListener(this);
        MCD.getPresence().setActivity(Activity.of(ActivityType.WATCHING, "Minecraft Chat"));
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) { //When message comes from discord
        try {
            // TODO: add !ign @Username to get ign of the player here, and !discord_username mc_username as well

            if (event.getMessage().getContentRaw().trim().startsWith("!sync ") && !event.getAuthor().isBot()) { //Handling the Verify Request
                ArrayList<ArrayList<String>> syncList = chatCommands.getCurrentSyncingMemberList();
                ArrayList<ArrayList<String>> arr = new ArrayList<>();
                for (ArrayList<String> args : syncList) {
                    String UUID = args.get(0);
                    String rnd = args.get(1);
                    if (event.getMessage().getContentRaw().replace("!sync ", "").trim().equals(rnd)) {
                        arr.add(args);
                        String toAdd = UUID + ":" + event.getAuthor().getId();
                        ArrayList<String> temp = chatCommands.getSyncedPeopleList();
                        temp.add(toAdd);
                        chatCommands.setSyncedPeopleList(temp);
                        sendMessageToDiscord(event.getAuthor().getAsMention() + " successfully synced!", true);
                        event.getMember().getRoles().add(event.getGuild().getRoleById(permId));
                    } else {
                        sendMessageToDiscord(event.getAuthor().getAsMention() + " bad code", true);
                    }
                }
                chatCommands.removeFromRequestList(arr); // Removing the request/s from queue
                for (ArrayList<String> arg : arr) {
                    Objects.requireNonNull(Bukkit.getPlayer(UUID.fromString(arg.get(0)))).sendRawMessage("Accounts had been synced, have fun!");
                }
            } else if (event.getChannel().getId().equals(channelId) && !event.getAuthor().isBot()) {
                server.broadcastMessage("[Discord] " + event.getAuthor().getName() + " : " + event.getMessage().getContentRaw()); // Mirroring Discord Chat to In-Game Chat
            }


        } catch (CommandException ex) {
            server.dispatchCommand(dcd, event.getMessage().getContentRaw().replace("!exec ", " ").trim());
            sendMessageToDiscord("Command run, but output is out of reach. Check console.", false);
        } catch (NumberFormatException ex) {
            lg.warning("PERM ID IS NOT RIGHT!");
        }
    }

    @Override
    public void onReady(ReadyEvent event) { //First Connection
        sendMessageToDiscord("AHHHHHHHHHHHHHHH", false);
        if (firstConnection) {
            MCD.getTextChannelById(channelId).sendMessage(serverStartMessage).queue();
            firstConnection = false;
            dcd = new DiscordConsoleDummy(this, this.main);
        } else if (sendByDiscordFullReload) {
            sendByDiscordFullReload = false;
            sendMessageToDiscord("Full Reload Successful!", false);
        }
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (Main.syncRoleID_GRANTED) {
            ArrayList<String> SyncedList = chatCommands.getSyncedPeopleList();
            for (String user : SyncedList) {
                String discordID = user.split(":")[1].trim();
                if (event.getMember().getId().trim().equals(discordID)) {
                    event.getGuild().addRoleToMember(event.getMember(), event.getJDA().getRoleById(main.getSyncRoleID())).queue();
                }
            }
        }
    }

    public void sendMessageToDiscord(String message, boolean send_in_bot_commands) {
        String[] MineCraftLanguageFilter = {"§a", "§b", "§c", "§d", "§e", "§f", "§k", "§l", "§m", "§n", "§o", "§r", "§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8", "§9"}; //Weird color thingies & next-back selections
        for (String f : MineCraftLanguageFilter)
            message = message.replaceAll(f, "");
        if (!message.isEmpty()) {
            if (message.contains("@everyone")) {
                MCD.getTextChannelById(channelId).sendMessage("someone tried to send a message with \"at everyone\".").queue();
            }
            if (send_in_bot_commands) {
                MCD.getTextChannelById(commandsChannelId).sendMessage(message).queue();
            } else {
                MCD.getTextChannelById(channelId).sendMessage(message).queue();
            }
        }
    }

    public void returnLogFromConsole(String message) {
        String[] MineCraftLanguageFilter = {"§a", "§b", "§c", "§d", "§e", "§f", "§k", "§l", "§m", "§n", "§o", "§r", "§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8", "§9"}; //Weird color thingies & next-back selections
        for (String f : MineCraftLanguageFilter)
            message = message.replaceAll(f, "");
        if (!message.isEmpty())
            MCD.getTextChannelById(commandsChannelId).sendMessage(message).queue();
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public void setCommandsChannelId(String commandsChannelId) {
        this.commandsChannelId = commandsChannelId;
    }

    public void setPermId(String permId) {
        this.permId = permId;
    }
}