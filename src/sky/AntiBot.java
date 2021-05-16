package sky;

import arc.Events;
import arc.files.Fi;
import arc.util.*;
import arc.util.serialization.Jval;
import mindustry.game.EventType;
import mindustry.gen.*;
import mindustry.io.JsonIO;
import mindustry.mod.Plugin;
import mindustry.net.Packets;

import static mindustry.Vars.*;

public class AntiBot extends Plugin{

    private Config config;

    @Override
    public void init(){

        JsonIO.json.setUsePrototypes(false);
        Fi configFi = dataDirectory.child("anti-bot-config.json");
        if(configFi.exists()){
            config = JsonIO.json.fromJson(Config.class, configFi);
            Log.info("[AntiBot]: Конфиг загружен");
        }else{
            configFi.writeString(Jval.read(JsonIO.json.toJson(config = new Config(), Config.class)).toString(Jval.Jformat.formatted));
            Log.info("[AntiBot]: Конфиг создан (@)", configFi.absolutePath());
        }

        JsonIO.json.setUsePrototypes(true);

        Events.on(EventType.PlayerConnect.class, event -> {
            if(Groups.player.contains(p -> p.uuid().equals(event.player.uuid()) || p.con.address.equals(event.player.con.address) ||
                    config.isMatch(event.player.name, p.name)) ||
                    isBanned(event.player)){

                event.player.kick(Packets.KickReason.idInUse);
                Log.info("[AntiBot]: Пользователь '@' уже на сервере", event.player.name);
            }
        });
    }

    private boolean isBanned(Player player){
        return netServer.admins.isIDBanned(player.uuid()) ||
                netServer.admins.isIPBanned(player.con.address) ||
                netServer.admins.isSubnetBanned(player.con.address);
    }

    public static class Config{
        public boolean isNameMatchingEnabled = false;
        public int nameMatching = 3;

        public boolean isMatch(String a, String b){
            return isNameMatchingEnabled && Strings.levenshtein(a, b) < nameMatching;
        }

        @Override
        public String toString(){
            return "Config{" +
                    "isNameMatchingEnabled=" + isNameMatchingEnabled +
                    ", nameMatching=" + nameMatching +
                    '}';
        }
    }
}
