package com.mobgrab.compat;

import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import org.bukkit.Location;
import org.bukkit.entity.Player;

final class PlotSquaredHook implements ProtectionHook {

    @Override
    public boolean canBuild(Player player, Location location) {
        var psLoc = BukkitUtil.adapt(location);
        PlotArea area = PlotSquared.get().getPlotAreaManager().getPlotArea(psLoc);
        if (area == null) return true;

        Plot plot = area.getPlot(psLoc);
        if (plot == null) return false;
        if (!plot.hasOwner()) return false;
        return plot.isAdded(player.getUniqueId());
    }

    @Override
    public String getName() {
        return "PlotSquared";
    }
}
