package xyz.nifeather.morph.client.graphics.capes.providers;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nifeather.morph.client.graphics.capes.ICapeProvider;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 修改自 <a href="https://github.com/Hibiii/Kappa">Hibiii/Kappa</a>
 * <br>
 * 十分感谢Orz
 */
public final class KappaCapeProvider implements ICapeProvider
{
	//单独给披风请求开一个Worker，避免阻塞MainWorker上的其他请求
	private final ExecutorService capeService = Executors.newFixedThreadPool(3, runnable ->
	{
		var thread = new Thread(runnable);

		thread.setName("FeatherMorph Cape Worker");

		thread.setUncaughtExceptionHandler((t, error) ->
		{
			log.info("Error occurred in thread '%s': %s".formatted(t.getName(), error.getMessage()));
			error.printStackTrace();
		});

		return thread;
	});

	private static final Logger log = LoggerFactory.getLogger(KappaCapeProvider.class);

	private ExecutorService getCapeExecutor()
	{
		return capeService;
	}

	@Override
	public void getCape(GameProfile profile, Consumer<Identifier> callback)
	{
		this.loadCape(profile, callback::accept);
	}

	private final List<UUID> onGoingRequests = ObjectLists.synchronize(new ObjectArrayList<>());

	// This loads the cape for one player, doesn't matter if it's the player or not.
	// Requires a callback, that receives the id for the cape
	public void loadCape(GameProfile player, CapeTextureAvailableCallback callback)
	{
		if (onGoingRequests.stream().anyMatch(uuid -> player.getId().equals(uuid)))
			return;

		onGoingRequests.add(player.getId());

		this.getCapeExecutor().execute(() ->
		{
			// Check if the player doesn't already have a cape.
			Identifier existingCape = capes.get(player.getName());

			if(existingCape != null)
			{
				callback.onTexAvail(existingCape);
				return;
			}

			if(!this.tryUrl(player, callback, "https://optifine.net/capes/" + player.getName() + ".png"))
				this.tryUrl(player, callback, "http://s.optifine.net/capes/" + player.getName() + ".png");
		});
	}

	public interface CapeTextureAvailableCallback {
		public void onTexAvail(Identifier id);
	}

	// This is a provider specific implementation.
	// Images are usually 46x22 or 92x44, and these work as expected (64x32, 128x64).
	// There are edge cages with sizes 184x88, 1024x512 and 2048x1024,
	// but these should work alright.
	private NativeImage uncrop(NativeImage in)
	{
		int srcHeight = in.getHeight(), srcWidth = in.getWidth();
		int zoom = (int) Math.ceil(in.getHeight() / 32f);

		NativeImage out = new NativeImage(64 * zoom, 32 * zoom, true);

		// NativeImage.copyFrom doesn't work! :(
		for (int x = 0; x < srcWidth; x++)
		{
			for (int y = 0; y < srcHeight; y++)
			{
				out.setColorArgb(x, y, in.getColorArgb(x, y));
			}
        }

		return out;
	}

	// This is where capes will be stored
	private static final Map<String, Identifier> capes = new HashMap<String, Identifier>();

	// Try to load a cape from an URL.
	// If this fails, it'll return false, and let us try another url.
	private boolean tryUrl(GameProfile player, CapeTextureAvailableCallback callback, String urlFrom)
	{
		try
		{
			URL url = new URL(urlFrom);

			NativeImage tex = uncrop(NativeImage.read(url.openStream()));
			NativeImageBackedTexture nIBT = new NativeImageBackedTexture(tex);

			Identifier id = Identifier.of("kappa", player.getId().toString().replace("-", "_"));
			MinecraftClient.getInstance().getTextureManager().registerTexture(id, nIBT);

			capes.put(player.getName(), id);
			callback.onTexAvail(id);

			onGoingRequests.removeIf(uuid -> uuid.equals(player.getId()));
		}
		catch(FileNotFoundException e)
		{
			onGoingRequests.removeIf(uuid -> uuid.equals(player.getId()));

			// Getting the cape was successful! But there's no cape, so don't retry.
			return true;
		}
		catch(Throwable t)
		{
			onGoingRequests.removeIf(uuid -> uuid.equals(player.getId()));
			return false;
		}

		return true;
	}

	public KappaCapeProvider()
	{
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> capes.clear());
	}
}
