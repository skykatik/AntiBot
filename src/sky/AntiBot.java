package sky;

import arc.Events;
import arc.files.Fi;
import arc.util.*;
import arc.util.serialization.Jval;
import mindustry.game.EventType;
import mindustry.gen.*;
import mindustry.io.JsonIO;
import mindustry.mod.Plugin;
import mindustry.net.*;

import static mindustry.Vars.*;

public class AntiBot extends Plugin{

    private Config config;

    @Override
    public void init(){

        JsonIO.json.setUsePrototypes(false);
        Fi configFi = dataDirectory.child("anti-bot-config.json");
        if(configFi.exists()){
            config = JsonIO.json.fromJson(Config.class, configFi);
            Log.info("[AntiBot]: Config loaded");
        }else{
            configFi.writeString(Jval.read(JsonIO.json.toJson(config = new Config(), Config.class)).toString(Jval.Jformat.formatted));
            Log.info("[AntiBot]: Config created (@)", configFi.absolutePath());
        }

        JsonIO.json.setUsePrototypes(true);

        Events.on(EventType.ConnectionEvent.class, event -> {
            if(!config.joinkeeper.allow(config.joinkeeperInterval, config.joinRateLimit)){
                event.connection.kick(Packets.KickReason.kick);
            }
        });

        Events.on(EventType.PlayerConnect.class, event -> {
            Administration.PlayerInfo playerInfo = netServer.admins.getInfo(event.player.uuid());
            if(Groups.player.contains(p -> p.uuid().equals(event.player.uuid()) || p.con.address.equals(event.player.con.address) ||
                    config.isMatch(event.player.name, p.name)) ||
                    isBanned(event.player) ||
                    (!config.joinkeeper.allow(config.joinkeeperInterval, config.joinRateLimit) && playerInfo.timesJoined == 1)){

                event.player.kick(Packets.KickReason.idInUse);
                Log.info("[AntiBot]: User @ already on server, IP: @; ID: @", event.player.name, event.player.con.address,
                        event.player.uuid());
            }
        });
    }

    private boolean isBanned(Player player){
        return netServer.admins.isIDBanned(player.uuid()) ||
                netServer.admins.isIPBanned(player.con.address) ||
                netServer.admins.isSubnetBanned(player.con.address);
    }

    public static class Config{
        public boolean nameMatchingEnabled = false;
        public int nameMatching = 3;
        public long joinkeeperInterval = 1000; // 1 second
        public int joinRateLimit = 4;

        public transient Ratekeeper joinkeeper = new Ratekeeper();

        public boolean isMatch(String a, String b){
            return nameMatchingEnabled && Strings.levenshtein(a, b) < nameMatching;
        }

        @Override
        public String toString(){
            return "Config{" +
                    "nameMatchingEnabled=" + nameMatchingEnabled +
                    ", nameMatching=" + nameMatching +
                    ", joinkeeperInterval=" + joinkeeperInterval +
                    ", joinRateLimit=" + joinRateLimit +
                    ", joinkeeper=" + joinkeeper +
                    '}';
        }
    }
}
