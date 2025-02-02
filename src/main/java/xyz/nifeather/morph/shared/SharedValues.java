package xyz.nifeather.morph.shared;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SharedValues
{
    public static boolean allowSinglePlayerDebugging = false;
    public static final Logger LOGGER = LoggerFactory.getLogger("FeatherMorph");

    private static final String morphNameSpace = "morphplugin";

    public static Identifier initializeChannelIdentifier = Identifier.of(morphNameSpace, "init");
    public static Identifier versionChannelIdentifier = Identifier.of(morphNameSpace, "version_v2");
    public static Identifier commandChannelIdentifier = Identifier.of(morphNameSpace, "commands_v2");

    public static final String newProtocolIdentify = "1_21_3_packetbuf";

    public static Identifier versionChannelIdentifierLegacy = Identifier.of(morphNameSpace, "version");
    public static Identifier commandChannelIdentifierLegacy = Identifier.of(morphNameSpace, "commands");

    public static boolean client_UseNewPacketSerializeMethod = false;
}
