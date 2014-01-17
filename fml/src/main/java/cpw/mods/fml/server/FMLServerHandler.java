/*
 * The FML Forge Mod Loader suite. Copyright (C) 2012 cpw
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package cpw.mods.fml.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.network.INetHandler;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StringTranslate;

import com.google.common.collect.ImmutableList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.IFMLSidedHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;

/**
 * Handles primary communication from hooked code into the system
 *
 * The FML entry point is {@link #beginServerLoading(MinecraftServer)} called from
 * {@link net.minecraft.server.dedicated.DedicatedServer}
 *
 * Obfuscated code should focus on this class and other members of the "server"
 * (or "client") code
 *
 * The actual mod loading is handled at arms length by {@link Loader}
 *
 * It is expected that a similar class will exist for each target environment:
 * Bukkit and Client side.
 *
 * It should not be directly modified.
 *
 * @author cpw
 *
 */
public class FMLServerHandler implements IFMLSidedHandler
{
    /**
     * The singleton
     */
    private static final FMLServerHandler INSTANCE = new FMLServerHandler();

    /**
     * A reference to the server itself
     */
    private MinecraftServer server;

    private FMLServerHandler()
    {
        FMLCommonHandler.instance().beginLoading(this);
    }
    /**
     * Called to start the whole game off from
     * {@link MinecraftServer#startServer}
     *
     * @param minecraftServer
     */
    public void beginServerLoading(MinecraftServer minecraftServer)
    {
        server = minecraftServer;
        Loader.instance().loadMods();
    }

    /**
     * Called a bit later on during server initialization to finish loading mods
     */
    public void finishServerLoading()
    {
        Loader.instance().initializeMods();
        LanguageRegistry.reloadLanguageTable();
    }

    @Override
    public void haltGame(String message, Throwable exception)
    {
        throw new RuntimeException(message, exception);
    }

    /**
     * Get the server instance
     */
    public MinecraftServer getServer()
    {
        return server;
    }

    /**
     * @return the instance
     */
    public static FMLServerHandler instance()
    {
        return INSTANCE;
    }

    /* (non-Javadoc)
     * @see cpw.mods.fml.common.IFMLSidedHandler#getAdditionalBrandingInformation()
     */
    @Override
    public List<String> getAdditionalBrandingInformation()
    {
        return ImmutableList.<String>of();
    }

    /* (non-Javadoc)
     * @see cpw.mods.fml.common.IFMLSidedHandler#getSide()
     */
    @Override
    public Side getSide()
    {
        return Side.SERVER;
    }

    @Override
    public void showGuiScreen(Object clientGuiElement)
    {

    }
    @Override
    public boolean shouldServerShouldBeKilledQuietly()
    {
        return false;
    }
    @Override
    public void addModAsResource(ModContainer container)
    {
        File source = container.getSource();
        try
        {
            if (source.isDirectory())
            {
                searchDirForENUSLanguage(source,"");
            }
            else
            {
                searchZipForENUSLanguage(source);
            }
        }
        catch (IOException ioe)
        {

        }
    }
    private static final Pattern assetENUSLang = Pattern.compile("assets/(.*)/lang/en_US.lang");
    private void searchZipForENUSLanguage(File source) throws IOException
    {
        ZipFile zf = new ZipFile(source);
        for (ZipEntry ze : Collections.list(zf.entries()))
        {
            Matcher matcher = assetENUSLang.matcher(ze.getName());
            if (matcher.matches())
            {
                FMLLog.fine("Injecting found translation data in zip file %s at %s into language system", source.getName(), ze.getName());
                StringTranslate.inject(zf.getInputStream(ze));
            }
        }
        zf.close();
    }
    private void searchDirForENUSLanguage(File source, String path) throws IOException
    {
        for (File file : source.listFiles())
        {
            String currPath = path+file.getName();
            if (file.isDirectory())
            {
                searchDirForENUSLanguage(file, currPath+'/');
            }
            Matcher matcher = assetENUSLang.matcher(currPath);
            if (matcher.matches())
            {
                FMLLog.fine("Injecting found translation data at %s into language system", currPath);
                StringTranslate.inject(new FileInputStream(file));
            }
        }
    }
    @Override
    public void updateResourcePackList()
    {

    }
    @Override
    public String getCurrentLanguage()
    {
        return "en_US";
    }

    @Override
    public void serverStopped()
    {
        // NOOP
    }
    @Override
    public NetworkManager getClientToServerNetworkManager()
    {
        throw new RuntimeException("Missing");
    }
    @Override
    public INetHandler getClientPlayHandler()
    {
        return null;
    }
    @Override
    public void waitForPlayClient()
    {
        // NOOP
    }

    @Override
    public void fireNetRegistrationEvent(EventBus bus, NetworkManager manager, Set<String> channelSet, String channel, Side side)
    {
        bus.post(new FMLNetworkEvent.CustomPacketRegistrationEvent<NetHandlerPlayServer>(manager, channelSet, channel, side, NetHandlerPlayServer.class));
    }
}
