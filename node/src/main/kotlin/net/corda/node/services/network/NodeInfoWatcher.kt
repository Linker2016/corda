package net.corda.node.services.network

import net.corda.cordform.CordformNode
import net.corda.core.crypto.SecureHash
import net.corda.core.internal.*
import net.corda.core.node.NodeInfo
import net.corda.core.serialization.internal.SerializationEnvironmentImpl
import net.corda.core.serialization.internal._contextSerializationEnv
import net.corda.core.serialization.serialize
import net.corda.core.utilities.contextLogger
import net.corda.core.utilities.seconds
import net.corda.nodeapi.internal.NodeInfoAndSigned
import net.corda.nodeapi.internal.SignedNodeInfo
import net.corda.nodeapi.internal.network.NodeInfoFilesCopier
import net.corda.nodeapi.internal.serialization.AMQP_P2P_CONTEXT
import net.corda.nodeapi.internal.serialization.SerializationFactoryImpl
import net.corda.nodeapi.internal.serialization.amqp.AMQPServerSerializationScheme
import rx.Observable
import rx.Scheduler
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.streams.toList

/**
 * Class containing the logic to
 * - Serialize and de-serialize a [NodeInfo] to disk and reading it back.
 * - Poll a directory for new serialized [NodeInfo]
 *
 * @param nodePath the base path of a node.
 * @param pollInterval how often to poll the filesystem in milliseconds. Must be longer then 5 seconds.
 * @param scheduler a [Scheduler] for the rx [Observable] returned by [nodeInfoUpdates], this is mainly useful for
 *        testing. It defaults to the io scheduler which is the appropriate value for production uses.
 */
// TODO: Use NIO watch service instead?
class NodeInfoWatcher(private val nodePath: Path,
                      private val scheduler: Scheduler,
                      private val pollInterval: Duration = 5.seconds) {
    private val nodeInfoDirectory = nodePath / CordformNode.NODE_INFO_DIRECTORY
    private val processedNodeInfoFiles = mutableSetOf<Path>()
    private val _processedNodeInfoHashes = mutableSetOf<SecureHash>()
    val processedNodeInfoHashes: Set<SecureHash> get() = _processedNodeInfoHashes.toSet()

    companion object {
        private val logger = contextLogger()

        // TODO This method doesn't belong in this class
        fun saveToFile(path: Path, nodeInfoAndSigned: NodeInfoAndSigned) {
            // By using the hash of the node's first name we ensure:
            // 1) node info files for the same node map to the same filename and thus avoid having duplicate files for
            //    the same node
            // 2) avoid having to deal with characters in the X.500 name which are incompatible with the local filesystem
            val fileNameHash = nodeInfoAndSigned.nodeInfo.legalIdentities[0].name.serialize().hash
            nodeInfoAndSigned
                    .signed
                    .serialize()
                    .open()
                    .copyTo(path / "${NodeInfoFilesCopier.NODE_INFO_FILE_NAME_PREFIX}$fileNameHash", REPLACE_EXISTING)
        }
    }

    init {
        require(pollInterval >= 5.seconds) { "Poll interval must be 5 seconds or longer." }
        if (!nodeInfoDirectory.isDirectory()) {
            try {
                nodeInfoDirectory.createDirectories()
            } catch (e: IOException) {
                logger.info("Failed to create $nodeInfoDirectory", e)
            }
        }
    }

    /**
     * Read all the files contained in [nodePath] / [CordformNode.NODE_INFO_DIRECTORY] and keep watching
     * the folder for further updates.
     *
     * We simply list the directory content every 5 seconds, the Java implementation of WatchService has been proven to
     * be unreliable on MacOs and given the fairly simple use case we have, this simple implementation should do.
     *
     * @return an [Observable] returning [NodeInfo]s, at most one [NodeInfo] is returned for each processed file.
     */
    fun nodeInfoUpdates(): Observable<NodeInfo> {
        return Observable.interval(pollInterval.toMillis(), TimeUnit.MILLISECONDS, scheduler)
                .flatMapIterable { loadFromDirectory() }
    }

    // TODO This method doesn't belong in this class
    fun saveToFile(nodeInfoAndSigned: NodeInfoAndSigned) {
        return Companion.saveToFile(nodePath, nodeInfoAndSigned)
    }

    /**
     * Loads all the files contained in a given path and returns the deserialized [NodeInfo]s.
     * Signatures are checked before returning a value.
     *
     * @return a list of [NodeInfo]s
     */
    private fun loadFromDirectory(): List<NodeInfo> {
        if (!nodeInfoDirectory.isDirectory()) {
            return emptyList()
        }
        val result = nodeInfoDirectory.list { paths ->
            paths.filter { it !in processedNodeInfoFiles }
                    .filter { it.isRegularFile() }
                    .map { path ->
                        processFile(path)?.apply {
                            processedNodeInfoFiles.add(path)
                            _processedNodeInfoHashes.add(this.serialize().hash)
                        }
                    }
                    .toList()
                    .filterNotNull()
        }
        if (result.isNotEmpty()) {
            logger.info("Successfully read ${result.size} NodeInfo files from disk.")
        }
        return result
    }

    private fun processFile(file: Path): NodeInfo? {
        return try {
            logger.info("Reading NodeInfo from file: $file")
            val signedData = file.readObject<SignedNodeInfo>()
            signedData.verified()
        } catch (e: Exception) {
            logger.warn("Exception parsing NodeInfo from file. $file", e)
            null
        }
    }
}

// TODO Remove this once we have a tool that can read AMQP serialised files
fun main(args: Array<String>) {
    _contextSerializationEnv.set(SerializationEnvironmentImpl(
            SerializationFactoryImpl().apply {
                registerScheme(AMQPServerSerializationScheme())
            },
            AMQP_P2P_CONTEXT)
    )
    println(Paths.get(args[0]).readObject<SignedNodeInfo>().verified())
}
