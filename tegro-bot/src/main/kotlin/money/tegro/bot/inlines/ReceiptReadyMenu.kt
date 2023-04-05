package money.tegro.bot.inlines

import io.github.g0dkar.qrcode.QRCode
import io.github.g0dkar.qrcode.render.Colors
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import money.tegro.bot.api.Bot
import money.tegro.bot.api.TgBot
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.Messages
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.receipts.PostgresReceiptPersistent
import money.tegro.bot.receipts.Receipt
import money.tegro.bot.utils.button
import money.tegro.bot.utils.linkButton
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class ReceiptReadyMenu(
    val user: User,
    val receipt: Receipt,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = getBody(bot),
            keyboard = getKeyboard(bot, true)
        )
    }

    private fun getBody(bot: Bot): String {
        val code = receipt.id.toString()
        val tgLink = String.format("t.me/%s?start=RC-%s", System.getenv("TG_USER_NAME"), code)
        val vkLink = String.format("https://vk.com/write-%s?ref=RC-%s", System.getenv("VK_GROUP_ID"), code)
        val date = Date.from(receipt.issueTime.toJavaInstant())
        val time =
            SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(date)
        return Messages[user.settings.lang].menuReceiptReadyMessage.format(
            receipt.coins,
            receipt.activations,
            time,
            if (bot is TgBot) tgLink else vkLink
        )
    }

    private fun getKeyboard(bot: Bot, qrButton: Boolean): BotKeyboard {
        val code = receipt.id.toString()
        val tgLink = String.format("t.me/%s?start=RC-%s", System.getenv("TG_USER_NAME"), code)
        val vkLink = String.format("https://vk.com/write-%s?ref=RC-%s", System.getenv("VK_GROUP_ID"), code)
        return BotKeyboard {
            row {
                linkButton(
                    Messages[user.settings.lang].menuReceiptReadyShare,
                    if (bot is TgBot) tgLink else vkLink,
                    ButtonPayload.serializer(),
                    ButtonPayload.SHARE
                )
            }
            if (qrButton) {
                row {
                    button(
                        Messages[user.settings.lang].menuReceiptReadyQr,
                        ButtonPayload.serializer(),
                        ButtonPayload.QR
                    )
                }
            }
            row {
                button(
                    Messages[user.settings.lang].menuReceiptReadyLimitations,
                    ButtonPayload.serializer(),
                    ButtonPayload.LIMITATIONS
                )
            }
            row {
                button(
                    Messages[user.settings.lang].menuReceiptReadyDelete,
                    ButtonPayload.serializer(),
                    ButtonPayload.DELETE
                )
            }
            row {
                button(
                    Messages[user.settings.lang].menuButtonBack,
                    ButtonPayload.serializer(),
                    ButtonPayload.BACK
                )
            }
        }
    }

    override suspend fun handleMessage(bot: Bot, message: BotMessage): Boolean {
        val payload = message.payload ?: return false
        val code = receipt.id.toString()
        val tgLink = String.format("t.me/%s?start=RC-%s", System.getenv("TG_USER_NAME"), code)
        val vkLink = String.format("https://vk.com/write-%s?ref=RC-%s", System.getenv("VK_GROUP_ID"), code)
        when (Json.decodeFromString<ButtonPayload>(payload)) {

            ButtonPayload.SHARE -> TODO()
            ButtonPayload.QR -> {

                val filename = "qr-$code.png"

                val darkColor = Colors.css("#0D1117")
                val lightColor = Colors.css("#8B949E")

                val qrCodeCanvas =
                    QRCode(if (bot is TgBot) tgLink else vkLink).render(darkColor = lightColor, brightColor = darkColor)
                val imageBytes = qrCodeCanvas.getBytes()

                bot.sendPhoto(
                    message.peerId,
                    getBody(bot),
                    ByteArrayInputStream(imageBytes),
                    filename,
                    null
                )

                val list = PostgresReceiptPersistent.loadReceipts(user).filter { it.isActive }
                user.setMenu(bot, ReceiptsListMenu(user, list.toMutableList(), 1, this), message.lastMenuMessageId)

                /*
                bot.deleteMessage(message.peerId, message.messageId)
                bot.sendPhoto(
                    message.peerId,
                    getBody(bot),
                    ByteArrayInputStream(imageBytes),
                    filename,
                    getKeyboard(bot, false)
                )
                 */
            }

            ButtonPayload.LIMITATIONS -> user.setMenu(
                bot,
                ReceiptLimitationsMenu(user, receipt, this),
                message.lastMenuMessageId
            )

            ButtonPayload.DELETE -> {
                PostgresReceiptPersistent.deleteReceipt(receipt)
                bot.sendMessage(message.peerId, Messages[user.settings.lang].menuReceiptDeleted)
                user.setMenu(bot, ReceiptsMenu(user, MainMenu(user)), message.lastMenuMessageId)
            }

            ButtonPayload.BACK -> {
                val list = PostgresReceiptPersistent.loadReceipts(user).filter { it.isActive }
                user.setMenu(bot, ReceiptsListMenu(user, list.toMutableList(), 1, this), message.lastMenuMessageId)
            }
        }
        return true
    }

    @Serializable
    private enum class ButtonPayload {
        SHARE,
        QR,
        LIMITATIONS,
        DELETE,
        BACK
    }
}