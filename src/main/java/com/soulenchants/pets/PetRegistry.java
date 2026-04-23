package com.soulenchants.pets;

import com.soulenchants.SoulEnchants;
import com.soulenchants.pets.impl.*;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Central pet catalog. Mirrors MythicRegistry / MaskRegistry. */
public final class PetRegistry {

    /** NBT keys carried on every pet egg. */
    public static final String NBT_ID    = "se_pet_id";
    public static final String NBT_LEVEL = "se_pet_level";
    public static final String NBT_XP    = "se_pet_xp";
    public static final String NBT_UID   = "se_pet_uid";

    private static final Map<String, Pet> PETS = new LinkedHashMap<>();

    private PetRegistry() {}

    public static void registerDefaults(SoulEnchants plugin) {
        PETS.clear();
        register(new WispPet(plugin));
        register(new GuardianPet(plugin));
        register(new HellhoundPet(plugin));
        register(new IceSpritePet(plugin));
        register(new EmberFoxPet(plugin));
        register(new ShadowRavenPet(plugin));
        register(new SeerPet(plugin));
    }

    public static void register(Pet p)          { PETS.put(p.getId(), p); }
    public static Pet get(String id)            { return id == null ? null : PETS.get(id); }
    public static Collection<Pet> all()         { return PETS.values(); }
}
