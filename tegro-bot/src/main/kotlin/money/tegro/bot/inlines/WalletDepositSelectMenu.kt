package money.tegro.bot.inlines

import money.tegro.bot.api.Bot
import money.tegro.bot.objects.BotMessage
import money.tegro.bot.objects.MessagesContainer
import money.tegro.bot.objects.User
import money.tegro.bot.objects.keyboard.BotKeyboard
import money.tegro.bot.utils.button
import money.tegro.bot.wallet.CryptoCurrency
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
class WalletDepositSelectMenu(
    val user: User,
    val parentMenu: Menu
) : Menu {
    override suspend fun sendKeyboard(bot: Bot, lastMenuMessageId: Long?) {
        bot.updateKeyboard(
            to = user.vkId ?: user.tgId ?: 0,
            lastMenuMessageId = lastMenuMessageId,
            message = MessagesContainer[user.settings.lang].menuWalletDepositSelectMessage,
            keyboard = BotKeyboard {
                row {
                    button("TON", ButtonPayload.serializer(), ButtonPayload.TON)
                }
                row {
                    button("TGR", ButtonPayload.serializer(), ButtonPayload.TGR)
                }
                row {
                    button("USDT", ButtonPayload.serializer(), ButtonPayload.USDT)
                }
                row {
                    button(
                        MessagesContainer[user.settings.lang].menuButtonBack,
                        WalletMenu.ButtonPayload.serializer(),
                        WalletMenu.ButtonPayload.BACK
                    )
                }
            }
        )
    }

    override suspend fun handleMessage(bot: Bot, message: BotMessage): Boolean {
        val payload = message.payload ?: return false
        when (Json.decodeFromString<ButtonPayload>(payload)) {

            ButtonPayload.TON -> user.setMenu(
                bot,
                WalletDepositMenu(user, CryptoCurrency.TON, this),
                message.lastMenuMessageId
            )

            ButtonPayload.TGR -> user.setMenu(
                bot,
                WalletDepositMenu(user, CryptoCurrency.TGR, this),
                message.lastMenuMessageId
            )

            ButtonPayload.USDT -> user.setMenu(
                bot,
                WalletDepositMenu(user, CryptoCurrency.USDT, this),
                message.lastMenuMessageId
            )

            ButtonPayload.BACK -> {
                user.setMenu(bot, parentMenu, message.lastMenuMessageId)
            }
        }
        return true
    }

    @Serializable
    enum class ButtonPayload {
        TON,
        TGR,
        USDT,
        BACK
    }
}