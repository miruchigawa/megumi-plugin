package moe.nanakura.megumi.commands

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.nanakura.megumi.trakteer.TrakteerClient
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

class SupporterCommand(private val client: TrakteerClient) : BasicCommand {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun execute(
            source: io.papermc.paper.command.brigadier.CommandSourceStack,
            args: Array<out String>
    ) {
        val sender = source.sender
        val page = args.firstOrNull()?.toIntOrNull() ?: 1

        if (page < 1) {
            sender.sendMessage(Component.text("Page must be at least 1.", NamedTextColor.RED))
            return
        }

        scope.launch {
            try {
                val includes =
                        "is_guest,reply_message,net_amount,payment_method,order_id,supporter_email,updated_at_diff_label"
                val response = client.getSupports(limit = 5, page = page, include = includes)
                if (response.status == "success") {
                    val supports = response.result.data

                    if (supports.isEmpty()) {
                        sender.sendMessage(
                                Component.text(
                                        "No supporters found on page $page.",
                                        NamedTextColor.RED
                                )
                        )
                        return@launch
                    }

                    source.sender.sendMessage(
                            Component.text(
                                            "=== Trakteer Supporters (Page $page) ===",
                                            NamedTextColor.GOLD
                                    )
                                    .decoration(TextDecoration.BOLD, true)
                    )

                    supports.forEach { support ->
                        val supporter = support.supporterName ?: "Unknown"
                        val quantity = support.quantity ?: 0
                        val unit = support.unitName ?: "Units"
                        val amount = support.amount ?: 0

                        val line =
                                Component.text()
                                        .append(Component.text(supporter, NamedTextColor.AQUA))
                                        .append(Component.text(" supported ", NamedTextColor.WHITE))
                                        .append(
                                                Component.text(
                                                        "$quantity $unit",
                                                        NamedTextColor.GREEN
                                                )
                                        )
                                        .append(
                                                Component.text(
                                                        " ($amount IDR)",
                                                        NamedTextColor.GRAY
                                                )
                                        )

                        if (support.paymentMethod != null) {
                            line.append(Component.text(" via ", NamedTextColor.GRAY))
                                    .append(
                                            Component.text(
                                                    support.paymentMethod,
                                                    NamedTextColor.LIGHT_PURPLE
                                            )
                                    )
                        }

                        sender.sendMessage(line.build())

                        if (support.orderId != null) {
                            sender.sendMessage(
                                    Component.text(
                                            "  Order ID: ${support.orderId}",
                                            NamedTextColor.DARK_GRAY
                                    )
                            )
                        }

                        if (!support.supportMessage.isNullOrBlank()) {
                            // Sanitize: remove newlines to prevent chat
                            // rendering issues
                            val sanitizedMessage =
                                    support.supportMessage.replace("\n", " ").replace("\r", " ")
                            sender.sendMessage(
                                    Component.text(
                                                    "  \"$sanitizedMessage\"",
                                                    NamedTextColor.LIGHT_PURPLE
                                            )
                                            .decoration(TextDecoration.ITALIC, true)
                            )
                        }
                    }

                    sender.sendMessage(
                            Component.text(
                                    "Type /supporter ${page + 1} for more.",
                                    NamedTextColor.GRAY
                            )
                    )
                } else {
                    sender.sendMessage(
                            Component.text(
                                    "Failed to fetch supporters: ${response.message}",
                                    NamedTextColor.RED
                            )
                    )
                }
            } catch (ex: Exception) {
                sender.sendMessage(
                        Component.text("An error occurred: ${ex.message}", NamedTextColor.RED)
                )
                ex.printStackTrace()
            }
        }
    }
}
