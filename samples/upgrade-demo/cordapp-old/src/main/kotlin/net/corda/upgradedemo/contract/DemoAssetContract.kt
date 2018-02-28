package net.corda.upgradedemo.contract

import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.TransactionBuilder


interface DemoState : ContractState {
    val value: Int
}

data class DemoAsset(override val value: Int, override val owner: AbstractParty) : DemoState, OwnableState {
    override val participants: List<AbstractParty>
        get() = listOf(owner)

    override fun withNewOwner(newOwner: AbstractParty) = CommandAndState(DemoAssetContract.Commands.Move(), copy(owner = newOwner))
}


data class DemoMultiOwnerState(override val value: Int = 0,
                               val owners: List<AbstractParty>) : DemoState, ContractState {
    override val participants: List<AbstractParty> get() = owners
}


data class DemoAssetContract(val blank: Any? = null) : Contract {
    interface Commands : CommandData {
        class Create : TypeOnlyCommandData(), Commands
        class Move : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        // Always accepts.

    }

    companion object {
        const val PROGRAM_ID: ContractClassName = "net.corda.upgradedemo.contract.DemoAssetContract"

        @JvmStatic
        fun generateInitial(magicNumber: Int, notary: Party, owner: Party, vararg otherOwners: Party): TransactionBuilder {
            val owners = (listOf(owner) + otherOwners)

            val state: DemoState = if (owners.size ==1) {
                DemoAsset(magicNumber, owners.first())
            } else DemoMultiOwnerState(magicNumber, owners)

            return TransactionBuilder(notary)
                    .addCommand(Command(Commands.Create(), owners.first().owningKey))
                    .addOutputState(state, PROGRAM_ID)
//                    .addOutputState(state, PROGRAM_ID, WhitelistedByZoneAttachmentConstraint)
        }

        @JvmStatic
        fun move(prior: StateAndRef<DemoState>, newOwner: AbstractParty): TransactionBuilder {
            val priorState = prior.state.data
            return TransactionBuilder(notary = prior.state.notary)
                    .addInputState(prior)
                    .addCommand(Command(DemoAssetContract.Commands.Move(), priorState.participants.map { it.owningKey }))
                    .addOutputState(priorState, PROGRAM_ID, prior.state.constraint)
        }
    }
}


//data class DemoAsset2(override val value: Int, override val owner: AbstractParty) : DemoState, OwnableState {
//    override val participants: List<AbstractParty>
//        get() = listOf(owner)
//
//    override fun withNewOwner(newOwner: AbstractParty) = CommandAndState(DemoAssetContract.Commands.Move(), copy(owner = newOwner))
//}

//data class DemoMultiOwnerState2(override val value: Int = 0,
//                               val owners: List<AbstractParty>) : DemoState, ContractState {
//    val newValue = value * 2
//    override val participants: List<AbstractParty> get() = owners
//}
//
//data class DemoAssetContract2(val blank: Any? = null) : UpgradedContractWithLegacyConstraint<DemoMultiOwnerState, DemoMultiOwnerState2> {
//    override val legacyContract: ContractClassName
//        get() = DemoAssetContract.PROGRAM_ID
//
//    override fun upgrade(state: DemoMultiOwnerState): DemoMultiOwnerState2 {
//        return DemoMultiOwnerState2(state.value, state.owners)
//    }
//
//    override val legacyContractConstraint: AttachmentConstraint
//        get() = HashAttachmentConstraint(SecureHash.parse("E02BD2B9B010BBCE49C0D7C35BECEF2C79BEB2EE80D902B54CC9231418A4FA0C"))
//
//    interface Commands : CommandData {
//        class Create : TypeOnlyCommandData(), Commands
//        class Move : TypeOnlyCommandData(), Commands
//    }
//
//    override fun verify(tx: LedgerTransaction) {
//        // Always accepts.
//
//    }
//
//    companion object {
//        const val PROGRAM_ID: ContractClassName = "net.corda.upgradedemo.contract.DemoAssetContract2"
//
//        @JvmStatic
//        fun generateInitial(magicNumber: Int, notary: Party, owner: Party, vararg otherOwners: Party): TransactionBuilder {
//            val owners = (listOf(owner) + otherOwners)
//
//            val state: DemoState = if (owners.size ==1) {
//                DemoAsset2(magicNumber, owners.first())
//            } else DemoMultiOwnerState2(magicNumber, owners)
//
//            return TransactionBuilder(notary)
//                    .addCommand(Command(Commands.Create(), owners.first().owningKey))
//                    .addOutputState(state, PROGRAM_ID)
////                    .addOutputState(state, PROGRAM_ID, WhitelistedByZoneAttachmentConstraint)
//        }
//
//        @JvmStatic
//        fun move(prior: StateAndRef<DemoState>, newOwner: AbstractParty): TransactionBuilder {
//            val priorState = prior.state.data
//            return TransactionBuilder(notary = prior.state.notary)
//                    .addInputState(prior)
//                    .addCommand(Command(Commands.Move(), priorState.participants.map { it.owningKey }))
//                    .addOutputState(priorState, PROGRAM_ID, prior.state.constraint)
//        }
//    }
//}
//
