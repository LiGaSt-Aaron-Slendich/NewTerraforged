# Default biome rule templates (DISABLED until BiomeRuleDefaults.ENABLED = true)
#
# Layout:
#   synonyms.json              — canonical name → synonym path tokens
#   by_name/{canonical}.json   — template rule body (same schema as player Biomes/*.json)
#   by_id/{mod}/{biome}.json   — exact id override (optional)
#
# Resolution when ENABLED:
#   1) by_id exact match
#   2) by_name using biome path
#   3) synonym → by_name canonical
#   4) else emergency BiomeRuleAutogen
#
# Player config files under config/.../Terrain_rules/Biomes/ are never overwritten when valid.
# Fill by_name/*.json together later; leave ENABLED=false until then.
