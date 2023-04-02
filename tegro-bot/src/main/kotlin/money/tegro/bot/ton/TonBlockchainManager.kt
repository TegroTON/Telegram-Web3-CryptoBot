package money.tegro.bot.ton

import com.github.benmanes.caffeine.cache.AsyncCacheLoader
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import money.tegro.bot.blockchain.BlockchainManager
import money.tegro.bot.utils.base64
import money.tegro.bot.wallet.BlockchainType
import money.tegro.bot.wallet.Coins
import money.tegro.bot.wallet.CryptoCurrency
import org.ton.api.exception.TvmException
import org.ton.api.liteserver.LiteServerDesc
import org.ton.api.pk.PrivateKeyEd25519
import org.ton.api.pub.PublicKeyEd25519
import org.ton.block.*
import org.ton.cell.Cell
import org.ton.cell.buildCell
import org.ton.lite.client.LiteClient
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object TonBlockchainManager : BlockchainManager {
    @OptIn(DelicateCoroutinesApi::class)
    val liteClient = LiteClient(
        newSingleThreadContext("lite-client"), LiteServerDesc(
            id = PublicKeyEd25519(base64("n4VDnSCUuSpjnCyUk9e3QOOd6o0ItSWYbTnW3Wnn8wk=")),
            ip = 84478511,
            port = 19949
        )
    )

    override val type: BlockchainType
        get() = BlockchainType.TON

    private val jettonMasterContractCache = Caffeine.newBuilder()
        .build<MsgAddressInt, JettonMasterContract> { address ->
            JettonMasterContract(liteClient, address)
        }

    private val jettonWalletContractCache = Caffeine.newBuilder()
        .maximumSize(1_000)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .buildAsync(AsyncCacheLoader<Pair<JettonMasterContract, MsgAddressInt>, JettonWalletContract> { (jettonMaster, ownerAddress), executor ->
            GlobalScope.async(executor.asCoroutineDispatcher()) {
                val walletAddress = jettonMaster.getWalletAddress(ownerAddress)
                JettonWalletContract(liteClient, walletAddress)
            }.asCompletableFuture()
        })

    fun getAddress(privateKey: PrivateKeyEd25519): String {
        return getAddrStd(privateKey).toString(userFriendly = true, bounceable = false)
    }

    override fun getAddress(privateKey: ByteArray): String =
        getAddress(PrivateKeyEd25519(privateKey))

    override suspend fun getBalance(address: String): Coins {
        val addrStd = AddrStd(address)
        val contract = WalletV3Contract(liteClient, addrStd)
        val balance = contract.balance().coins.amount.value
        return Coins(CryptoCurrency.TON, balance)
    }

    override suspend fun getTokenBalance(cryptoCurrency: CryptoCurrency, ownerAddress: String): Coins {
        val tokenAddress = requireNotNull(cryptoCurrency.getTokenContractAddress(type)) {
            "$cryptoCurrency not support $type"
        }
        val jettonMasterContract = jettonMasterContractCache[AddrStd(tokenAddress)]
        val jettonWalletContract = jettonWalletContractCache[jettonMasterContract to AddrStd(ownerAddress)].await()
        val walletData = try {
            jettonWalletContract.getWalletData()
        } catch (e: TvmException) {
            null
        }
        return Coins(cryptoCurrency, walletData?.balance?.amount?.value ?: BigInteger.ZERO)
    }

    override suspend fun transfer(privateKey: ByteArray, destinationAddress: String, value: Coins) {
        val pk = PrivateKeyEd25519(privateKey)
        val walletContract = WalletV3Contract(liteClient, getAddrStd(pk))
        walletContract.transfer(pk) {
            destination = AddrStd(destinationAddress)
            coins = Coins(VarUInteger(value.amount))
        }
    }

    override suspend fun transferToken(
        privateKey: ByteArray,
        cryptoCurrency: CryptoCurrency,
        destinationAddress: String,
        value: Coins
    ) {
        val pk = PrivateKeyEd25519(privateKey)
        val ownerAddress = getAddrStd(pk)
        val tokenAddress = requireNotNull(cryptoCurrency.getTokenContractAddress(type)) {
            "$cryptoCurrency not support $type"
        }
        val jettonMasterContract = jettonMasterContractCache[AddrStd(tokenAddress)]
        val jettonWalletContract = jettonWalletContractCache[jettonMasterContract to ownerAddress].await()

        println(jettonWalletContract.address)
        val walletContract = WalletV3Contract(liteClient, ownerAddress)
        walletContract.transfer(pk) {
            destination = jettonWalletContract.address
            coins = org.ton.block.Coins.of(0.1)
            body = buildCell {
                JettonTransfer.storeTlb(
                    this, JettonTransfer(
                        queryId = 0,
                        amount = Coins(value.amount),
                        destination = AddrStd(destinationAddress),
                        responseDestination = ownerAddress,
                        customPayload = Maybe.of(null),
                        forwardTonAmount = Coins(0),
                        forwardPayload = Either.of(Cell("00000000"), null)
                    )
                )
            }
        }
    }

    private fun getAddrStd(privateKey: PrivateKeyEd25519): AddrStd = WalletV3Contract.getAddress(privateKey)
}

suspend fun main() {
    val pk = Random(123123).nextBytes(32)
    val address = TonBlockchainManager.getAddress(pk)
    println(address)
    println(TonBlockchainManager.getBalance(address))
    println(TonBlockchainManager.getTokenBalance(CryptoCurrency.USDT, address))

    TonBlockchainManager.transferToken(
        privateKey = Random(123123).nextBytes(32),
        cryptoCurrency = CryptoCurrency.USDT,
        destinationAddress = "EQAKtVj024T9MfYaJzU1xnDAkf_GGbHNu-V2mgvyjTuP6rvC",
        value = Coins(currency = CryptoCurrency.USDT, amount = 0.1.toBigDecimal())
    )
}