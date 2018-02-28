import com.google.common.io.BaseEncoding
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.identity.PartyAndCertificate
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.serialization.deserialize
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.testing.core.BOB_NAME
import net.corda.upgradedemo.contract.IssueFlow
import net.corda.upgradedemo.contract.MoveAllFlow
import net.corda.upgradedemo.contract.MoveAllFlow2
import net.corda.upgradedemo.contract.UpgradeFlow
import net.corda.upgradedemo.demoUser
import javax.xml.bind.DatatypeConverter


fun main(args: Array<String>) {
//    runBobNode()


    runAliceNode()
}

private fun runAliceNode() {
    val address = NetworkHostAndPort("localhost", 10006)
    println("Connecting to the recipient node ($address)")
//    CordaRPCClient(address).start(demoUser.username, demoUser.password).use {
    CordaRPCClient(address).start("guest", "letmein").use {
        val clientApi = UpgradeDemoClientApi(it.proxy)
//        clientApi.issue(10)
//        clientApi.issue(11)
//        clientApi.issue(12)

//        clientApi.rpc.

//        clientApi.moveAll()

//        val snapshot = it.proxy.internalVerifiedTransactionsSnapshot()

//        it.proxy.

//        println(snapshot.map { it.coreTransaction.id })
//
//        clientApi.upgrade()
        clientApi.moveUpgraded()
    }
}

private fun runBobNode() {
    val address = NetworkHostAndPort("localhost", 10006)
    println("Connecting to the recipient node ($address)")
    CordaRPCClient(address).start(demoUser.username, demoUser.password).use {
        val clientApi = UpgradeDemoClientApi(it.proxy)
//        clientApi.authorise()

//        clientApi.rpc.

//        clientApi.moveAll()
//        clientApi.upgrade()
//        clientApi.moveUpgraded()
    }
}

private class UpgradeDemoClientApi(val rpc: CordaRPCOps) {
    private val notary by lazy {
        val id = rpc.notaryIdentities().singleOrNull()
        checkNotNull(id) { "No unique notary identity, try cleaning the node directories." }
    }
    private val me = rpc.nodeInfo().legalIdentities.first()
    private val counterparty by lazy {
        val parties = rpc.networkMapSnapshot()
        parties.last().legalIdentities.first()
    }

    fun issue(value: Int) {
        rpc.startFlow(::IssueFlow, notary, counterparty, value).returnValue.get()
    }

    fun moveAll() {
        rpc.startFlow(::MoveAllFlow, me).returnValue.get()
    }

    fun upgrade() {
        rpc.startFlow(::UpgradeFlow, counterparty)
    }
//
//    fun authorise() {
//        rpc.startFlow(::AuthoriseFlow)
//    }


    fun moveUpgraded() {
        rpc.startFlow(::MoveAllFlow2, counterparty)
    }
}
