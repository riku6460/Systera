package net.synchthia.systera.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.synchthia.systera.APIClient;
import net.synchthia.systera.SysteraPlugin;
import net.synchthia.systera.chat.Japanize;
import net.synchthia.systera.i18n.I18n;
import net.synchthia.systera.player.SysteraPlayer;
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
        Component componentMessage = Component.text(message).color(NamedTextColor.WHITE);

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
                componentMessage = Component.text(converted).color(NamedTextColor.WHITE)
                        .append(Component.text(" (" + message + ")").color(NamedTextColor.GRAY));

            }
        }

        sender.sendMessage(
                I18n.getComponent(sender, "chat.tell.send", Placeholder.unparsed("_player_from_", sender.getName()), Placeholder.unparsed("_player_to_", target.getName())).append(Component.space()).append(componentMessage)
        );


        if ((sender instanceof Player)) {
            Player player = ((Player) sender);
            if (targetSP.getIgnoreList().stream().noneMatch(pi -> APIClient.toUUID(pi.getUuid()).equals(player.getUniqueId()))) {
                sender.sendMessage(
                        I18n.getComponent(sender, "chat.tell.receive", Placeholder.unparsed("_player_from_", sender.getName()), Placeholder.unparsed("_player_to_", target.getName())).append(Component.space()).append(componentMessage)
                );
            } else {
                return;
            }
        }

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.hasPermission("systera.spy") && !p.getName().equals(sender.getName()) && !p.getName().equals(target.getName())) {
                p.sendMessage(
                        I18n.getComponent(sender, "chat.tell.spy", Placeholder.unparsed("_player_from_", sender.getName()), Placeholder.unparsed("_player_to_", target.getName())).append(Component.space()).append(componentMessage)
                );
            }
        }

        // プレイヤーからの場合のみメタデータをセットする
        if ((sender instanceof Player)) {
            target.setMetadata("reply", new FixedMetadataValue(plugin, sender));
        }
    }
}
