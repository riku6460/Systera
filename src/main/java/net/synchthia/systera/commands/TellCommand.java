package net.synchthia.systera.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import lombok.RequiredArgsConstructor;
import net.synchthia.systera.APIClient;
import net.synchthia.systera.SysteraPlugin;
import net.synchthia.systera.chat.Japanize;
import net.synchthia.systera.i18n.I18n;
import net.synchthia.systera.player.SysteraPlayer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

@RequiredArgsConstructor
public class TellCommand extends BaseCommand {
    private final SysteraPlugin plugin;

    @CommandAlias("tell|msg|message|pm|privatemessage|w|whisper")
    @CommandPermission("systera.command.tell")
    @CommandCompletion("@players")
    @Description("Tell Command")
    public void onTell(CommandSender sender, String target, String message) {
        Player targetPlayer = plugin.getServer().getPlayer(target);
        if (targetPlayer == null) {
            I18n.sendMessage(sender, "player.error.not_found");
            return;
        }

        sendTellMsg(sender, targetPlayer, message);
    }

    @CommandAlias("reply|r")
    @CommandPermission("systera.command.tell")
    @Description("Reply Command")
    public void onReply(Player sender, String message) {
        if (!sender.hasMetadata("reply")) {
            I18n.sendMessage(sender, "chat.error.not_received");
            return;
        }

        Player target = ((Player) sender.getMetadata("reply").get(0).value());
        sendTellMsg(sender, target, message);
    }

    private void sendTellMsg(CommandSender sender, Player target, String message) {
        if (target == null) {
            I18n.sendMessage(sender, "player.error.not_found");
            return;
        }

        SysteraPlayer targetSP = plugin.getPlayerStore().get(target.getUniqueId());

        // Vanish
        if (!sender.hasPermission("systera.vanish") && targetSP.getSettings().getVanish().getValue()) {
            I18n.sendMessage(sender, "player.error.not_found");
            return;
        }

        // Ignored
        if ((sender instanceof Player)) {
            SysteraPlayer senderSP = plugin.getPlayerStore().get(((Player) sender).getUniqueId());
            if (senderSP.getIgnoreList().stream().anyMatch(p -> APIClient.toUUID(p.getUuid()).equals(target.getUniqueId()))) {
                I18n.sendMessage(sender, "chat.error.cant_send_to_ignoring");
                return;
            }
        }

        // Japanize
        if ((sender instanceof Player) && plugin.getPlayerStore().get(((Player) sender).getUniqueId()).getSettings().getJapanize().getValue()) {
            Japanize japanize = new Japanize();
            String converted = japanize.convert(message);

            // converted
            if (converted != null && !converted.isEmpty()) {
                message = ChatColor.RESET + converted + ChatColor.GRAY + " (" + message + ChatColor.GRAY + ")";
            } else {
                message = ChatColor.RESET + message;
            }
        }

        I18n.sendMessage(sender, "chat.tell.send", sender.getName(), target.getName(), message);

        if ((sender instanceof Player)) {
            Player player = ((Player) sender);
            if (targetSP.getIgnoreList().stream().noneMatch(pi -> APIClient.toUUID(pi.getUuid()).equals(player.getUniqueId()))) {
                I18n.sendMessage(target, "chat.tell.receive", sender.getName(), target.getName(), message);
            } else {
                return;
            }
        }

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.hasPermission("systera.spy") && !p.getName().equals(sender.getName()) && !p.getName().equals(target.getName())) {
                I18n.sendMessage(p, "chat.tell.spy", sender.getName(), target.getName(), message);
            }
        }

        // プレイヤーからの場合のみメタデータをセットする
        if ((sender instanceof Player)) {
            target.setMetadata("reply", new FixedMetadataValue(plugin, sender));
        }
    }
}
