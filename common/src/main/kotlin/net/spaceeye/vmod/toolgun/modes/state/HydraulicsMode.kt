package net.spaceeye.vmod.toolgun.modes.state

import dev.architectury.networking.NetworkManager
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.spaceeye.vmod.constraintsManaging.addFor
import net.spaceeye.vmod.constraintsManaging.makeManagedConstraint
import net.spaceeye.vmod.constraintsManaging.types.HydraulicsMConstraint
import net.spaceeye.vmod.networking.C2SConnection
import net.spaceeye.vmod.rendering.types.A2BRenderer
import net.spaceeye.vmod.toolgun.modes.BaseMode
import net.spaceeye.vmod.toolgun.modes.gui.HydraulicsGUIBuilder
import net.spaceeye.vmod.toolgun.modes.inputHandling.HydraulicsCRIHandler
import net.spaceeye.vmod.toolgun.modes.serializing.HydraulicsSerializable
import net.spaceeye.vmod.toolgun.modes.util.PositionModes
import net.spaceeye.vmod.toolgun.modes.util.serverRaycast2PointsFnActivation
import net.spaceeye.vmod.toolgun.modes.util.serverRaycastAndActivate
import net.spaceeye.vmod.utils.RaycastFunctions
import java.awt.Color

class HydraulicsMode: BaseMode, HydraulicsSerializable, HydraulicsCRIHandler, HydraulicsGUIBuilder{
    var compliance: Double = 1e-20
    var maxForce: Double = 1e10
    var width: Double = .2

    var extensionDistance: Double = 5.0
    var extensionSpeed: Double = 1.0

    var channel: String = "hydraulics"

    var posMode = PositionModes.NORMAL

    var primaryFirstRaycast = false


    val conn_primary = register { object : C2SConnection<HydraulicsMode>("hydraulics_mode_primary", "toolgun_command") { override fun serverHandler(buf: FriendlyByteBuf, context: NetworkManager.PacketContext) = serverRaycastAndActivate<HydraulicsMode>(context.player, buf, ::HydraulicsMode) { item, serverLevel, player, raycastResult -> item.activatePrimaryFunction(serverLevel, player, raycastResult) } } }

    var previousResult: RaycastFunctions.RaycastResult? = null
    fun activatePrimaryFunction(level: Level, player: Player, raycastResult: RaycastFunctions.RaycastResult) = serverRaycast2PointsFnActivation(posMode, level, raycastResult, { if (previousResult == null || primaryFirstRaycast) { previousResult = it; Pair(false, null) } else { Pair(true, previousResult) } }, ::resetState) {
            level, shipId1, shipId2, ship1, ship2, spoint1, spoint2, rpoint1, rpoint2, prresult, rresult ->

        val dist = (rpoint1 - rpoint2).dist()
        level.makeManagedConstraint(HydraulicsMConstraint(
            spoint1, spoint2, rpoint1, rpoint2,
            ship1, ship2, shipId1, shipId2,
            compliance, maxForce,
            dist,
            dist + extensionDistance,
            extensionSpeed,
            channel,
            listOf(prresult.blockPosition, rresult.blockPosition),
            A2BRenderer(
                ship1 != null,
                ship2 != null,
                spoint1, spoint2,
                Color(62, 62, 200),
                width
            )
        )).addFor(player)

        resetState()
    }

    override fun resetState() {
        previousResult = null
        primaryFirstRaycast = false
    }
}