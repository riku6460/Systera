package net.synchthia.systera.stream;

import net.synchthia.api.systera.SysteraProtos;
import net.synchthia.systera.APIClient;
import net.synchthia.systera.SysteraPlugin;
import net.synchthia.systera.player.SysteraPlayer;
import net.synchthia.systera.settings.Settings;
import net.synchthia.systera.util.StringUtil;
import redis.clients.jedis.JedisPubSub;

import java.util.logging.Level;

public class ChatSubs extends JedisPubSub {
    private static final SysteraPlugin plugin = SysteraPlugin.getInstance();

    @Override
    public void onPMessage(String pattern, String channel, String message) {
        SysteraProtos.ChatStream stream = APIClient.chatStreamFromJson(message);
        assert stream != null;
        switch (stream.getType()) {
            case CHAT:
                SysteraProtos.ChatEntry chatEntry = stream.getChatEntry();
                if (!SysteraPlugin.isEnableGlobalChat()) {
                    return;
                }

                if (chatEntry.getServerName().equals(SysteraPlugin.getServerId())) {
                    return;
                }

                // Format
                String fmt = StringUtil.coloring(String.format("&7[%s]&7%s&a:&r ", chatEntry.getServerName(), chatEntry.getAuthor().getName()));
                fmt += chatEntry.getMessage();

                // Log
                plugin.getServer().getConsoleSender().sendMessage(String.format("[GlobalChat] %s", fmt));

                // Send to Player
                String finalFmt = fmt;
                plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getServer().getOnlinePlayers().forEach(player -> {
                    SysteraPlayer sp = plugin.getPlayerStore().get(player.getUniqueId());
                    Settings settings = sp.getSettings();
                    if (settings.getGlobalChat().getValue() && sp.getIgnoreList().stream().noneMatch(x -> APIClient.toUUID(chatEntry.getAuthor().getUuid()).equals(APIClient.toUUID(x.getUuid())))) {
                        player.sendMessage(finalFmt);
                    }
                }));

                break;
        }
    }

    @Override
    public void onPSubscribe(String pattern, int subscribedChannels) {
        plugin.getLogger().log(Level.INFO, "P Subscribed : " + pattern);
    }

    @Override
    public void onPUnsubscribe(String pattern, int subscribedChannels) {
        plugin.getLogger().log(Level.INFO, "P UN Subscribed : " + pattern);
    }
}
