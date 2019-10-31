package io.github.thedoctorone;

import javax.security.auth.login.LoginException;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;

/**
 * Hello world!
 *
 */
public class App extends JavaPlugin implements Listener {
    private DiscordCommunication dc;
    private String VERSION = "0.2";
    private String playerJoin = "&p just joined to server!";
    private String playerLeft = "&p just leaved the server!";
    private String ServerStart = "Server Started!";
    private String ServerStop = "Server Stopped!";
    private String channelID;
    private String TOKEN;
    String boldStart = "**";
    String squareParOpen = "[";
    String squareParClose = "]";
    String boldEnd = "**";

    @Override
    public void onEnable() {
        getLogger().info("Hello, Minecraft Connects to Discord is here! by Mahmut H. Kocas");
        try {
            ConfigThingies();
        } catch (IOException e) {
            getLogger().warning("CAN'T INTERACT WITH DISCORD'S CONFIG FILE!");
        }
        if(TOKEN != null)
            try {
                dc = new DiscordCommunication(getServer(), getLogger(), TOKEN, channelID, ServerStart);
                getServer().getPluginManager().registerEvents(this, this);
                new Thread(dc).run();
            } catch (LoginException | InterruptedException e) {
                getLogger().warning("Couldn't join to discord!");
            }
    }

    @Override
    public void onDisable() {
        dc.sendMessageToDiscord(ServerStop);
        getLogger().info("See you again, SpigotMC!");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if(!event.getMessage().startsWith("/")) {
            String user = event.getPlayer().getDisplayName();
            String message = event.getMessage();
            dc.sendMessageToDiscord(boldStart + squareParOpen + user + squareParClose + boldEnd + " " + message);
        }
    }

    @EventHandler
    public void playerJoinEvent(PlayerLoginEvent event) {
        String player = event.getPlayer().getDisplayName();
        dc.sendMessageToDiscord(boldStart + playerJoin.replace("&p", player) + boldEnd);
    }

    @EventHandler
    public void playerLeftEvent(PlayerQuitEvent event) {
        String player = event.getPlayer().getDisplayName();
        dc.sendMessageToDiscord(boldStart + playerLeft.replace("&p", player) + boldEnd);
    }

    @EventHandler
    public void ServerCommandEvent(ServerCommandEvent event) {
        String message = event.getCommand();
        String user = "Server";
        if(message.startsWith("say ")) {
            message = message.replace("say", " ").trim();
            dc.sendMessageToDiscord("*" + boldStart + squareParOpen + user + squareParClose + message + boldEnd + "* ");
        }
    }

    private void ConfigThingies () throws IOException {
        File folder = new File("discord");
        if(!folder.exists())
            folder.mkdir();

        File config = new File("discord/config.ini");
        if(!config.exists()) {
            config.createNewFile();
            createConfigFile(config);
            return;
        }
        FileReader fr = new FileReader(config); //Reading the config
        BufferedReader br = new BufferedReader(fr);
        String temp = "";
        while((temp = br.readLine()) != null) {
            if(temp.startsWith("Discord Bot Token=")) {
                TOKEN = temp.replaceFirst("Discord Bot Token="," ").trim();
            }
            if(temp.startsWith("Discord Channel ID=")) {
                channelID = temp.replaceFirst("Discord Channel ID="," ").trim();
            }
            if(temp.startsWith("Player Join Message=")) {
                playerJoin = temp.replaceFirst("Player Join Message="," ").trim();
            }
            if(temp.startsWith("Player Disconnect Message=")) {
                playerLeft = temp.replaceFirst("Player Disconnect Message="," ").trim();
            }
            if(temp.startsWith("Server Start Message=")) {
                ServerStart = temp.replaceFirst("Server Start Message="," ").trim();
            }
            if(temp.startsWith("Server Stop Message=")) {
                ServerStop = temp.replaceFirst("Server Stop Message="," ").trim();
            }
            if(temp.startsWith("CFG-VERSION=")) {
                if(!temp.replaceFirst("CFG-VERSION="," ").trim().equals(VERSION)) {
                    createConfigFile(config);
                    TOKEN = null;
                }
            }
        }
        br.close();
        fr.close();
    }

    private void createConfigFile (File config) throws IOException {
        FileWriter fw = new FileWriter(config); //Creating the config
        BufferedWriter bw = new BufferedWriter(fw);
        String configFile = "" +
                "Discord Bot Token= ENTER YOUR TOKEN HERE\n" +
                "Discord Channel ID= ENTER YOUR CHANNEL ID HERE\n" +
                "Player Join Message= &p just joined to server!\n" +
                "Player Disconnect Message= &p just leaved the server!\n" +
                "Server Start Message= Server Started!\n" +
                "Server Stop Message= Server Stopped!\n" +
                "CFG-VERSION= 0.2";
        fw.write(configFile);
        bw.close();
        fw.close();
    }
}
