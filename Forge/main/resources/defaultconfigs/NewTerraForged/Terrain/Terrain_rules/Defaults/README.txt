# Default biome rule templates (ENABLED via BiomeRuleDefaults.ENABLED = true)
#
# Layout:
#   synonyms.json              - canonical name -> synonym path tokens
#   by_name/{canonical}.json   - template rule body (same schema as player Biomes/*.json)
#   by_id/{mod}/{biome}.json   - exact id override (optional)
#
# Resolution when ENABLED:
#   1) config Defaults/by_id (player editor saves land here)
#   2) classpath by_id
#   3) config / classpath by_name (path + synonyms)
#   4) else emergency BiomeRuleAutogen
#
# Player config files under config/.../Terrain_rules/Biomes/ are never overwritten when valid.
# EGF "Save rule" writes both Biomes/{id}.json and Defaults/by_id/{id}.json.
