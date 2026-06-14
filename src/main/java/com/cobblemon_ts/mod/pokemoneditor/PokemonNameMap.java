package com.cobblemon_ts.mod.pokemoneditor;

import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PokemonNameMap {

    // ============================================================
    // lang 翻译：优先用 Minecraft 的语言系统（zh_cn.lang），失败才回落硬编码映射
    // ============================================================
    private static String tryLang(String key, String fallback) {
        try {
            String translated = Component.translatable(key).getString();
            // 没有翻译时会返回 key 本身（例如 "cobblemon.move.tackle"）
            if (translated != null && !translated.isEmpty() && !translated.equals(key) && !translated.startsWith(key.split("\\.")[0] + ".")) {
                return translated.trim();
            }
        } catch (Throwable ignored) {}
        return fallback;
    }

    // 英文物种名 → 中文译名（按图鉴顺序排列）
    public static final Map<String, String> EN_TO_ZH = new LinkedHashMap<>();
    public static final Map<String, String> ZH_TO_EN = new LinkedHashMap<>();

    static {
        // ========== 关都地区 1-151 ==========
        add("bulbasaur", "妙蛙种子");
        add("ivysaur", "妙蛙草");
        add("venusaur", "妙蛙花");
        add("charmander", "小火龙");
        add("charmeleon", "火恐龙");
        add("charizard", "喷火龙");
        add("squirtle", "杰尼龟");
        add("wartortle", "卡咪龟");
        add("blastoise", "水箭龟");
        add("caterpie", "绿毛虫");
        add("metapod", "铁甲蛹");
        add("butterfree", "巴大蝶");
        add("weedle", "独角虫");
        add("kakuna", "铁壳蛹");
        add("beedrill", "大针蜂");
        add("pidgey", "波波");
        add("pidgeotto", "比比鸟");
        add("pidgeot", "大比鸟");
        add("rattata", "小拉达");
        add("raticate", "拉达");
        add("spearow", "烈雀");
        add("fearow", "大嘴雀");
        add("ekans", "阿柏蛇");
        add("arbok", "阿柏怪");
        add("pikachu", "皮卡丘");
        add("raichu", "雷丘");
        add("sandshrew", "穿山鼠");
        add("sandslash", "穿山王");
        add("nidoran_f", "尼多兰");
        add("nidorina", "尼多娜");
        add("nidoqueen", "尼多后");
        add("nidoran_m", "尼多朗");
        add("nidorino", "尼多力诺");
        add("nidoking", "尼多王");
        add("clefairy", "皮皮");
        add("clefable", "皮可西");
        add("vulpix", "六尾");
        add("ninetales", "九尾");
        add("jigglypuff", "胖丁");
        add("wigglytuff", "胖可丁");
        add("zubat", "超音蝠");
        add("golbat", "大嘴蝠");
        add("oddish", "走路草");
        add("gloom", "臭臭花");
        add("vileplume", "霸王花");
        add("paras", "派拉斯");
        add("parasect", "派拉斯特");
        add("venonat", "毛球");
        add("venomoth", "摩鲁蛾");
        add("diglett", "地鼠");
        add("dugtrio", "三地鼠");
        add("meowth", "喵喵");
        add("persian", "猫老大");
        add("psyduck", "可达鸭");
        add("golduck", "哥达鸭");
        add("mankey", "猴怪");
        add("primeape", "火爆猴");
        add("growlithe", "卡蒂狗");
        add("arcanine", "风速狗");
        add("poliwag", "蚊香蝌蚪");
        add("poliwhirl", "蚊香君");
        add("poliwrath", "蚊香泳士");
        add("abra", "凯西");
        add("kadabra", "勇基拉");
        add("alakazam", "胡地");
        add("machop", "腕力");
        add("machoke", "豪力");
        add("machamp", "怪力");
        add("bellsprout", "喇叭芽");
        add("weepinbell", "口呆花");
        add("victreebel", "大食花");
        add("tentacool", "玛瑙水母");
        add("tentacruel", "毒刺水母");
        add("geodude", "小拳石");
        add("graveler", "隆隆石");
        add("golem", "隆隆岩");
        add("ponyta", "小火马");
        add("rapidash", "烈焰马");
        add("slowpoke", "呆呆兽");
        add("slowbro", "呆壳兽");
        add("magnemite", "小磁怪");
        add("magneton", "三合一磁怪");
        add("farfetchd", "大葱鸭");
        add("doduo", "嘟嘟");
        add("dodrio", "嘟嘟利");
        add("seel", "小海狮");
        add("dewgong", "白海狮");
        add("grimer", "臭泥");
        add("muk", "臭臭泥");
        add("shellder", "大舌贝");
        add("cloyster", "刺甲贝");
        add("gastly", "鬼斯");
        add("haunter", "鬼斯通");
        add("gengar", "耿鬼");
        add("onix", "大岩蛇");
        add("drowzee", "催眠貘");
        add("hypno", "引梦貘人");
        add("krabby", "大钳蟹");
        add("kingler", "巨钳蟹");
        add("voltorb", "霹雳电球");
        add("electrode", "顽皮雷弹");
        add("exeggcute", "蛋蛋");
        add("exeggutor", "椰蛋树");
        add("cubone", "卡拉卡拉");
        add("marowak", "嘎啦嘎啦");
        add("hitmonlee", "飞腿郎");
        add("hitmonchan", "快拳郎");
        add("lickitung", "大舌头");
        add("koffing", "瓦斯弹");
        add("weezing", "双弹瓦斯");
        add("rhyhorn", "独角犀牛");
        add("rhydon", "钻角犀兽");
        add("chansey", "吉利蛋");
        add("tangela", "蔓藤怪");
        add("kangaskhan", "袋兽");
        add("horsea", "墨海马");
        add("seadra", "海刺龙");
        add("goldeen", "角金鱼");
        add("seaking", "金鱼王");
        add("staryu", "海星星");
        add("starmie", "宝石海星");
        add("mr_mime", "魔墙人偶");
        add("scyther", "飞天螳螂");
        add("jynx", "迷唇姐");
        add("electabuzz", "电击兽");
        add("magmar", "鸭嘴火兽");
        add("pinsir", "凯罗斯");
        add("tauros", "肯泰罗");
        add("magikarp", "鲤鱼王");
        add("gyarados", "暴鲤龙");
        add("lapras", "拉普拉斯");
        add("ditto", "百变怪");
        add("eevee", "伊布");
        add("vaporeon", "水伊布");
        add("jolteon", "雷伊布");
        add("flareon", "火伊布");
        add("porygon", "多边兽");
        add("omanyte", "菊石兽");
        add("omastar", "多刺菊石兽");
        add("kabuto", "化石盔");
        add("kabutops", "镰刀盔");
        add("aerodactyl", "化石翼龙");
        add("snorlax", "卡比兽");
        add("articuno", "急冻鸟");
        add("zapdos", "闪电鸟");
        add("moltres", "火焰鸟");
        add("dratini", "迷你龙");
        add("dragonair", "哈克龙");
        add("dragonite", "快龙");
        add("mewtwo", "超梦");
        add("mew", "梦幻");

        // ========== 城都地区 152-251 ==========
        add("chikorita", "菊草叶");
        add("bayleef", "月桂叶");
        add("meganium", "大菊花");
        add("cyndaquil", "火球鼠");
        add("quilava", "火岩鼠");
        add("typhlosion", "火暴兽");
        add("totodile", "小锯鳄");
        add("croconaw", "蓝鳄");
        add("feraligatr", "大力鳄");
        add("sentret", "尾立");
        add("furret", "大尾立");
        add("hoothoot", "咕咕");
        add("noctowl", "猫头夜鹰");
        add("ledyba", "芭瓢虫");
        add("ledian", "安瓢虫");
        add("spinarak", "线球");
        add("ariados", "阿利多斯");
        add("crobat", "叉字蝠");
        add("chinchou", "灯笼鱼");
        add("lanturn", "电灯怪");
        add("pichu", "皮丘");
        add("cleffa", "皮宝宝");
        add("igglybuff", "宝宝丁");
        add("togepi", "波克比");
        add("togetic", "波克基古");
        add("natu", "天然雀");
        add("xatu", "天然鸟");
        add("mareep", "咩利羊");
        add("flaaffy", "绵绵");
        add("ampharos", "电龙");
        add("bellossom", "美丽花");
        add("marill", "玛力露");
        add("azumarill", "玛力露丽");
        add("sudowoodo", "胡说树");
        add("politoed", "蚊香蛙皇");
        add("hoppip", "毽子草");
        add("skiploom", "毽子花");
        add("jumpluff", "毽子棉");
        add("aipom", "长尾怪手");
        add("sunkern", "向日种子");
        add("sunflora", "向日花怪");
        add("yanma", "蜻蜻蜓");
        add("wooper", "乌波");
        add("quagsire", "沼王");
        add("espeon", "太阳伊布");
        add("umbreon", "月亮伊布");
        add("murkrow", "黑暗鸦");
        add("slowking", "呆呆王");
        add("misdreavus", "梦妖");
        add("unown", "未知图腾");
        add("wobbuffet", "果然翁");
        add("girafarig", "麒麟奇");
        add("pineco", "榛果球");
        add("forretress", "佛烈托斯");
        add("dunsparce", "土龙弟弟");
        add("gligar", "天蝎");
        add("steelix", "大钢蛇");
        add("snubbull", "布鲁");
        add("granbull", "布鲁皇");
        add("qwilfish", "千针鱼");
        add("scizor", "巨钳螳螂");
        add("shuckle", "壶壶");
        add("heracross", "赫拉克罗斯");
        add("sneasel", "狃拉");
        add("teddiursa", "熊宝宝");
        add("ursaring", "圈圈熊");
        add("slugma", "熔岩虫");
        add("magcargo", "熔岩蜗牛");
        add("swinub", "小山猪");
        add("piloswine", "长毛猪");
        add("corsola", "太阳珊瑚");
        add("remoraid", "铁炮鱼");
        add("octillery", "章鱼桶");
        add("delibird", "信使鸟");
        add("mantine", "巨翅飞鱼");
        add("skarmory", "盔甲鸟");
        add("houndour", "戴鲁比");
        add("houndoom", "黑鲁加");
        add("kingdra", "刺龙王");
        add("phanpy", "小小象");
        add("donphan", "顿甲");
        add("porygon2", "多边兽II");
        add("stantler", "惊角鹿");
        add("smeargle", "图图犬");
        add("tyrogue", "无畏小子");
        add("hitmontop", "战舞郎");
        add("smoochum", "迷唇娃");
        add("elekid", "电击怪");
        add("magby", "鸭嘴宝宝");
        add("miltank", "大奶罐");
        add("blissey", "幸福蛋");
        add("raikou", "雷公");
        add("entei", "炎帝");
        add("suicune", "水君");
        add("larvitar", "由基拉");
        add("pupitar", "沙基拉");
        add("tyranitar", "班基拉斯");
        add("lugia", "洛奇亚");
        add("ho_oh", "凤王");
        add("celebi", "时拉比");

        // ========== 丰缘地区 252-386 ==========
        add("treecko", "木守宫");
        add("grovyle", "森林蜥蜴");
        add("sceptile", "蜥蜴王");
        add("torchic", "火稚鸡");
        add("combusken", "力壮鸡");
        add("blaziken", "火焰鸡");
        add("mudkip", "水跃鱼");
        add("marshtomp", "沼跃鱼");
        add("swampert", "巨沼怪");
        add("poochyena", "土狼犬");
        add("mightyena", "大狼犬");
        add("zigzagoon", "蛇纹熊");
        add("linoone", "直冲熊");
        add("wurmple", "刺尾虫");
        add("silcoon", "甲壳茧");
        add("beautifly", "狩猎凤蝶");
        add("cascoon", "盾甲茧");
        add("dustox", "毒粉蛾");
        add("lotad", "莲叶童子");
        add("lombre", "莲帽小童");
        add("ludicolo", "乐天河童");
        add("seedot", "橡实果");
        add("nuzleaf", "长鼻叶");
        add("shiftry", "狡猾天狗");
        add("taillow", "傲骨燕");
        add("swellow", "大王燕");
        add("wingull", "长翅鸥");
        add("pelipper", "大嘴鸥");
        add("ralts", "拉鲁拉丝");
        add("kirlia", "奇鲁莉安");
        add("gardevoir", "沙奈朵");
        add("surskit", "溜溜糖球");
        add("masquerain", "雨翅蛾");
        add("shroomish", "蘑蘑菇");
        add("breloom", "斗笠菇");
        add("slakoth", "懒人獭");
        add("vigoroth", "过动猿");
        add("slaking", "请假王");
        add("nincada", "土居忍士");
        add("ninjask", "铁面忍者");
        add("shedinja", "脱壳忍者");
        add("whismur", "咕妞妞");
        add("loudred", "吼爆弹");
        add("exploud", "爆音怪");
        add("makuhita", "幕下力士");
        add("hariyama", "超力王");
        add("azurill", "露力丽");
        add("nosepass", "朝北鼻");
        add("skitty", "向尾喵");
        add("delcatty", "优雅猫");
        add("sableye", "勾魂眼");
        add("mawile", "大嘴娃");
        add("aron", "可可多拉");
        add("lairon", "可多拉");
        add("aggron", "波士可多拉");
        add("meditite", "玛沙那");
        add("medicham", "恰雷姆");
        add("electrike", "落雷兽");
        add("manectric", "雷电兽");
        add("plusle", "正电拍拍");
        add("minun", "负电拍拍");
        add("volbeat", "电萤虫");
        add("illumise", "甜甜萤");
        add("roselia", "毒蔷薇");
        add("gulpin", "溶食兽");
        add("swalot", "吞食兽");
        add("carvanha", "利牙鱼");
        add("sharpedo", "巨牙鲨");
        add("wailmer", "吼吼鲸");
        add("wailord", "吼鲸王");
        add("numel", "呆火驼");
        add("camerupt", "喷火驼");
        add("torkoal", "煤炭龟");
        add("spoink", "跳跳猪");
        add("grumpig", "噗噗猪");
        add("spinda", "晃晃斑");
        add("trapinch", "大颚蚁");
        add("vibrava", "超音波幼虫");
        add("flygon", "沙漠蜻蜓");
        add("cacnea", "沙漠奈亚");
        add("cacturne", "梦歌仙人掌");
        add("swablu", "青绵鸟");
        add("altaria", "七夕青鸟");
        add("zangoose", "猫鼬斩");
        add("seviper", "饭匙蛇");
        add("lunatone", "月石");
        add("solrock", "太阳岩");
        add("barboach", "泥泥鳅");
        add("whiscash", "鲶鱼王");
        add("corphish", "龙虾小兵");
        add("crawdaunt", "铁螯龙虾");
        add("baltoy", "天秤偶");
        add("claydol", "念力土偶");
        add("lileep", "触手百合");
        add("cradily", "摇篮百合");
        add("anorith", "太古羽虫");
        add("armaldo", "太古盔甲");
        add("feebas", "笨笨鱼");
        add("milotic", "美纳斯");
        add("castform", "飘浮泡泡");
        add("kecleon", "变隐龙");
        add("shuppet", "怨影娃娃");
        add("banette", "诅咒娃娃");
        add("duskull", "夜巡灵");
        add("dusclops", "彷徨夜灵");
        add("tropius", "热带龙");
        add("chimecho", "风铃铃");
        add("absol", "阿勃梭鲁");
        add("glalie", "冰鬼护");
        add("spheal", "海豹球");
        add("sealeo", "海魔狮");
        add("walrein", "帝牙海狮");
        add("clamperl", "珍珠贝");
        add("huntail", "猎斑鱼");
        add("gorebyss", "樱花鱼");
        add("relicanth", "古空棘鱼");
        add("luvdisc", "爱心鱼");
        add("bagon", "宝贝龙");
        add("shelgon", "甲壳龙");
        add("salamence", "暴飞龙");
        add("beldum", "铁哑铃");
        add("metang", "金属怪");
        add("metagross", "巨金怪");
        add("regirock", "雷吉洛克");
        add("regice", "雷吉艾斯");
        add("registeel", "雷吉斯奇鲁");
        add("latias", "拉帝亚斯");
        add("latios", "拉帝欧斯");
        add("kyogre", "盖欧卡");
        add("groudon", "固拉多");
        add("rayquaza", "烈空坐");
        add("jirachi", "基拉祈");
        add("deoxys", "代欧奇希斯");

        // ========== 神奥地区 387-493 ==========
        add("turtwig", "草苗龟");
        add("grotle", "树林龟");
        add("torterra", "土台龟");
        add("chimchar", "小火焰猴");
        add("monferno", "猛火猴");
        add("infernape", "烈焰猴");
        add("piplup", "波加曼");
        add("prinplup", "波皇子");
        add("empoleon", "帝王拿波");
        add("starly", "姆克儿");
        add("staravia", "姆克鸟");
        add("staraptor", "姆克鹰");
        add("bidoof", "大牙狸");
        add("bibarel", "大尾狸");
        add("kricketot", "圆法师");
        add("kricketune", "音箱蟀");
        add("shinx", "小猫怪");
        add("luxio", "勒克猫");
        add("luxray", "伦琴猫");
        add("budew", "含羞苞");
        add("roserade", "罗丝雷朵");
        add("cranidos", "头盖龙");
        add("rampardos", "战槌龙");
        add("shieldon", "盾甲龙");
        add("bastiodon", "护城龙");
        add("burmy", "结草儿");
        add("wormadam", "结草贵妇");
        add("mothim", "绅士蛾");
        add("combee", "三蜜蜂");
        add("vespiquen", "蜂女王");
        add("pachirisu", "帕奇利兹");
        add("buizel", "泳圈鼬");
        add("floatzel", "浮潜鼬");
        add("cherubi", "樱花宝");
        add("cherrim", "樱花儿");
        add("shellos", "无壳海兔");
        add("gastrodon", "海兔兽");
        add("ambipom", "双尾怪手");
        add("drifloon", "飘飘球");
        add("drifblim", "随风球");
        add("buneary", "卷卷耳");
        add("lopunny", "长耳兔");
        add("mismagius", "梦妖魔");
        add("honchkrow", "乌鸦头头");
        add("glameow", "魅力喵");
        add("purugly", "东施喵");
        add("chingling", "铃铛响");
        add("stunky", "臭鼬噗");
        add("skuntank", "坦克臭鼬");
        add("bronzor", "铜镜怪");
        add("bronzong", "青铜钟");
        add("bonsly", "爱哭树");
        add("mime_jr", "魔尼尼");
        add("happiny", "好运蛋");
        add("chatot", "聒噪鸟");
        add("spiritomb", "花岩怪");
        add("gible", "圆陆鲨");
        add("gabite", "尖牙陆鲨");
        add("garchomp", "烈咬陆鲨");
        add("munchlax", "小卡比兽");
        add("riolu", "利欧路");
        add("lucario", "路卡利欧");
        add("hippopotas", "沙河马");
        add("hippowdon", "河马兽");
        add("skorupi", "钳尾蝎");
        add("drapion", "龙王蝎");
        add("croagunk", "不良蛙");
        add("toxicroak", "毒骷蛙");
        add("carnivine", "尖牙笼");
        add("finneon", "荧光鱼");
        add("lumineon", "霓虹鱼");
        add("mantyke", "小球飞鱼");
        add("snover", "雪笠怪");
        add("abomasnow", "暴雪王");
        add("weavile", "玛狃拉");
        add("magnezone", "自爆磁怪");
        add("lickilicky", "大舌舔");
        add("rhyperior", "超铁暴龙");
        add("tangrowth", "巨蔓藤");
        add("electivire", "电击魔兽");
        add("magmortar", "鸭嘴炎兽");
        add("togekiss", "波克基斯");
        add("yanmega", "梅卡阳玛");
        add("leafeon", "叶伊布");
        add("glaceon", "冰伊布");
        add("gliscor", "天蝎王");
        add("mamoswine", "象牙猪");
        add("porygon_z", "多边兽Z");
        add("gallade", "艾路雷朵");
        add("probopass", "大朝北鼻");
        add("dusknoir", "黑夜魔灵");
        add("froslass", "雪妖女");
        add("rotom", "洛托姆");
        add("uxie", "由克希");
        add("mesprit", "艾姆利多");
        add("azelf", "亚克诺姆");
        add("dialga", "帝牙卢卡");
        add("palkia", "帕路奇亚");
        add("heatran", "席多蓝恩");
        add("regigigas", "雷吉奇卡斯");
        add("giratina", "骑拉帝纳");
        add("cresselia", "克雷色利亚");
        add("phione", "霏欧纳");
        add("manaphy", "玛纳霏");
        add("darkrai", "达克莱伊");
        add("shaymin", "谢米");
        add("arceus", "阿尔宙斯");

        // ========== 合众地区 494-649 ==========
        add("victini", "比克提尼");
        add("snivy", "藤藤蛇");
        add("servine", "青藤蛇");
        add("serperior", "君主蛇");
        add("tepig", "暖暖猪");
        add("pignite", "炒炒猪");
        add("emboar", "炎武王");
        add("oshawott", "水水獭");
        add("dewott", "双刃丸");
        add("samurott", "大剑鬼");
        add("patrat", "探探鼠");
        add("watchog", "步哨鼠");
        add("lillipup", "小约克");
        add("herdier", "哈约克");
        add("stoutland", "长毛狗");
        add("purrloin", "扒手猫");
        add("liepard", "酷豹");
        add("pansage", "花椰猴");
        add("simisage", "花椰猿");
        add("pansear", "爆香猴");
        add("simisear", "爆香猿");
        add("panpour", "冷水猴");
        add("simipour", "冷水猿");
        add("munna", "食梦梦");
        add("musharna", "梦梦蚀");
        add("pidove", "豆豆鸽");
        add("tranquill", "咕咕鸽");
        add("unfezant", "高傲雉鸡");
        add("blitzle", "斑斑马");
        add("zebstrika", "雷电斑马");
        add("roggenrola", "石丸子");
        add("boldore", "地幔岩");
        add("gigalith", "庞岩怪");
        add("woobat", "滚滚蝙蝠");
        add("swoobat", "心蝙蝠");
        add("drilbur", "螺钉地鼠");
        add("excadrill", "龙头地鼠");
        add("audino", "差不多娃娃");
        add("timburr", "搬运小匠");
        add("gurdurr", "铁骨土人");
        add("conkeldurr", "修建老匠");
        add("tym pole", "圆蝌蚪");
        add("palpitoad", "蓝蟾蜍");
        add("seismitoad", "蟾蜍王");
        add("throh", "投摔鬼");
        add("sawk", "打击鬼");
        add("sewaddle", "虫宝包");
        add("swadloon", "宝包茧");
        add("leavanny", "保姆虫");
        add("venipede", "百足蜈蚣");
        add("whirlipede", "车轮球");
        add("scolipede", "蜈蚣王");
        add("cottonee", "木棉球");
        add("whimsicott", "风妖精");
        add("petilil", "百合根娃娃");
        add("lilligant", "裙儿小姐");
        add("basculin", "勇士鲈鱼");
        add("sandile", "黑眼鳄");
        add("krokorok", "混混鳄");
        add("krookodile", "流氓鳄");
        add("darumaka", "火红不倒翁");
        add("darmanitan", "达摩狒狒");
        add("maractus", "沙铃仙人掌");
        add("dwebble", "石居蟹");
        add("crustle", "岩殿居蟹");
        add("scraggy", "滑头小子");
        add("scrafty", "头巾混混");
        add("sigilyph", "象征鸟");
        add("yamask", "哭哭面具");
        add("cofagrigus", "死神棺");
        add("tirtouga", "原盖海龟");
        add("carracosta", "肋骨海龟");
        add("archen", "始祖小鸟");
        add("archeops", "始祖大鸟");
        add("trubbish", "破破袋");
        add("garbodor", "灰尘山");
        add("zorua", "索罗亚");
        add("zoroark", "索罗亚克");
        add("minccino", "奇拉米");
        add("cinccino", "奇拉米诺");
        add("gothita", "歌德小童");
        add("gothorita", "歌德童");
        add("gothitelle", "哥特小姐");
        add("solosis", "单卵细胞球");
        add("duosion", "双卵细胞球");
        add("reuniclus", "人造细胞卵");
        add("ducklett", "鸭宝宝");
        add("swanna", "首席天鹅");
        add("vanillite", "迷你冰");
        add("vanillish", "多多冰");
        add("vanilluxe", "双倍多多冰");
        add("deerling", "四季鹿");
        add("sawsbuck", "芽吹鹿");
        add("emolga", "电飞鼠");
        add("karrablast", "盖盖虫");
        add("escavalier", "骑士蜗牛");
        add("foongus", "哎呀球菇");
        add("amoonguss", "败露球菇");
        add("frillish", "轻飘飘");
        add("jellicent", "胖嘟嘟");
        add("alomomola", "保母曼波");
        add("joltik", "电电虫");
        add("galvantula", "电蜘蛛");
        add("ferroseed", "种子铁球");
        add("ferrothorn", "坚果哑铃");
        add("klink", "齿轮儿");
        add("klang", "齿轮组");
        add("klinklang", "齿轮怪");
        add("tynamo", "麻麻小鱼");
        add("eelektrik", "麻麻鳗");
        add("eelektross", "麻麻鳗鱼王");
        add("elgyem", "小灰怪");
        add("beheeyem", "大宇怪");
        add("litwick", "烛光灵");
        add("lampent", "灯火幽灵");
        add("chandelure", "水晶灯火灵");
        add("axew", "牙牙");
        add("fraxure", "斧牙龙");
        add("haxorus", "双斧战龙");
        add("cubchoo", "喷嚏熊");
        add("beartic", "冻原熊");
        add("cryogonal", "几何雪花");
        add("shelmet", "小嘴蜗");
        add("accelgor", "敏捷虫");
        add("stunfisk", "泥巴鱼");
        add("mienfoo", "功夫鼬");
        add("mienshao", "师父鼬");
        add("druddigon", "赤面龙");
        add("golett", "泥偶小人");
        add("golurk", "泥偶巨人");
        add("pawniard", "驹刀小兵");
        add("bisharp", "劈斩司令");
        add("bouffalant", "爆炸头水牛");
        add("rufflet", "毛头小鹰");
        add("braviary", "勇士雄鹰");
        add("vullaby", "秃鹰丫头");
        add("mandibuzz", "秃鹰娜");
        add("heatmor", "熔蚁兽");
        add("durant", "铁蚁");
        add("deino", "单首龙");
        add("zweilous", "双首暴龙");
        add("hydreigon", "三首恶龙");
        add("larvesta", "燃烧虫");
        add("volcarona", "火神蛾");
        add("cobalion", "勾帕路翁");
        add("terrakion", "代拉基翁");
        add("virizion", "毕力吉翁");
        add("tornadus", "龙卷云");
        add("thundurus", "雷电云");
        add("reshiram", "莱希拉姆");
        add("zekrom", "捷克罗姆");
        add("landorus", "土地云");
        add("kyurem", "酋雷姆");
        add("keldeo", "凯路迪欧");
        add("meloetta", "美洛耶塔");
        add("genesect", "盖诺赛克特");

        // ========== 卡洛斯地区 650-721 ==========
        add("chespin", "哈力栗");
        add("quilladin", "胖胖哈力");
        add("chesnaught", "布里卡隆");
        add("fennekin", "火狐狸");
        add("braixen", "长尾火狐");
        add("delphox", "妖火红狐");
        add("froakie", "呱呱泡蛙");
        add("frogadier", "呱头蛙");
        add("greninja", "甲贺忍蛙");
        add("bunnelby", "掘掘兔");
        add("diggersby", "长耳兔");
        add("fletchling", "小箭雀");
        add("fletchinder", "火箭雀");
        add("talonflame", "烈箭鹰");
        add("scatterbug", "粉蛹");
        add("spewpa", "鳞粉龙");
        add("vivillon", "彩粉蝶");
        add("litleo", "小狮狮");
        add("pyroar", "火炎狮");
        add("flabebe", "花蓓蓓");
        add("floette", "花叶蒂");
        add("florges", "花洁夫人");
        add("skiddo", "坐骑小羊");
        add("gogoat", "坐骑山羊");
        add("pancham", "顽皮熊猫");
        add("pangoro", "流氓熊猫");
        add("furfrou", "多丽米亚");
        add("espurr", "妙喵");
        add("meowstic", "超能妙喵");
        add("honedge", "独剑鞘");
        add("doublade", "双剑鞘");
        add("aegislash", "坚盾剑怪");
        add("spritzee", "绵绵泡芙");
        add("aromatisse", "胖甜妮");
        add("swirlix", "粉香香");
        add("slurpuff", "芳香精");
        add("inkay", "好啦鱿");
        add("malamar", "乌贼王");
        add("binacle", "龟脚脚");
        add("barbaracle", "龟足巨铠");
        add("skrelp", "垃垃藻");
        add("dragalge", "毒藻龙");
        add("clauncher", "钢炮臂虾");
        add("clawitzer", "铁臂枪虾");
        add("helioptile", "伞电蜥");
        add("heliolisk", "光电伞蜥");
        add("tyrunt", "宝宝暴龙");
        add("tyrantrum", "怪颚龙");
        add("amaura", "冰雪龙");
        add("aurorus", "冰雪巨龙");
        add("sylveon", "仙子伊布");
        add("hawlucha", "摔跤鹰人");
        add("dedenne", "咚咚鼠");
        add("carbink", "小碎钻");
        add("goomy", "黏黏宝");
        add("sliggoo", "黏美儿");
        add("goodra", "黏美龙");
        add("klefki", "钥圈儿");
        add("phantump", "小木灵");
        add("trevenant", "朽木妖");
        add("pumpkaboo", "南瓜精");
        add("gourgeist", "南瓜怪人");
        add("bergmite", "迷你冰宝");
        add("avalugg", "冰岩怪");
        add("noibat", "嗡蝠");
        add("noivern", "音波龙");
        add("xerneas", "哲尔尼亚斯");
        add("yveltal", "伊裴尔塔尔");
        add("zygarde", "基格尔德");
        add("diancie", "蒂安希");
        add("hoopa", "胡帕");
        add("volcanion", "波尔凯尼恩");

        // ========== 阿罗拉地区 722-809 ==========
        add("rowlet", "木木枭");
        add("dartrix", "投羽枭");
        add("decidueye", "狙射树枭");
        add("litten", "火斑喵");
        add("torracat", "炎热喵");
        add("incineroar", "炽焰咆哮虎");
        add("popplio", "球球海狮");
        add("brionne", "花漾海狮");
        add("primarina", "西狮海壬");
        add("pikipek", "小笃儿");
        add("trumbeak", "喇叭啄鸟");
        add("toucannon", "铳嘴大鸟");
        add("yungoos", "猫鼬少");
        add("gumshoos", "猫鼬探长");
        add("grubbin", "强颚鸡母虫");
        add("charjabug", "虫电宝");
        add("vikavolt", "锹农炮虫");
        add("crabrawler", "好胜蟹");
        add("crabominable", "好胜毛蟹");
        add("oricorio", "花舞鸟");
        add("cutiefly", "萌虻");
        add("ribombee", "蝶结萌虻");
        add("rockruff", "岩狗狗");
        add("lycanroc", "鬃岩狼人");
        add("wishiwashi", "弱丁鱼");
        add("mareanie", "好坏星");
        add("toxapex", "超坏星");
        add("mudbray", "泥驴仔");
        add("muds dale", "重泥挽马");
        add("dewpider", "滴蛛");
        add("araquanid", "滴蛛霸");
        add("fomantis", "伪螳草");
        add("lurantis", "兰螳花");
        add("morelull", "睡睡菇");
        add("shiinotic", "灯罩夜菇");
        add("salandit", "夜盗火蜥");
        add("salazzle", "焰后蜥");
        add("stufful", "童偶熊");
        add("bewear", "穿着熊");
        add("bounsweet", "甜竹竹");
        add("steenee", "甜舞妮");
        add("tsareena", "甜冷美后");
        add("comfey", "花疗环环");
        add("oranguru", "智挥猩");
        add("passimian", "投掷猴");
        add("wimpod", "胆小虫");
        add("golisopod", "具甲武者");
        add("sandygast", "沙丘娃");
        add("palossand", "噬沙堡爷");
        add("pyukumuku", "拳海参");
        add("type_null", "属性:空");
        add("silvally", "银伴战兽");
        add("minior", "小陨星");
        add("komala", "树枕尾熊");
        add("turtonator", "爆焰龟兽");
        add("togedemaru", "托戈德玛尔");
        add("mimikyu", "谜拟丘");
        add("bruxish", "磨牙彩皮鱼");
        add("drampa", "老翁龙");
        add("dhelmise", "破破舵轮");
        add("jangmo_o", "心鳞宝");
        add("hakamo_o", "鳞甲龙");
        add("kommo_o", "杖尾鳞甲龙");
        add("tapu_koko", "卡璞·鸣鸣");
        add("tapu_lele", "卡璞·蝶蝶");
        add("tapu_bulu", "卡璞·哞哞");
        add("tapu_fini", "卡璞·鳍鳍");
        add("cosmog", "科斯莫古");
        add("cosmoem", "科斯莫姆");
        add("solgaleo", "索尔迦雷欧");
        add("lunala", "露奈雅拉");
        add("nihilego", "虚吾伊德");
        add("buzzwole", "爆肌蚊");
        add("pheromosa", "费洛美螂");
        add("xurkitree", "电束木");
        add("celesteela", "铁火辉夜");
        add("kartana", "纸御剑");
        add("guzzlord", "恶食大王");
        add("necrozma", "奈克洛兹玛");
        add("magearna", "玛机雅娜");
        add("marshadow", "玛夏多");
        add("poipole", "毒贝比");
        add("naganadel", "四颚针龙");
        add("stakataka", "垒磊石");
        add("blacephalon", "砰头小丑");
        add("zeraora", "捷拉奥拉");
        add("meltan", "美录坦");
        add("melmetal", "美录梅塔");

        // ========== 伽勒尔地区 810-898 ==========
        add("grookey", "敲音猴");
        add("thwackey", "啪咚猴");
        add("rillaboom", "轰擂金刚猩");
        add("scorbunny", "炎兔儿");
        add("raboot", "腾蹴小将");
        add("cinderace", "闪焰王牌");
        add("sobble", "泪眼蜥");
        add("drizzile", "变涩蜥");
        add("inteleon", "千面避役");
        add("skwovet", "贪心栗鼠");
        add("greedent", "藏饱栗鼠");
        add("rookidee", "稚山雀");
        add("corvisquire", "蓝鸦");
        add("corviknight", "钢铠鸦");
        add("blipbug", "索侦虫");
        add("dottler", "天罩虫");
        add("orbeetle", "以欧路普");
        add("nickit", "偷儿狐");
        add("thievul", "狐大盗");
        add("gossifleur", "幼绵绵");
        add("eldegoss", "白蓬蓬");
        add("wooloo", "毛辫羊");
        add("dubwool", "毛毛角羊");
        add("chewtle", "咬咬龟");
        add("drednaw", "暴噬龟");
        add("yamper", "来电汪");
        add("boltund", "逐电犬");
        add("rolycoly", "小炭仔");
        add("carkol", "大炭车");
        add("coalossal", "巨炭山");
        add("applin", "啃果虫");
        add("flapple", "苹裹龙");
        add("appletun", "丰蜜龙");
        add("silicobra", "沙包蛇");
        add("sandaconda", "沙螺蟒");
        add("cramorant", "古月鸟");
        add("arrokuda", "刺梭鱼");
        add("barraskewda", "戽斗尖梭");
        add("toxel", "电音婴");
        add("toxtricity", "颤弦蝾螈");
        add("sizzlipede", "烧火蚣");
        add("centiskorch", "焚焰蚣");
        add("clobbopus", "拳拳蛸");
        add("grapploct", "八爪武师");
        add("sinistea", "怖思壶");
        add("polteageist", "怖思壶");
        add("hatenna", "哈喽");
        add("hattrem", "魔灵珊瑚");
        add("hatterene", "布莉姆温");
        add("impidimp", "捣蛋小妖");
        add("morgrem", "诈唬魔");
        add("grimmsnarl", "长毛巨魔");
        add("obstagoon", "堵拦熊");
        add("perrserker", "喵头目");
        add("cursola", "噬沙堡");
        add("sirfetchd", "葱游兵");
        add("mr_rime", "踏冰人偶");
        add("runerigus", "死神板");
        add("milcery", "小仙奶");
        add("alcremie", "霜奶仙");
        add("falinks", "列阵兵");
        add("pincurchin", "啪嚓海胆");
        add("snom", "雪吞虫");
        add("frosmoth", "雪绒蛾");
        add("dracozolt", "雷鸟龙");
        add("arctozolt", "雷鸟海兽");
        add("dracovish", "鳃鱼龙");
        add("arctovish", "鳃鱼海兽");
        add("duraludon", "铝钢龙");
        add("dreepy", "多龙梅西亚");
        add("drakloak", "多龙奇");
        add("dragapult", "多龙巴鲁托");
        add("zacian", "苍响");
        add("zamazenta", "藏玛然特");
        add("eternatus", "无极汰那");
        add("kubfu", "熊徒弟");
        add("urshifu", "武道熊师");
        add("zarude", "萨戮德");
        add("regieleki", "雷吉艾勒奇");
        add("regidrago", "雷吉铎拉戈");
        add("glastrier", "雪暴马");
        add("spectrier", "灵幽马");
        add("calyrex", "蕾冠王");

        // ========== 帕底亚地区 906-1025 ==========
        add("sprigatito", "新叶喵");
        add("floragato", "蒂蕾喵");
        add("meowscarada", "魔幻假面喵");
        add("fuecoco", "呆火鳄");
        add("crocalor", "炙烫鳄");
        add("skeledirge", "骨纹巨声鳄");
        add("quaxly", "润水鸭");
        add("quaxwell", "涌跃鸭");
        add("quaquaval", "狂欢浪舞鸭");
        add("lechonk", "小团鼠");
        add("oinkologne", "巴布土拨");
        add("pawmi", "布拨");
        add("pawmo", "布土拨");
        add("pawmot", "电肚蛙");
        add("tandemaus", "一对鼠");
        add("maushold", "一家鼠");
        add("fidough", "狗仔包");
        add("dachsbun", "麻花犬");
        add("smoliv", "小橄榄");
        add("dolliv", "曼陀罗");
        add("arboliva", "橄榄霸王");
        add("squawkabilly", "花漾海鸭");
        add("naclstack", "盐石堆");
        add("garganacl", "盐石巨灵");
        add("glimmet", "光宝");
        add("glimmora", "晶光花");
        add("shroodle", "毒菇鼠");
        add("grafaiai", "涂鸦猴");
        add("bramblin", "缠草小妖");
        add("brambleghast", "缠草死灵");
        add("toedscool", "走地菇");
        add("toedscruel", "走地环菇");
        add("klawf", "岩蟹");
        add("capsakid", "辣椒籽");
        add("scovillain", "辣椒龙");
        add("mabosstiff", "獒教父");
        add("flittle", "小舞仙");
        add("espathra", "超能艳鸵");
        add("flamigo", "红鹤");
        add("cetoddle", "小鳍鲸");
        add("cetitan", "巨齿鲸");
        add("veluza", "电棘鱼");
        add("orthworm", "土栖蚯");
        add("bellibolt", "电肚蛙");
        add("wugtrio", "三海地鼠");
        add("bombirdier", "坠石鸟");
        add("great_tusk", "雄伟牙");
        add("scream_tail", "猛雷鼓");
        add("brute_bonnet", "沙铁皮");
        add("flutter_mane", "狂舞挥");
        add("slither_wing", "爬地翅");
        add("sandy_shocks", "沙电兽");
        add("iron_treads", "铁辙迹");
        add("iron_bundle", "铁包袱");
        add("iron_hands", "铁臂膀");
        add("iron_jugulis", "铁脖颈");
        add("iron_moth", "铁毒蛾");
        add("iron_thorns", "铁荆棘");
        add("frigibax", "冻脊龙");
        add("arctibax", "戟脊龙");
        add("baxcalibur", "冰脊龙");
        add("gimmighoul", "索财灵");
        add("gholdengo", "赛富豪");
        add("wo_chien", "古简蜗");
        add("chien_pao", "古剑豹");
        add("ting_lu", "古鼎鹿");
        add("chi_yu", "古玉鱼");
        add("treasures_of_ruin", "灾厄之器");
        add("koraidon", "故勒顿");
        add("miraidon", "密勒顿");
    }

    // ===== 性格映射 =====
    public static final Map<String, String> NATURE_EN_TO_ZH = new LinkedHashMap<>();
    public static final Map<String, String> NATURE_ZH_TO_EN = new LinkedHashMap<>();

    static {
        addNature("hardy", "勤奋");
        addNature("lonely", "怕寂寞");
        addNature("brave", "勇敢");
        addNature("adamant", "固执");
        addNature("naughty", "顽皮");
        addNature("bold", "大胆");
        addNature("docile", "坦率");
        addNature("relaxed", "悠闲");
        addNature("impish", "淘气");
        addNature("lax", "无虑");
        addNature("timid", "胆小");
        addNature("hasty", "急躁");
        addNature("serious", "认真");
        addNature("jolly", "爽朗");
        addNature("naive", "天真");
        addNature("modest", "内敛");
        addNature("mild", "慢吞吞");
        addNature("quiet", "冷静");
        addNature("bashful", "害羞");
        addNature("rash", "鲁莽");
        addNature("calm", "温和");
        addNature("gentle", "温顺");
        addNature("sassy", "自大");
        addNature("careful", "慎重");
        addNature("quirky", "浮躁");
    }

    private static void addNature(String en, String zh) {
        NATURE_EN_TO_ZH.put(en.toLowerCase(), zh);
        NATURE_ZH_TO_EN.put(zh, en);
    }

    public static String natureToChinese(String en) {
        if (en == null || en.isEmpty()) return en;
        String id = en.toLowerCase().trim();
        // 1. 优先 lang 翻译
        String fromLang = tryLang("cobblemon.nature." + id, null);
        if (fromLang != null) return fromLang;
        // 2. 回落硬编码映射
        return NATURE_EN_TO_ZH.getOrDefault(id, en);
    }
    public static String natureToEnglish(String zh) {
        if (zh == null || zh.isEmpty()) return zh;
        String trimmed = zh.trim();
        // 1. 精确硬编码映射
        String en = NATURE_ZH_TO_EN.get(trimmed);
        if (en != null) return en;
        // 2. 如果输入本身就是英文 ID（例如手动输入 hardy）
        String lower = trimmed.toLowerCase();
        if (NATURE_EN_TO_ZH.containsKey(lower)) return lower;
        // 3. 反向 lang 查找：遍历所有硬编码 key，看 lang 翻译是否等于中文
        for (String key : NATURE_EN_TO_ZH.keySet()) {
            String translated = tryLang("cobblemon.nature." + key, null);
            if (translated != null && translated.trim().equals(trimmed)) {
                return key;
            }
        }
        // 4. 也尝试用中文作为 key 调 lang（某些 mod 可能用中文 ID）
        String reverse = tryLang("cobblemon.nature." + lower, null);
        if (reverse != null && !reverse.equals("cobblemon.nature." + lower)) {
            // 这返回的是中文翻译...没用
        }
        return zh;
    }

    // ===== 性格效果描述（+攻击 -防御 这种格式）=====
    // 顺序: [0]=HP [1]=攻击 [2]=防御 [3]=特攻 [4]=特防 [5]=速度
    // 正值=加成，负值=减益，0=无变化
    public static final Map<String, double[]> NATURE_EFFECTS = new LinkedHashMap<>();
    static {
        // 无变化性格
        addNatureEffect("hardy", 0, 0, 0, 0, 0, 0);     // 勤奋
        addNatureEffect("docile", 0, 0, 0, 0, 0, 0);    // 坦率
        addNatureEffect("serious", 0, 0, 0, 0, 0, 0);   // 认真
        addNatureEffect("bashful", 0, 0, 0, 0, 0, 0);   // 害羞
        addNatureEffect("quirky", 0, 0, 0, 0, 0, 0);    // 浮躁

        // +攻击 -防御 类
        addNatureEffect("lonely", 0, 0.1, -0.1, 0, 0, 0);   // 怕寂寞 +攻击 -防御
        addNatureEffect("adamant", 0, 0.1, 0, -0.1, 0, 0);  // 固执 +攻击 -特攻
        addNatureEffect("naughty", 0, 0.1, 0, 0, -0.1, 0);  // 顽皮 +攻击 -特防
        addNatureEffect("brave", 0, 0.1, 0, 0, 0, -0.1);    // 勇敢 +攻击 -速度

        // +防御 -攻击 类
        addNatureEffect("bold", 0, -0.1, 0.1, 0, 0, 0);     // 大胆 +防御 -攻击
        addNatureEffect("relaxed", 0, 0, 0.1, 0, 0, -0.1);  // 悠闲 +防御 -速度
        addNatureEffect("impish", 0, 0, 0.1, -0.1, 0, 0);   // 淘气 +防御 -特攻
        addNatureEffect("lax", 0, 0, 0.1, 0, -0.1, 0);      // 无虑 +防御 -特防

        // +速度 类
        addNatureEffect("timid", 0, -0.1, 0, 0, 0, 0.1);    // 胆小 -攻击 +速度
        addNatureEffect("hasty", 0, 0, -0.1, 0, 0, 0.1);    // 急躁 -防御 +速度
        addNatureEffect("jolly", 0, 0, 0, -0.1, 0, 0.1);    // 爽朗 -特攻 +速度
        addNatureEffect("naive", 0, 0, 0, 0, -0.1, 0.1);    // 天真 -特防 +速度

        // +特攻 类
        addNatureEffect("modest", 0, -0.1, 0, 0.1, 0, 0);   // 内敛 -攻击 +特攻
        addNatureEffect("mild", 0, 0, -0.1, 0.1, 0, 0);     // 慢吞吞 -防御 +特攻
        addNatureEffect("quiet", 0, 0, 0, 0.1, 0, -0.1);    // 冷静 +特攻 -速度
        addNatureEffect("rash", 0, 0, 0, 0.1, -0.1, 0);     // 鲁莽 +特攻 -特防

        // +特防 类
        addNatureEffect("calm", 0, -0.1, 0, 0, 0.1, 0);     // 温和 -攻击 +特防
        addNatureEffect("gentle", 0, 0, -0.1, 0, 0.1, 0);   // 温顺 -防御 +特防
        addNatureEffect("sassy", 0, 0, 0, 0, 0.1, -0.1);    // 自大 +特防 -速度
        addNatureEffect("careful", 0, 0, 0, -0.1, 0.1, 0);  // 慎重 -特攻 +特防
    }
    private static void addNatureEffect(String en, double hp, double atk, double def, double spa, double spd, double spe) {
        NATURE_EFFECTS.put(en.toLowerCase(), new double[]{hp, atk, def, spa, spd, spe});
    }
    public static String natureEffectText(String en) {
        if (en == null || en.isEmpty()) return "";
        double[] eff = NATURE_EFFECTS.get(en.toLowerCase().trim());
        if (eff == null) return "";
        String[] names = {"HP", "攻击", "防御", "特攻", "特防", "速度"};
        StringBuilder sb = new StringBuilder();
        boolean hasAny = false;
        for (int i = 0; i < 6; i++) {
            if (eff[i] > 0) {
                sb.append(" +").append(names[i]);
                hasAny = true;
            } else if (eff[i] < 0) {
                sb.append(" -").append(names[i]);
                hasAny = true;
            }
        }
        if (!hasAny) return " 无变化";
        return sb.toString();
    }

    // ===== 携带物品汉化映射（常见物品）=====
    public static final Map<String, String> ITEM_EN_TO_ZH = new LinkedHashMap<>();
    public static final Map<String, String> ITEM_ZH_TO_EN = new LinkedHashMap<>();
    static {
        addItem("air", ""); // 空
        addItem("empty", "");
        addItem("potion", "药水");
        addItem("super_potion", "高级药水");
        addItem("hyper_potion", "超级药水");
        addItem("max_potion", "极限药水");
        addItem("revive", "复活草");
        addItem("max_revive", "极限复活草");
        addItem("pokeball", "宝可梦球");
        addItem("great_ball", "超级球");
        addItem("ultra_ball", "高级球");
        addItem("master_ball", "大师球");
        addItem("pokemon_breeder", "宝可梦饲育家");
        addItem("leftovers", "吃剩的东西");
        addItem("life_orb", "生命宝珠");
        addItem("choice_band", "讲究头带");
        addItem("choice_scarf", "讲究围巾");
        addItem("choice_specs", "讲究眼镜");
        addItem("focus_sash", "气势披带");
        addItem("focus_band", "气势头带");
        addItem("shelmet", "蜗牛壳");
        addItem("iron_ball", "铁球");
        addItem("air_balloon", "气球");
        addItem("power_herb", "力量香草");
        addItem("white_herb", "白色香草");
        addItem("mental_herb", "精神香草");
        addItem("quick_claw", "先制之爪");
        addItem("kings_rock", "王者之证");
        addItem("razor_fang", "锐利之牙");
        addItem("razor_claw", "锐利之爪");
        addItem("scope_lens", "广角镜");
        addItem("zoom_lens", "放大镜");
        addItem("wide_lens", "宽镜头");
        addItem("muscle_band", "力量头带");
        addItem("wise_glasses", "博识眼镜");
        addItem("expert_belt", "专家腰带");
        addItem("eviolite", "进化奇石");
        addItem("assault_vest", "突击背心");
        addItem("safety_goggles", "护目镜");
        addItem("heavy_duty_boots", "重型靴子");
        addItem("throat_spray", "咽喉喷雾");
        addItem("utility_umbrella", "万用伞");
        addItem("berry", "树果");
        addItem("sitrus_berry", "文柚果");
        addItem("oran_berry", "橙橙果");
        addItem("cheri_berry", "Cheri果");
        addItem("chesto_berry", "Chesto果");
        addItem("pecha_berry", "Pecha果");
        addItem("rawst_berry", "Rawst果");
        addItem("aspear_berry", "Aspear果");
        addItem("leppa_berry", "Leppa果");
        addItem("persim_berry", "Persim果");
        addItem("lum_berry", "Lum果");
        addItem("mega_stone", "超进化石");
        addItem("z_crystal", "Z纯晶");
        addItem("dynamax_candy", "极巨糖果");
        addItem("exp_share", "学习装置");
        addItem("lucky_egg", "幸运蛋");
        addItem("amulet_coin", "护身金币");
        addItem("soothe_bell", "安抚之铃");
        addItem("everstone", "不变之石");
        addItem("oval_stone", "圆石");
        addItem("shiny_stone", "光之石");
        addItem("dusk_stone", "暗之石");
        addItem("dawn_stone", "觉醒之石");
        addItem("moon_stone", "月之石");
        addItem("sun_stone", "日之石");
        addItem("thunder_stone", "雷之石");
        addItem("water_stone", "水之石");
        addItem("fire_stone", "火之石");
        addItem("leaf_stone", "叶之石");
        addItem("ice_stone", "冰之石");
        addItem("dragon_scale", "龙之鳞片");
        addItem("metal_coat", "金属膜");
        addItem("king_rock", "王者之印");
        addItem("up_grade", "升级卡");
        addItem("protector", "护具");
        addItem("electirizer", "电力发动机");
        addItem("magmarizer", "熔岩增幅器");
        addItem("dubious_disc", "可疑光盘");
        addItem("reaper_cloth", "灵界之布");
        addItem("prism_scale", "棱鳞");
        addItem("whipped_dream", "泡沫奶油");
        addItem("sachet", "香袋");
        addItem("odd_souvenir", "纪念品");
        addItem("red_card", "红牌");
        addItem("eject_button", "弹射按钮");
        addItem("lagging_tail", "延迟尾");
        addItem("full_incense", "满腹熏香");
        addItem("lax_incense", "松弛熏香");
        addItem("odd_incense", "奇特熏香");
        addItem("rock_incense", "岩石熏香");
        addItem("rose_incense", "玫瑰熏香");
        addItem("sea_incense", "海洋熏香");
        addItem("wave_incense", "波涛熏香");
        addItem("pure_incense", "清净熏香");
        addItem("grassy_seed", "青草种子");
        addItem("misty_seed", "薄雾种子");
        addItem("electric_seed", "电气种子");
        addItem("psychic_seed", "精神种子");
        addItem("sticky_barb", "黏黏网");
        addItem("red_orb", "红色宝珠");
        addItem("blue_orb", "蓝色宝珠");
        addItem("adamant_orb", "金刚宝珠");
        addItem("lustrous_orb", "白玉宝珠");
        addItem("griseous_orb", "白金宝珠");
        addItem("twisted_spoon", "弯曲的汤勺");
        addItem("silk_scarf", "丝绸围巾");
        addItem("charcoal", "木炭");
        addItem("mystic_water", "神秘水滴");
        addItem("miracle_seed", "奇迹种子");
        addItem("magnet", "磁铁");
        addItem("soft_sand", "软沙");
        addItem("hard_stone", "硬石头");
        addItem("spell_tag", "诅咒符");
        addItem("dragon_fang", "龙之牙");
        addItem("never_melt_ice", "不融冰");
        addItem("black_belt", "黑带");
        addItem("black_glasses", "黑色眼镜");
        addItem("sharp_beak", "锐利鸟嘴");
        addItem("poison_barb", "毒刺");
        addItem("silver_powder", "银粉");
        addItem("metal_powder", "金属粉");
    }
    private static void addItem(String en, String zh) {
        String key = en.toLowerCase();
        if (!ITEM_EN_TO_ZH.containsKey(key)) {
            ITEM_EN_TO_ZH.put(key, zh);
            ITEM_ZH_TO_EN.put(zh, en);
        }
    }
    public static String itemToChinese(String en) {
        if (en == null || en.isEmpty()) return en;
        String raw = cleanItemName(en);
        String id = raw.toLowerCase().trim();
        // 1. 优先 lang 翻译（尝试多种常见 key 格式）
        String[] itemKeys = new String[] {
            "item.cobblemon." + id,
            "item.minecraft." + id,
            "block.minecraft." + id,
            "block.cobblemon." + id,
            "item.tm." + id,
            "cobblemon.item.tm." + id
        };
        for (String key : itemKeys) {
            String fromLang = tryLang(key, null);
            if (fromLang != null) return fromLang;
        }
        // 2. 回落硬编码映射
        return ITEM_EN_TO_ZH.getOrDefault(id, raw);
    }
    public static String itemToEnglish(String zh) {
        if (zh == null || zh.isEmpty()) return zh;
        String trimmed = zh.trim();
        // 1. 精确硬编码映射
        String en = ITEM_ZH_TO_EN.get(trimmed);
        if (en != null) return en;
        // 2. 如果输入本身就是英文 ID
        String lower = trimmed.toLowerCase();
        if (ITEM_EN_TO_ZH.containsKey(lower)) return lower;
        // 3. 遍历物品注册表：通过每个物品的 descriptionId 查 lang 翻译，匹配中文
        try {
            String[] prefixes = new String[] { "item.", "block." };
            for (Map.Entry<ResourceKey<Item>, Item> entry : BuiltInRegistries.ITEM.entrySet()) {
                String itemId = entry.getKey().location().getPath();
                for (String prefix : prefixes) {
                    String langKey = prefix + entry.getKey().location().getNamespace() + "." + itemId;
                    String translated = tryLang(langKey, null);
                    if (translated != null && translated.trim().equals(trimmed)) {
                        // 匹配到中文，返回英文 ID
                        return itemId;
                    }
                }
            }
        } catch (Throwable ignored) {}
        // 4. 反向 lang 查找：遍历所有硬编码 key，看 lang 翻译是否等于中文（兜底）
        String[] itemKeys = new String[] {
            "item.cobblemon.", "item.minecraft.", "block.minecraft.", "block.cobblemon."
        };
        for (String key : ITEM_EN_TO_ZH.keySet()) {
            for (String prefix : itemKeys) {
                String translated = tryLang(prefix + key, null);
                if (translated != null && translated.trim().equals(trimmed)) {
                    return key;
                }
            }
        }
        // 5. 清理命名空间（如果用户直接输入了如 minecraft:dirt）
        if (trimmed.contains(":")) {
            int idx = trimmed.lastIndexOf(":");
            return trimmed.substring(idx + 1).trim();
        }
        return zh;
    }
    // 清理物品名（去除引号、minecraft:前缀等）
    private static String cleanItemName(String s) {
        if (s == null) return "";
        String v = s.trim();
        // 去除首尾引号
        if (v.length() >= 2 && ((v.startsWith("'") && v.endsWith("'")) || (v.startsWith("\"") && v.endsWith("\"")))) {
            v = v.substring(1, v.length() - 1);
        }
        // 去除命名空间前缀 (minecraft:xxx → xxx)
        if (v.contains(":")) {
            int idx = v.lastIndexOf(":");
            v = v.substring(idx + 1);
        }
        // [线性-L01] 去除cobblemon.move.或.move.等LangKey前缀 (cobblemon.move.pound → pound)
        // 解决重启后技能名变成键值名的问题：当NBT或其他系统存储了完整localization key时
        while (v.contains(".move.") || v.startsWith("move.")) {
            int idx = v.lastIndexOf(".move.");
            if (idx < 0) idx = v.indexOf("move.");
            if (idx < 0) break;
            v = v.substring(idx + ".move.".length());
        }
        // [线性-L02] 去除残留的cobblemon.前缀 (cobblemon.pound → pound)
        if (v.startsWith("cobblemon.")) {
            v = v.substring("cobblemon.".length());
        }
        // 去除 "1 of " 之类前缀
        if (v.toLowerCase().contains(" of ")) {
            v = v.substring(v.indexOf(" of ") + 4);
        }
        return v.trim();
    }

    // ===== 技能名称汉化映射（常见技能）=====
    public static final Map<String, String> MOVE_EN_TO_ZH = new LinkedHashMap<>();
    public static final Map<String, String> MOVE_ZH_TO_EN = new LinkedHashMap<>();
    static {
        addMove("tackle", "撞击");
        addMove("scratch", "抓");
        addMove("pound", "拍击");
        addMove("quick_attack", "电光一闪");
        addMove("vine_whip", "藤鞭");
        addMove("ember", "火花");
        addMove("water_gun", "水枪");
        addMove("peck", "啄");
        addMove("bite", "咬咬");
        addMove("lick", "舔舔");
        addMove("absorb", "吸取");
        addMove("mega_drain", "超级吸取");
        addMove("giga_drain", "终极吸取");
        addMove("leech_seed", "寄生种子");
        addMove("synthesis", "光合作用");
        addMove("solar_beam", "日光束");
        addMove("flamethrower", "喷射火焰");
        addMove("fire_blast", "大字爆炎");
        addMove("hydro_pump", "水炮");
        addMove("surf", "冲浪");
        addMove("waterfall", "攀瀑");
        addMove("dive", "潜水");
        addMove("whirlpool", "潮旋");
        addMove("thunder_shock", "电击");
        addMove("thunderbolt", "十万伏特");
        addMove("thunder", "打雷");
        addMove("volt_tackle", "伏特攻击");
        addMove("spark", "电光");
        addMove("rock_throw", "岩石砸击");
        addMove("rock_slide", "岩崩");
        addMove("stone_edge", "尖石攻击");
        addMove("earthquake", "地震");
        addMove("dig", "挖洞");
        addMove("fissure", "地裂");
        addMove("shadow_ball", "暗影球");
        addMove("shadow_punch", "暗影拳");
        addMove("night_shade", "黑夜魔影");
        addMove("phantom_force", "潜灵奇袭");
        addMove("psychic", "精神强念");
        addMove("psybeam", "幻象光线");
        addMove("confusion", "念力");
        addMove("zen_headbutt", "意念头锤");
        addMove("calm_mind", "冥想");
        addMove("dragon_pulse", "龙之波动");
        addMove("dragon_claw", "龙爪");
        addMove("dragon_breath", "龙之怒");
        addMove("outrage", "逆鳞");
        addMove("hyper_beam", "破坏光线");
        addMove("giga_impact", "终极冲击");
        addMove("hyper_voice", "巨声");
        addMove("ice_beam", "冰冻光束");
        addMove("blizzard", "暴风雪");
        addMove("frost_breath", "冰之吐息");
        addMove("ice_punch", "冰冻拳");
        addMove("fire_punch", "火焰拳");
        addMove("thunder_punch", "雷电拳");
        addMove("drain_punch", "吸取拳");
        addMove("dynamic_punch", "爆裂拳");
        addMove("focus_punch", "集中猛击");
        addMove("mach_punch", "音速拳");
        addMove("bullet_punch", "子弹拳");
        addMove("iron_fist", "铁蹄光线");
        addMove("swords_dance", "剑舞");
        addMove("dragon_dance", "龙之舞");
        addMove("belly_drum", "腹鼓");
        addMove("work_up", "磨爪");
        addMove("nasty_plot", "诡计");
        addMove("tail_glow", "萤火");
        addMove("quiver_dance", "蝶之舞");
        addMove("shell_smash", "破壳");
        addMove("agility", "高速移动");
        addMove("rock_polish", "岩石打磨");
        addMove("defense_curl", "卷紧");
        addMove("harden", "变硬");
        addMove("iron_defense", "铁壁");
        addMove("acid_armor", "溶化");
        addMove("barrier", "光墙");
        addMove("light_screen", "光壁");
        addMove("reflect", "反射壁");
        addMove("aurora_veil", "极光幕");
        addMove("safeguard", "神秘守护");
        addMove("mist", "白雾");
        addMove("rest", "睡觉");
        addMove("sleep_talk", "梦话");
        addMove("snore", "打鼾");
        addMove("roost", "羽栖");
        addMove("recover", "自我再生");
        addMove("soft_boiled", "生蛋");
        addMove("moonlight", "月光");
        addMove("morning_sun", "朝日");
        addMove("synthesis_day", "光合作用");
        addMove("wish", "祈愿");
        addMove("healing_wish", "治愈之愿");
        addMove("lunar_dance", "月之祝福");
        addMove("pain_split", "分担痛楚");
        addMove("substitute", "替身");
        addMove("protect", "守住");
        addMove("detect", "看穿");
        addMove("endure", "挺住");
        addMove("flinch", "退缩");
        addMove("fake_out", "击掌奇袭");
        addMove("taunt", "挑衅");
        addMove("encore", "再来一次");
        addMove("disable", "定身法");
        addMove("curse", "诅咒");
        addMove("spite", "怨恨");
        addMove("grudge", "牵挂");
        addMove("destiny_bond", "同命");
        addMove("mean_look", "黑色目光");
        addMove("spider_web", "蜘蛛网");
        addMove("block", "挡路");
        addMove("trap", "陷阱");
        addMove("ingrain", "扎根");
        addMove("memento", "临别礼物");
        addMove("explosion", "大爆炸");
        addMove("self_destruct", "自爆");
        addMove("magnitude", "震级");
        addMove("earth_power", "大地之力");
        addMove("mud_shot", "泥巴射击");
        addMove("mud_bomb", "泥汤");
        addMove("sludge", "污泥攻击");
        addMove("sludge_bomb", "污泥炸弹");
        addMove("poison_jab", "毒击");
        addMove("toxic", "剧毒");
        addMove("toxic_spikes", "毒菱");
        addMove("venoshock", "毒液冲击");
        addMove("cross_poison", "十字毒刃");
        addMove("gunk_shot", "垃圾射击");
        addMove("bug_bite", "虫咬");
        addMove("signal_beam", "信号光束");
        addMove("bug_buzz", "虫鸣");
        addMove("x_scissor", "十字剪");
        addMove("u_turn", "急速折返");
        addMove("volt_switch", "伏特替换");
        addMove("baton_pass", "接力棒");
        addMove("parting_shot", "抛下狠话");
        addMove("flip_turn", "快速折返");
        addMove("teleport", "瞬间移动");
        addMove("stealth_rock", "隐形岩");
        addMove("spikes", "撒菱");
        addMove("toxic_spikes_mid", "毒菱");
        addMove("sticky_web", "黏黏网");
        addMove("tailwind", "顺风");
        addMove("trick_room", "戏法空间");
        addMove("magic_room", "魔法空间");
        addMove("wonder_room", "奇妙空间");
        addMove("hazard", "危险");
        addMove("iron_head", "铁头");
        addMove("iron_tail", "铁尾");
        addMove("steel_wing", "钢翼");
        addMove("flash_cannon", "加农光炮");
        addMove("bullet_seed", "种子机关枪");
        addMove("pin_missile", "飞弹针");
        addMove("icicle_spear", "冰锥");
        addMove("rock_blade", "岩石炮");
        addMove("bone_rush", "骨头回力镖");
        addMove("scale_shot", "鳞片射击");
        addMove("tail_ slap", "尾部拍打");
        addMove("double_kick", "双踢");
        addMove("triple_kick", "三连踢");
        addMove("comet_punch", "彗星拳");
        addMove("furry_cutter", "连续切");
        addMove("slam", "猛撞");
        addMove("body_slam", "泰山压顶");
        addMove("take_down", "舍身冲撞");
        addMove("double_edge", "舍身冲撞");
        addMove("wood_hammer", "木槌");
        addMove("brave_bird", "勇鸟猛攻");
        addMove("flare_blitz", "闪焰冲锋");
        addMove("volt_tackle_mid", "伏特攻击");
        addMove("head_smash", "头锤");
        addMove("submission", "过肩摔");
        addMove("counter", "双倍奉还");
        addMove("mirror_coat", "镜面反射");
        addMove("metal_burst", "金属爆裂");
        addMove("reversal", "起死回生");
        addMove("endeavor", "蛮干");
        addMove("eruption", "喷火");
        addMove("water_spout", "喷水");
        addMove("dragon_rage", "龙之怒");
        addMove("sonic_boom", "音速波");
        addMove("fissure_mid", "地裂");
        addMove("guillotine", "断头钳");
        addMove("horn_drill", "独角钻");
        addMove("sheer_cold", "绝对零度");
        addMove("weather_ball", "气象球");
        addMove("judgement", "制裁光砾");
        addMove("relic_song", "古老之歌");
        addMove("secret_sword", "神秘之剑");
        addMove("v_create", "V热焰");
        addMove("fusion_flare", "火焰交织");
        addMove("fusion_bolt", "雷电交织");
        addMove("land_s_wrath", "大地之怒");
        addMove("freeze_shock", "冰冻伏特");
        addMove("ice_burn", "炽焰咆哮");
        addMove("snarl", "大声咆哮");
        addMove("dark_pulse", "恶之波动");
        addMove("knock_off", "拍落");
        addMove("sucker_punch", "突袭");
        addMove("foul_play", "欺诈");
        addMove("crunch", "咬碎");
        addMove("bite_mid", "咬咬");
        addMove("astonish", "惊吓");
        addMove("hex", "诅咒");
        addMove("night_daze", "暗夜爆破");
        addMove("hyperspace_fury", "极巨攻击");
        addMove("moongeist_beam", "月亮之力");
        addMove("sunsteel_strike", "旭日光");
        addMove("giga_drain_mid", "终极吸取");
        addMove("moonblast", "月亮之力");
        addMove("dazzling_gleam", "魔法闪耀");
        addMove("play_rough", "嬉闹");
        addMove("fairy_wind", "妖精之风");
        addMove("draining_kiss", "吸取之吻");
        addMove("charm", "迷人");
        addMove("attract", "着迷");
        addMove("sweet_kiss", "甜蜜kiss");
        addMove("disarming_voice", "魅惑之声");
        addMove("spirit_shackle", "精神冲击");
        addMove("king_s_shield", "王者盾牌");
        addMove("spiky_shield", "尖刺防守");
        addMove("obstruct", "阻挡");
        addMove("silver_wind", "银色旋风");
        addMove("iron_wind", "铁旋风");
        addMove("aeroblast", "空中爆破");
        addMove("sky_attack", "天空攻击");
        addMove("fly", "飞翔");
        addMove("bounce", "弹跳");
        addMove("sky_drop", "天空下落");
        addMove("aerial_ace", "燕返");
        addMove("wing_attack", "翅膀攻击");
        addMove("drill_peck", "钻啄");
        addMove("hurricane", "暴风");
        addMove("gust", "旋风");
        addMove("air_cutter", "空气利刃");
        addMove("air_slash", "空气斩");
        addMove("brave_bird_mid", "勇鸟猛攻");
        addMove("dual_wingbeat", "双翼");
        addMove("cut", "居合斩");
        addMove("strength", "劲力");
        addMove("surf_mid", "冲浪");
        addMove("fly_mid", "飞翔");
        addMove("rock_climb", "攀岩");
        addMove("waterfall_mid", "攀瀑");
        addMove("dive_mid", "潜水");
        addMove("flash", "闪光");
        addMove("rock_smash", "碎岩");
        addMove("telekinesis", "念力");
        addMove("gravity_mid", "重力");
        addMove("wonder_room_mid", "奇妙房间");
        addMove("magnet_rise", "磁力上升");
        addMove("trick", "戏法");
        addMove("switcheroo", "换手");
        addMove("bestow", "赠予");
        addMove("role_play", "角色扮演");
        addMove("simple_beam", "单纯光线");
        addMove("worry_seed", "烦恼种子");
        addMove("imprison", "封印");
        addMove("perish_song", "灭亡之歌");
        addMove("heal_bell", "治愈铃铛");
        addMove("aromatherapy", "芳香治疗");
        addMove("refresh", "自我激励");
        addMove("haze", "黑雾");
        addMove("mist_mid", "白雾");
        addMove("supersonic", "超音波");
        addMove("screech", "刺耳声");
        addMove("metal_sound", "金属音");
        addMove("growl", "叫声");
        addMove("leer", "瞪眼");
        addMove("scary_face", "鬼脸");
        addMove("charm_mid", "迷人");
        addMove("baby_doll_eyes", "魅惑之声");
        addMove("sweet_scent", "甜甜香气");
        addMove("string_shot", "吐丝");
        addMove("stun_spore", "麻痹粉");
        addMove("sleep_powder", "催眠粉");
        addMove("poison_powder", "毒粉");
        addMove("toxic_mid", "剧毒");
        addMove("will_o_wisp", "鬼火");
        addMove("thunder_wave", "电磁波");
        addMove("glare", "光墙");
        addMove("hypnosis", "催眠术");
        addMove("sing", "歌唱");
        addMove("grass_whistle", "草笛");
        addMove("confuse_ray", "怪异光");
        addMove("swagger", "虚张声势");
        addMove("flatter", "吹捧");
        addMove("teeter_dance", "摇晃舞");
        addMove("uproar", "吵杂");
        addMove("uproar_mid", "吵杂");
        addMove("echoed_voice", "回声");
        addMove("round", "圆瞳");
        addMove("hidden_power", "觉醒力量");
        addMove("secret_power", "秘密之力");
        addMove("nature_power", "自然之恩");
        addMove("power_swap", "力量交换");
        addMove("guard_swap", "防御交换");
        addMove("heart_swap", "心之交换");
        addMove("power_split", "力量平分");
        addMove("guard_split", "防御平分");
        addMove("topsy_turvy", "颠倒");
        addMove("instruct", "号令");
        addMove("after_you", "您先请");
        addMove("quash", "压下");
        addMove("held_item_swap", "道具交换");
        addMove("magic_coat", "魔法外套");
        addMove("snatch", "抢夺");
        addMove("helping_hand", "帮助");
        addMove("follow_me", "看我嘛");
        addMove("rage_powder", "愤怒粉");
        addMove("ally_switch", "交换位置");
        addMove("celebrate", "庆祝");
        addMove("hold_hands", "携手");
        addMove("happy_face", "笑脸");
        addMove("celebrate_mid", "庆祝");
    }
    private static void addMove(String en, String zh) {
        String key = en.toLowerCase();
        if (!MOVE_EN_TO_ZH.containsKey(key)) {
            MOVE_EN_TO_ZH.put(key, zh);
            MOVE_ZH_TO_EN.put(zh, en);
        }
    }
    public static String moveToChinese(String en) {
        if (en == null || en.isEmpty()) return en;
        String raw = cleanItemName(en);
        // 1. 优先 lang 翻译（Cobblemon 的招式 key 格式：cobblemon.move.<id>）
        String fromLang = tryLang("cobblemon.move." + raw.toLowerCase().trim(), null);
        if (fromLang != null) return fromLang;
        // 2. 回落硬编码映射
        return MOVE_EN_TO_ZH.getOrDefault(raw.toLowerCase().trim(), raw);
    }
    // [线性-L03] moveToEnglish: 支持中文→英文转换，同时清理langKey等异常输入
    // 解决重启后技能名变成键值名导致无法修改的问题
    public static String moveToEnglish(String zh) {
        if (zh == null || zh.isEmpty()) return zh;
        // [线性-L03a] 先尝试中文→英文映射
        String en = MOVE_ZH_TO_EN.get(zh.trim());
        if (en != null) return en;
        // [线性-L03b] 输入可能已经是英文内部名（如pound），直接返回
        if (MOVE_EN_TO_ZH.containsKey(zh.toLowerCase().trim())) return zh.toLowerCase().trim();
        // [线性-L03c] 输入可能是langKey（如cobblemon.move.pound）或包含命名空间，清洗后再查
        String cleaned = cleanItemName(zh);
        if (!cleaned.equals(zh.trim())) {
            String en2 = MOVE_ZH_TO_EN.get(cleaned);
            if (en2 != null) return en2;
            if (MOVE_EN_TO_ZH.containsKey(cleaned.toLowerCase().trim())) return cleaned.toLowerCase().trim();
            return cleaned;
        }
        return zh;
    }

    // ===== 特性映射（去重版本）=====
    public static final Map<String, String> ABILITY_EN_TO_ZH = new LinkedHashMap<>();
    public static final Map<String, String> ABILITY_ZH_TO_EN = new LinkedHashMap<>();
    private static final List<String> ABILITY_LIST = new ArrayList<>();

    static {
        // 按照官方特性名称表整理（去重后）
        addAbility("overgrow", "茂盛");
        addAbility("blaze", "猛火");
        addAbility("torrent", "激流");
        addAbility("swarm", "虫之预感");
        addAbility("shield_dust", "蜕皮");
        addAbility("keen_eye", "锐利目光");
        addAbility("sniper", "狙击手");
        addAbility("run_away", "逃跑");
        addAbility("guts", "毅力");
        addAbility("hustle", "强行");
        addAbility("intimidate", "威吓");
        addAbility("static", "静电");
        addAbility("volt_absorb", "避雷针");
        addAbility("sand_veil", "沙隐");
        addAbility("sand_rush", "拨沙");
        addAbility("oblivious", "迟钝");
        addAbility("own_tempo", "我行我素");
        addAbility("cloud_nine", "天气锁");
        addAbility("natural_cure", "自然回复");
        addAbility("serene_grace", "天恩");
        addAbility("poison_point", "毒刺");
        addAbility("rivalry", "斗争心");
        addAbility("sturdy", "结实");
        addAbility("rock_head", "硬壳盔甲");
        addAbility("smoke_screen", "雪隐");
        addAbility("inner_focus", "精神力");
        addAbility("sticky_hold", "黏着");
        addAbility("synchronize", "同步");
        addAbility("clear_body", "净体");
        addAbility("pure_power", "纯力量");
        addAbility("huge_power", "大力士");
        addAbility("lightning_rod", "避雷针");
        addAbility("compound_eyes", "复眼");
        addAbility("effect_spore", "孢子");
        addAbility("levitate", "飘浮");
        addAbility("wonder_guard", "神奇守护");
        addAbility("soundproof", "隔音");
        addAbility("pressure", "压迫感");
        addAbility("color_change", "变色");
        addAbility("flash_fire", "引火");
        addAbility("chlorophyll", "叶绿素");
        addAbility("drizzle", "降雨");
        addAbility("drought", "日照");
        addAbility("snow_cloak", "雪隐");
        addAbility("ice_body", "冰之身躯");
        addAbility("gale_wings", "疾风之翼");
        addAbility("prankster", "恶作剧之心");
        addAbility("sand_force", "沙之力量");
        addAbility("iron_barbs", "铁棘");
        addAbility("iron_fist", "铁拳");
        addAbility("justified", "正义之心");
        addAbility("rattled", "紧张感");
        addAbility("magic_bounce", "魔法反射");
        addAbility("contrary", "唱反调");
        addAbility("super_luck", "超幸运");
        addAbility("anticipation", "预知");
        addAbility("forewarn", "预感");
        addAbility("unaware", "纯朴");
        addAbility("reckless", "舍身");
        addAbility("fluffy", "毛茸茸");
        addAbility("pixilate", "妖精皮肤");
        addAbility("refrigerate", "冷冻皮肤");
        addAbility("aerilate", "飞行皮肤");
        addAbility("normalize", "一般皮肤");
        addAbility("galvanize", "电气皮肤");
        addAbility("liquid_voice", "湿润之声");
        addAbility("speed_boost", "加速");
        addAbility("hyper_cutter", "怪力钳");
        addAbility("battle_armor", "战斗盔甲");
        addAbility("storm_drain", "引水");
        addAbility("water_absorb", "储水");
        addAbility("thick_fat", "厚脂肪");
        addAbility("stench", "恶臭");
        addAbility("trace", "追踪");
        addAbility("quick_feet", "飞毛腿");
        addAbility("immunity", "免疫");
        addAbility("white_smoke", "白烟");
        addAbility("gluttony", "贪吃");
        addAbility("anger_point", "愤怒穴位");
        addAbility("technician", "技术高手");
        addAbility("leaf_guard", "绿叶守护");
        addAbility("mold_breaker", "破格");
        addAbility("tinted_lens", "有色眼镜");
        addAbility("moody", "性情多变");
        addAbility("tangled_feet", "蹒跚");
        addAbility("solid_rock", "坚实");
        addAbility("quick_draw", "快速防守");
        addAbility("unburden", "轻装");
        addAbility("gravity", "重力");
        addAbility("scrappy", "破格");
        addAbility("aroma_veil", "芳香幕");
        addAbility("flower_veil", "花幕");
        addAbility("cheek_pouch", "颊囊");
        addAbility("stamina", "持久力");
        addAbility("wimp_out", "软弱");
        addAbility("emergency_exit", "紧急撤退");
        addAbility("disguise", "伪装");
        addAbility("battle_bond", "牵绊变身");
        addAbility("power_of_alchemy", "炼金术之力");
        addAbility("dazzling", "耀眼");
        addAbility("soul_heart", "魂心");
        addAbility("tangling_hair", "缠绕发");
        addAbility("mirror_armor", "镜甲");
        addAbility("stance_change", "换场");
        addAbility("steelworker", "钢之意志");
        addAbility("punk_rock", "朋克摇滚");
        addAbility("dauntless_shield", "无惧之盾");
        addAbility("grassy_surge", "青草制造者");
        addAbility("psychic_surge", "精神制造者");
        addAbility("misty_surge", "薄雾制造者");
        addAbility("electric_surge", "电气制造者");
        addAbility("libero", "自由者");
        addAbility("protean", "变幻自如");
        addAbility("parental_bond", "亲子爱");
        addAbility("mummy", "木乃伊");
        addAbility("no_guard", "无防守");
        addAbility("adaptability", "适应力");
        addAbility("skill_link", "连续攻击");
        addAbility("hydration", "湿润身体");
        addAbility("frisk", "探查");
        addAbility("rocky_pellet", "岩石盔");
        addAbility("bulletproof", "防弹");
        addAbility("competitive", "好胜");
        addAbility("strong_jaw", "强壮之颚");
        addAbility("sweet_veil", "甜蜜幕");
        addAbility("schooling", "鱼群形态");
        addAbility("multiscale", "多重鳞片");
        addAbility("truant", "懒惰");
        addAbility("slow_start", "慢启动");
        addAbility("air_lock", "气闸");
        addAbility("zen_mode", "正念模式");
        addAbility("turboblaze", "涡轮火焰");
        addAbility("teravolt", "兆级电压");
        addAbility("regenerator", "再生力");
        addAbility("big_pecks", "结实");
        addAbility("victory_star", "胜利之星");
        addAbility("marvel_scale", "奇迹鳞粉");
        addAbility("early_bird", "早起");
        addAbility("limber", "柔软");
        addAbility("magnet_pull", "磁力");
        addAbility("water_veil", "水幕");
        addAbility("overcoat", "防尘");
        addAbility("poison_heal", "毒回复");
        addAbility("damp", "湿气");
        addAbility("cute_charm", "迷人之躯");
        addAbility("sand_stream", "沙暴");
        addAbility("snow_warning", "雪天");
        addAbility("forecast", "预知天气");
        addAbility("bad_dreams", "恶梦");
        addAbility("light_metal", "轻金属");
        addAbility("heavy_metal", "重金属");
        addAbility("multitype", "多重属性");
        addAbility("stall", "延迟");
        addAbility("simple", "单纯");
        addAbility("dry_skin", "干燥皮肤");
        addAbility("analytic", "分析");
        addAbility("flame_body", "火焰之躯");
        addAbility("magma_armor", "熔岩铠甲");
        addAbility("water_compaction", "水之凝聚");
        addAbility("merciless", "残忍");
        addAbility("ice_scales", "冰鳞");
        addAbility("ice_face", "冰面");
        addAbility("neuroforce", "神经元");
        addAbility("chilling_neigh", "冷冽嘶鸣");
        addAbility("supreme_overlord", "至高霸主");
        addAbility("fairies_boost", "妖精之威");
        addAbility("mycelium_might", "菌丝之力");
        addAbility("purifying_salt", "净化之盐");
        addAbility("well_baked_body", "烘烤之躯");
        addAbility("zero_to_hero", "零到英雄");
        addAbility("commander", "指挥官");
        addAbility("electromorphosis", "电流形态");
        addAbility("protosynthesis", "古代活性");
        addAbility("quark_drive", "夸克驱动");
        addAbility("beads_of_ruin", "灾厄之玉");
        addAbility("sword_of_ruin", "灾厄之剑");
        addAbility("vessel_of_ruin", "灾厄之器");
        addAbility("tablets_of_ruin", "灾厄之碑");
        addAbility("orichalcum_pulse", "日珥");
        addAbility("hadron_engine", "强子引擎");
        addAbility("sharpness", "锐利");
        addAbility("mind_s_eye", "心眼");
        addAbility("supercell", "超级细胞");
        addAbility("tera_shift", "太晶变身");
        addAbility("tera_shell", "太晶之壳");
        addAbility("tera_blast", "太晶爆发");
        addAbility("supersonic_skate", "超音速滑行");
        addAbility("poison_puppeteer", "毒傀儡师");
        addAbility("threaded_fangs", "穿透之牙");
        addAbility("lattice", "晶格");
        addAbility("azure_eye", "碧蓝之瞳");
        addAbility("corrosion", "腐蚀");
        addAbility("shadow_shield", "暗影护盾");
        addAbility("healer", "治愈者");
        addAbility("friend_guard", "友情防护");
        addAbility("vegetable_guard", "素食防护");
        addAbility("lingering_aroma", "残留香气");
        addAbility("eerie_spell", "诡异咒语");
        addAbility("power_spot", "力量场地");
        addAbility("heroic_ability", "英雄之力");

        // 构建中文特性列表（用于下拉选择）
        for (String zh : ABILITY_ZH_TO_EN.keySet()) {
            ABILITY_LIST.add(zh);
        }
    }

    private static void addAbility(String en, String zh) {
        String key = en.toLowerCase();
        if (!ABILITY_EN_TO_ZH.containsKey(key)) {
            ABILITY_EN_TO_ZH.put(key, zh);
            ABILITY_ZH_TO_EN.put(zh, en);
        }
    }

    public static String abilityToChinese(String en) {
        if (en == null || en.isEmpty()) return en;
        String id = en.toLowerCase().trim();
        // 1. 优先 lang 翻译
        String fromLang = tryLang("cobblemon.ability." + id, null);
        if (fromLang != null) return fromLang;
        // 2. 回落硬编码映射
        return ABILITY_EN_TO_ZH.getOrDefault(id, en);
    }
    // ===== 物种名称映射辅助方法
    private static void add(String en, String zh) {
        String key = en.toLowerCase();
        if (!EN_TO_ZH.containsKey(key)) {
            EN_TO_ZH.put(key, zh);
            ZH_TO_EN.put(zh, en);
        }
    }

    public static String toChinese(String en) {
        if (en == null || en.isEmpty()) return en;
        String id = en.toLowerCase().trim();
        // 1. 优先 lang 翻译（Cobblemon 物种 key：cobblemon.species.<id>）
        String fromLang = tryLang("cobblemon.species." + id, null);
        if (fromLang != null) return fromLang;
        // 2. 回落硬编码映射
        return EN_TO_ZH.getOrDefault(id, en);
    }

    public static String toEnglish(String zh) {
        if (zh == null || zh.isEmpty()) return zh;
        String en = ZH_TO_EN.get(zh.trim());
        if (en != null) return en;
        if (EN_TO_ZH.containsKey(zh.toLowerCase().trim())) return zh.toLowerCase().trim();
        return zh;
    }

    public static String abilityToEnglish(String zh) {
        if (zh == null || zh.isEmpty()) return zh;
        String trimmed = zh.trim();
        // 1. 精确硬编码映射
        String en = ABILITY_ZH_TO_EN.get(trimmed);
        if (en != null) return en;
        // 2. 如果输入本身就是英文 ID
        String lower = trimmed.toLowerCase();
        if (ABILITY_EN_TO_ZH.containsKey(lower)) return lower;
        // 3. 反向 lang 查找：遍历所有硬编码 key，看 lang 翻译是否等于中文
        for (String key : ABILITY_EN_TO_ZH.keySet()) {
            String translated = tryLang("cobblemon.ability." + key, null);
            if (translated != null && translated.trim().equals(trimmed)) {
                return key;
            }
        }
        // 4. ★ 扩展：从 Cobblemon 的 Abilities 注册表获取所有特性 ID，反向 lang 查找
        //    这解决了"solar_power→太阳之力"这类只有 lang 翻译但没有硬编码映射的特性
        try {
            Object abilitiesInstance = com.cobblemon.mod.common.api.abilities.Abilities.class.getField("INSTANCE").get(null);
            // 尝试多种可能的方法签名来获取所有已注册的特性模板
            for (java.lang.reflect.Method m : abilitiesInstance.getClass().getMethods()) {
                String name = m.getName();
                Class<?> ret = m.getReturnType();
                // 查找返回 Map/Collection/Iterable 的方法（可能是注册表）
                if ((name.equals("getAbilities") || name.startsWith("get") || name.equals("values"))
                        && (java.util.Map.class.isAssignableFrom(ret)
                            || java.util.Collection.class.isAssignableFrom(ret)
                            || java.lang.Iterable.class.isAssignableFrom(ret))) {
                    try {
                        Object result = m.invoke(abilitiesInstance);
                        if (result == null) continue;
                        // 从返回值中提取所有可能的特性 ID
                        java.util.Set<String> candidateIds = new java.util.LinkedHashSet<>();
                        if (result instanceof java.util.Map<?, ?> map) {
                            for (Object k : map.keySet()) {
                                if (k != null) candidateIds.add(k.toString());
                            }
                        } else if (result instanceof java.util.Collection<?> col) {
                            for (Object v : col) {
                                if (v != null) {
                                    // 可能是 AbilityTemplate，尝试获取 name/showdownId
                                    String id = extractAbilityIdFromTemplate(v);
                                    if (id != null) candidateIds.add(id);
                                }
                            }
                        } else if (result instanceof java.lang.Iterable<?> it) {
                            for (Object v : it) {
                                if (v != null) {
                                    String id = extractAbilityIdFromTemplate(v);
                                    if (id != null) candidateIds.add(id);
                                }
                            }
                        }
                        // 从候选 ID 中查找中文匹配
                        for (String id : candidateIds) {
                            String cleaned = id.toLowerCase();
                            if (ABILITY_EN_TO_ZH.containsKey(cleaned)) continue; // 已在硬编码中检查过
                            String translated = tryLang("cobblemon.ability." + cleaned, null);
                            if (translated != null && translated.trim().equals(trimmed)) {
                                return cleaned;
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
        return zh;
    }

    // 辅助方法：从 AbilityTemplate 对象中尝试提取特性 ID（通过 getName/showdownId 等字段）
    private static String extractAbilityIdFromTemplate(Object template) {
        if (template == null) return null;
        try {
            for (java.lang.reflect.Method m : template.getClass().getMethods()) {
                if (m.getParameterCount() == 0
                        && (m.getName().equals("getName")
                            || m.getName().equals("name")
                            || m.getName().equals("getShowdownId")
                            || m.getName().equals("showdownId")
                            || m.getName().equals("getId")
                            || m.getName().equals("id"))) {
                    try {
                        Object val = m.invoke(template);
                        if (val != null) return val.toString();
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static List<String> abilityList() {
        return ABILITY_LIST;
    }

    public static List<String> natureList() {
        List<String> list = new ArrayList<>();
        for (String zh : NATURE_ZH_TO_EN.keySet()) {
            list.add(zh);
        }
        return list;
    }

    // ===== 招式完整列表（动态从 Cobblemon 获取 + 优先 lang 翻译） =====
    private static boolean dynamicMovesInitialized = false;
    private static final List<String> MOVE_LIST = new ArrayList<>();

    private static void ensureDynamicMoves() {
        if (dynamicMovesInitialized) return;
        try {
            List<String> allMoveIds = CobblemonBridge.getAllMoveIds();
            for (String id : allMoveIds) {
                if (id == null || id.isEmpty()) continue;
                String lowerId = id.toLowerCase().trim();
                String zh = tryLang("cobblemon.move." + lowerId, null);
                if (zh == null || zh.isEmpty()) zh = lowerId;
                if (!MOVE_EN_TO_ZH.containsKey(lowerId)) {
                    MOVE_EN_TO_ZH.put(lowerId, zh);
                    MOVE_ZH_TO_EN.put(zh, lowerId);
                }
            }
            MOVE_LIST.clear();
            MOVE_LIST.addAll(MOVE_ZH_TO_EN.keySet());
        } catch (Throwable ignored) {}
        dynamicMovesInitialized = true;
    }

    public static List<String> moveList() {
        ensureDynamicMoves();
        if (MOVE_LIST.isEmpty()) {
            MOVE_LIST.addAll(MOVE_ZH_TO_EN.keySet());
        }
        return MOVE_LIST;
    }

    // 每种宝可梦可能拥有的特性（Cobblemon常见配置 - 第一世代常见组合示例）
    // 为简化，提供按物种查找的特性列表功能（用户也可以手动输入）
    public static List<String> abilitiesForSpecies(String speciesEn) {
        if (speciesEn == null || speciesEn.isEmpty()) return new ArrayList<>();
        // 优先返回全部特性列表供下拉选择（用户也可手动输入）
        return new ArrayList<>(ABILITY_ZH_TO_EN.keySet());
    }
}