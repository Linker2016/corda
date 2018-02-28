package net.corda.upgradedemo.contract

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy

@StartableByRPC
class IssueFlow(private val notary: Party, private val counterpartyNode: Party, private val value: Int) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Self issue an asset
        val issueTx = serviceHub.signInitialTransaction(
                DemoAssetContract.generateInitial(value, notary, ourIdentity)
        )
        subFlow(FinalityFlow(issueTx))
    }
}

@StartableByRPC
class MoveAllFlow(private val counterpartyNode: Party) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val states = serviceHub.vaultService.queryBy<DemoState>().states

        states.forEach {
            // Move ownership of the asset to the counterparty
            val moveTx = serviceHub.signInitialTransaction(DemoAssetContract.move(it, counterpartyNode))
            subFlow(FinalityFlow(moveTx, setOf(counterpartyNode)))
        }
    }
}