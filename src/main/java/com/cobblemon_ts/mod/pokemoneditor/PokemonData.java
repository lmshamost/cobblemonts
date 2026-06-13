package com.cobblemon_ts.mod.pokemoneditor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PokemonData {
    public boolean exists = false;
    public UUID pokemonUUID = null;
    public String speciesName = "";
    public String displayName = "";
    public String formName = "";
    public int level = 1;
    public int experience = 0;
    public int friendship = 0;
    public boolean shiny = false;
    public int ivHp = 0, ivAtk = 0, ivDef = 0, ivSpA = 0, ivSpD = 0, ivSpe = 0;
    public int evHp = 0, evAtk = 0, evDef = 0, evSpA = 0, evSpD = 0, evSpe = 0;
    public String nature = "";
    public String ability = "";
    public String move1 = "", move2 = "", move3 = "", move4 = "";
    public int move1PP = 0, move2PP = 0, move3PP = 0, move4PP = 0; // 当前PP
    public int move1MaxPP = 0, move2MaxPP = 0, move3MaxPP = 0, move4MaxPP = 0; // PP最大值(受pp_max影响)
    public int move1Raised = 0, move2Raised = 0, move3Raised = 0, move4Raised = 0; // pp_max提升等级 (0=未提升,1=已用pp_max)
    public int currentHp = 1, maxHp = 1;
    public String heldItem = "";
    public int gender = 0; // 0=雄性 1=雌性 2=无性别
    public List<String> possibleAbilities = new ArrayList<>(); // 该物种可能拥有的特性ID
}