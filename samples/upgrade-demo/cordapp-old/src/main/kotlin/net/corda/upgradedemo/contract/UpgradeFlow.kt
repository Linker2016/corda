package net.corda.upgradedemo.contract

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.ContractUpgradeFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy

@StartableByRPC
class UpgradeFlow(private val counterpartyNode: Party) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val states = serviceHub.vaultService.queryBy<DemoAsset>().states

        states.forEach {

            val upgradeFlow = ContractUpgradeFlow.Initiate(it, DemoAssetContractV2::class.java)
            val result = subFlow(upgradeFlow)
            logger.info("Upgraded $it to $result")
        }
    }
}

@StartableByRPC
class MoveAllFlow2(private val counterpartyNode: Party) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val states = serviceHub.vaultService.queryBy<DemoAssetV2>().states

        states.forEach {
            // Move ownership of the asset to the counterparty
            val moveTx = serviceHub.signInitialTransaction(DemoAssetContractV2.move(it, counterpartyNode))
            subFlow(FinalityFlow(moveTx, setOf(counterpartyNode)))
        }
    }
}


//@StartableByRPC
//class UpgradeFlow(private val counterpartyNode: Party) : FlowLogic<Unit>() {
//    @Suspendable
//    override fun call() {
//        val states = serviceHub.vaultService.queryBy<DemoMultiOwnerState>().states
//
//        states.forEach {
//
//            val upgradeFlow = ContractUpgradeFlow.Initiate(it, DemoAssetContract2::class.java)
//            val result = subFlow(upgradeFlow)
//            logger.info("Upgraded $it to $result")
//        }
//    }
//}
//
//@StartableByRPC
//class AuthoriseFlow() : FlowLogic<Unit>() {
//    @Suspendable
//    override fun call() {
//        val states = serviceHub.vaultService.queryBy<DemoMultiOwnerState>().states
//
//        states.forEach {
//
//            val authorise = ContractUpgradeFlow.Authorise(it, DemoAssetContract2::class.java)
//            val result = subFlow(authorise)
//            logger.info("Authorised $it to $result")
//        }
//    }
//}
//
//
//@StartableByRPC
//class MoveAllFlow2(private val counterpartyNode: Party) : FlowLogic<Unit>() {
//    @Suspendable
//    override fun call() {
//        val states = serviceHub.vaultService.queryBy<DemoMultiOwnerState2>().states
//
//        states.forEach {
//            // Move ownership of the asset to the counterparty
//            val moveTx = serviceHub.signInitialTransaction(DemoAssetContract2.move(it, counterpartyNode))
//            subFlow(FinalityFlow(moveTx, setOf(counterpartyNode)))
//        }
//    }
//}