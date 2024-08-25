package xiamomc.morph.client.screens.emote;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import xiamomc.morph.client.AnimationNames;
import xiamomc.morph.client.ClientMorphManager;
import xiamomc.morph.client.MorphClient;
import xiamomc.morph.client.ServerHandler;
import xiamomc.morph.client.graphics.Anchor;
import xiamomc.morph.client.graphics.DrawableText;
import xiamomc.morph.client.graphics.transforms.easings.Easing;
import xiamomc.morph.client.screens.spinner.SpinnerScreen;
import xiamomc.morph.client.screens.WaitingForServerScreen;
import xiamomc.morph.network.commands.C2S.C2SAnimationCommand;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;

import java.util.Objects;

public class EmoteScreen extends SpinnerScreen<SingleEmoteWidget>
{
    private final Bindable<Boolean> serverReady = new Bindable<>();

    private final DrawableText titleText = new DrawableText();

    private final DrawableText currentAnimText = new DrawableText();

    @Override
    protected SingleEmoteWidget createWidget()
    {
        return new SingleEmoteWidget();
    }

    @Override
    protected int getWidgetSize()
    {
        return 55;
    }

    public EmoteScreen()
    {
        super(Text.literal("Disguise emote select screen"));

        serverHandler = MorphClient.getInstance().serverHandler;
        morphManager = MorphClient.getInstance().morphManager;

        titleText.setText(Text.translatable("gui.morphclient.emote_select"));
        titleText.setAnchor(Anchor.TopCentre);
        titleText.setDrawShadow(true);

        currentAnimText.setAnchor(Anchor.BottomCentre);
        currentAnimText.setDrawShadow(true);

        var size = this.getWidgetSize();

        var widgetOffset = size + 5;

        addEmoteWidget(0, -widgetOffset * 2).moveTo(new Vector2i(0, -widgetOffset), 300, Easing.OutExpo);
        addEmoteWidget(widgetOffset * 2, 0).moveTo(new Vector2i(widgetOffset, 0), 300, Easing.OutExpo);
        addEmoteWidget(0, widgetOffset * 2).moveTo(new Vector2i(0, widgetOffset), 300, Easing.OutExpo);
        addEmoteWidget(-widgetOffset * 2, 0).moveTo(new Vector2i(-widgetOffset, 0), 300, Easing.OutExpo);

        var morphManager = MorphClient.getInstance().morphManager;
        var emotes = morphManager.getEmotes();
        for (int i = 0; i < emotes.size(); i++)
        {
            if (i >= spinnerWidgets.size())
            {
                //logger.warn("We run out of widgets!!!");
                break;
            }

            var widget = spinnerWidgets.get(i);
            widget.setEmote(emotes.get(i));
        }

        addEmoteWidget(0, 0).setText(Text.translatable("gui.back"));
        this.add(titleText);
        this.add(currentAnimText);

        this.alpha.set(0f);
        this.fadeIn(500, Easing.OutQuint);

        var serverHandler = MorphClient.getInstance().serverHandler;
        this.serverReady.bindTo(serverHandler.serverReady);

        this.serverReady.onValueChanged((o, n) ->
        {
            MorphClient.getInstance().schedule(() ->
            {
                if (this.isCurrent() && !n)
                    this.push(new WaitingForServerScreen(new EmoteScreen()));
            });
        }, true);

        updateEmoteText(morphManager.emoteDisplayName);
    }

    private final ServerHandler serverHandler;

    private SingleEmoteWidget addEmoteWidget(int x, int y)
    {
        var widget = this.addSingleWidget(x, y);

        widget.setX(x);
        widget.setY(y);

        widget.onClick(() ->
        {
            var emote = widget.getEmote();

            if (emote != null)
                serverHandler.sendCommand(new C2SAnimationCommand(emote));

            MorphClient.getInstance().schedule(this::tryClose);
        });

        return widget;
    }

    @Override
    protected void onScreenExit(@Nullable Screen nextScreen)
    {
        super.onScreenExit(nextScreen);

        if (nextScreen == null)
            this.serverReady.unBindFromTarget();
    }

    @Override
    protected void onScreenResize()
    {
        super.onScreenResize();

        titleText.setY((int)Math.round(this.height * 0.07));
        currentAnimText.setY(-(int)Math.round(this.height * 0.07));
    }

    private Text getEmoteText(@Nullable String identifier)
    {
        if (identifier == null || identifier.equals(AnimationNames.INTERNAL_VANISH) || identifier.equals(AnimationNames.NONE))
             return Text.translatable("gui.none");

        return Text.translatable("emote.morphclient." + identifier);
    }

    private void updateEmoteText(@Nullable String identifier)
    {
        var text = this.getEmoteText(identifier);
        this.currentAnimText.setText(Text.translatable("gui.morphclient.current_emote", text));
    }

    @Nullable
    private String emoteName;

    private final ClientMorphManager morphManager;

    @Override
    public void tick()
    {
        var managerLast = morphManager.emoteDisplayName;

        if (!Objects.equals(this.emoteName, managerLast))
        {
            this.emoteName = managerLast;
            updateEmoteText(managerLast);
        }

        super.tick();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (MorphClient.getInstance().getEmoteKeyBind().matchesKey(keyCode, scanCode))
            MorphClient.getInstance().schedule(this::tryClose);

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void tryClose()
    {
        if (this.isCurrent())
            this.close();
    }
}
