# Autogenerates docs/biome-terrain-review.md from surface-biomes.toml + biome-terrain-integration.toml.
# Expands TerrainGroup sections (group.plains, nested group -> plains, bare group ids).
#
# Loop with apply-biome-terrain-review.ps1:
#   apply  -> toml
#   generate -> review (verify suggested vs allowed)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$surfacePath = Join-Path $root 'Forge\main\resources\defaultconfigs\NewTerraForged\Terrain\surface-biomes.toml'
$terrainPath = Join-Path $root 'Forge\main\resources\defaultconfigs\NewTerraForged\Terrain\biome-terrain-integration.toml'
$outPath = Join-Path $root 'docs\biome-terrain-review.md'
New-Item -ItemType Directory -Force -Path (Split-Path $outPath) | Out-Null

# Mirror TerrainGroup.java
$TerrainGroups = @{
    mountains        = @('mountains_1', 'mountains_2', 'mountains_3')
    mountains_ridge  = @('mountains_ridge_1', 'mountains_ridge_2')
    hills            = @('hills_1', 'hills_2')
    plains           = @('steppe', 'plains')
    plateau          = @('plateau')
    badlands         = @('badlands')
    dolomites        = @('dolomites')
    steppe           = @('steppe')
    dales            = @('dales')
    torridonian      = @('torridonian')
    island           = @('island_hills', 'island_plateau', 'island_mountains', 'island_flats')
    island_hills     = @('island_hills')
    island_plateau   = @('island_plateau')
    island_mountains = @('island_mountains')
    island_flats     = @('island_flats')
    island_volcano   = @('island_volcano')
    laguna           = @('laguna')
}

function Expand-TerrainKey([string]$raw) {
    $key = $raw.Trim().ToLowerInvariant()
    if ($key.StartsWith('group.')) { $key = $key.Substring(6) }
    if ($TerrainGroups.ContainsKey($key)) { return @($TerrainGroups[$key]) }
    return @($key)
}

$climate = @{}
$currentClimate = $null
Get-Content $surfacePath | ForEach-Object {
    if ($_ -match '^\[\[(.+)\]\]') { $currentClimate = $Matches[1] }
    elseif ($_ -match '^id = "(.+)"' -and $currentClimate -and $currentClimate -ne 'settings') {
        $climate[$Matches[1]] = $currentClimate
    }
}

$terrains = @(
    'steppe', 'plains', 'hills_1', 'hills_2', 'dales', 'plateau', 'badlands', 'torridonian',
    'mountains_1', 'mountains_2', 'mountains_3', 'dolomites', 'mountains_ridge_1', 'mountains_ridge_2',
    'island_hills', 'island_plateau', 'island_mountains', 'island_flats', 'island_volcano', 'laguna'
)

function Merge-Rules($a, $b) {
    if (-not $a) { return $b }
    if (-not $b) { return $a }
    return @{
        whitelist = @($a.whitelist + $b.whitelist | Select-Object -Unique)
        blacklist = @($a.blacklist + $b.blacklist | Select-Object -Unique)
    }
}

$rules = @{}
$currentSection = $null
$mode = $null
$pending = @{}  # section key -> rules before expand
Get-Content $terrainPath | ForEach-Object {
    if ($_ -match '^\[(.+)\]') {
        $currentSection = $Matches[1].Trim('"')
        if (-not $pending.ContainsKey($currentSection)) {
            $pending[$currentSection] = @{ whitelist = [System.Collections.Generic.List[string]]::new(); blacklist = [System.Collections.Generic.List[string]]::new() }
        }
        $mode = $null
    }
    elseif ($currentSection -and $_ -match '^\s*"(.+)"') {
        if ($mode -eq 'whitelist') { $pending[$currentSection].whitelist.Add($Matches[1]) | Out-Null }
        elseif ($mode -eq 'blacklist') { $pending[$currentSection].blacklist.Add($Matches[1]) | Out-Null }
    }
    elseif ($_ -match 'whitelist\s*=') { $mode = 'whitelist' }
    elseif ($_ -match 'blacklist\s*=') { $mode = 'blacklist' }
}

foreach ($sec in $pending.Keys) {
    $parsed = @{
        whitelist = @($pending[$sec].whitelist)
        blacklist = @($pending[$sec].blacklist)
    }
    if ($parsed.whitelist.Count -eq 0 -and $parsed.blacklist.Count -eq 0) { continue }
    foreach ($t in (Expand-TerrainKey $sec)) {
        $rules[$t] = Merge-Rules $rules[$t] $parsed
    }
}

function Test-Allowed([string]$biomeId, [string]$terrain) {
    $r = $rules[$terrain]
    if (-not $r) { return $true }
    if ($r.whitelist.Count -gt 0) { return $r.whitelist -contains $biomeId }
    return $r.blacklist -notcontains $biomeId
}

function Get-HeuristicTerrains([string]$biomeId, [string]$clim) {
    $n = $biomeId.ToLower()
    $rec = [System.Collections.Generic.List[string]]::new()
    $isMesa = $n -match 'badlands|mesa|outback|dryland|lush_desert|volcanic_plains|arid_mountains|barley_fields|desert_canyon|painted_mountains|bryce|sandstone_valley|ancient_sands|gravel_desert'
    $isBeach = $n -match 'beach|shore|dune_beach|gravel_beach'
    $isSwamp = $n -match 'swamp|bayou|marsh|mangrove|wetland|bog|fen|orchid_swamp|white_mangrove'
    $isJungle = $n -match 'jungle|rainforest|bamboo|tropics|tropical|fungal_jungle' -and $n -notmatch 'temperate_rainforest|cold'
    $isVolcano = $n -match 'volcano|volcanic_crater|volcanic_peaks|ashen_savanna|basalt_deltas|magma_wastes|basalt_barrera'
    $isAlpine = $n -match 'alpine|glacial|frozen|snowy|ice|tundra|siberian|frost|cold_|icy_|winter|peak|summit|highland|mountain|ridge|cliff|crag|torridonian|dolomite|yosemite|shield|granite|basalt_cliffs|white_cliffs|volcanic|scarlet|crimson|ashen|fractured'

    if ($isMesa) { $rec.Add('badlands'); return $rec }
    if ($isBeach) { $rec.Add('coast_override'); return $rec }
    if ($isVolcano) { foreach ($t in @('island_volcano', 'badlands')) { $rec.Add($t) }; return $rec }
    if ($isSwamp) {
        foreach ($t in @('dales', 'steppe', 'plains', 'island_hills', 'island_flats', 'laguna')) { $rec.Add($t) }
        return $rec
    }
    if ($isJungle) {
        foreach ($t in @('steppe', 'plains', 'dales', 'island_hills', 'island_flats', 'laguna')) { $rec.Add($t) }
        return $rec
    }
    if ($isAlpine -or $clim -eq 'alpine') {
        foreach ($t in @('mountains_1', 'mountains_2', 'mountains_3', 'dolomites', 'mountains_ridge_1', 'mountains_ridge_2', 'torridonian', 'hills_2', 'plateau', 'island_mountains', 'island_plateau')) { $rec.Add($t) }
        return $rec
    }
    if ($clim -in @('desert', 'savanna', 'cold_steppe', 'steppe')) {
        foreach ($t in @('steppe', 'plains', 'hills_1', 'hills_2', 'dales', 'plateau', 'island_hills', 'island_flats', 'island_plateau')) { $rec.Add($t) }
        return $rec
    }
    foreach ($t in @('plains', 'steppe', 'hills_1', 'hills_2', 'dales', 'plateau', 'island_hills', 'island_flats')) { $rec.Add($t) }
    return $rec
}

$jarBiomes = @{}
$mods = 'C:\curseforge\minecraft\Instances\TerraforgedTest\mods'
$jarSpecs = @{
    'Terralith_1.18.2_v2.2.6.jar' = 'terralith'
    'BiomesOPlenty-1.18.2-16.0.0.134.jar' = 'biomesoplenty'
    'RegionsUnexploredForge-0.4.1_1+1.18.2.jar' = 'regions_unexplored'
    'wn_1.18.2_terrablender_r1.0.jar' = 'wildnature'
}
foreach ($kv in $jarSpecs.GetEnumerator()) {
    $p = Join-Path $mods $kv.Key
    if (-not (Test-Path $p)) { continue }
    $ns = $kv.Value
    jar tf $p 2>$null | ForEach-Object {
        if ($_ -match "^data/$ns/worldgen/biome/([^/]+)\.json$") {
            $jarBiomes["${ns}:$($Matches[1])"] = $ns
        }
    }
}

$bygInConfig = @($climate.Keys | Where-Object { $_ -like 'byg:*' })
$missing = @($jarBiomes.Keys | Where-Object { -not $climate.ContainsKey($_) } | Sort-Object)
$badlandsWl = @()
if ($rules['badlands']) { $badlandsWl = @($rules['badlands'].whitelist) }
$mesaIds = @($climate.Keys | Where-Object { $_ -match 'badlands|mesa|outback|dryland|lush_desert|volcanic_plains|arid_mountains|barley_fields|desert_canyon|painted_mountains|bryce|sandstone_valley' } | Sort-Object)

$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add('# Biome - Terrain Review (TerraforgedTest pack)')
$lines.Add('')
$lines.Add('> Autogenerated for review. Apply with `scripts/apply-biome-terrain-review.ps1`, then regenerate.')
$lines.Add('')
$lines.Add('## Pipeline')
$lines.Add('')
$lines.Add('1. `surface-biomes.toml` climate pools')
$lines.Add('2. `BiomeSampler` picks candidate')
$lines.Add('3. `BiomeTerrainIntegration.filter()` uses expanded `biome-terrain-integration.toml` (TerrainGroup)')
$lines.Add('4. Island paint uses `island_*` / `laguna` terrain names for filtering')
$lines.Add('')
$lines.Add('## Stats')
$lines.Add('')
$lines.Add("- Biomes in surface-biomes.toml: **$($climate.Count)**")
$lines.Add("- Concrete terrains with rules: **$($rules.Keys.Count)**")
$lines.Add("- Reviewed terrain columns: **$($terrains.Count)**")
$lines.Add("- Surface JSON biomes in jars (Terralith/BOP/RU/WN): **$($jarBiomes.Count)**")
$lines.Add("- BYG biomes (config only, no JSON in jar): **$($bygInConfig.Count)**")
$lines.Add('')

$lines.Add("## Jar biomes NOT in surface-biomes.toml ($($missing.Count))")
$lines.Add('')
if ($missing.Count -eq 0) { $lines.Add('_none_') } else { foreach ($id in $missing) { $lines.Add("- $id") } }
$lines.Add('')

$lines.Add('## Mesa/Badlands biomes - badlands terrain only')
$lines.Add('')
$lines.Add('| biome | climate | in badlands whitelist? | allowed on other terrains? |')
$lines.Add('|-------|---------|------------------------|----------------------------|')
foreach ($id in $mesaIds) {
    $wl = if ($badlandsWl -contains $id) { 'yes' } else { '**NO**' }
    $elseList = @($terrains | Where-Object { $_ -ne 'badlands' -and (Test-Allowed $id $_) })
    $elseStr = if ($elseList.Count -eq 0) { '_blocked_' } else { ($elseList -join ', ') }
    $lines.Add("| $id | $($climate[$id]) | $wl | $elseStr |")
}
$lines.Add('')

$climates = @($climate.Values | Sort-Object -Unique)
foreach ($cl in $climates) {
    $bios = @($climate.GetEnumerator() | Where-Object { $_.Value -eq $cl } | ForEach-Object { $_.Key } | Sort-Object)
    $lines.Add("### $cl ($($bios.Count))")
    $lines.Add('')
    $lines.Add('| biome | allowed terrains | blocked terrains | suggested | notes |')
    $lines.Add('|-------|------------------|--------------------|-----------|-------|')
    foreach ($id in $bios) {
        $allowedList = @($terrains | Where-Object { Test-Allowed $id $_ })
        $blockedList = @($terrains | Where-Object { -not (Test-Allowed $id $_) })
        $allowed = $allowedList -join ', '
        $blocked = $blockedList -join ', '
        $suggested = (Get-HeuristicTerrains $id $cl) -join ', '
        $notes = @()
        if ($id -match 'jungle|rainforest|bamboo|tropical') { $notes += 'jungle-family' }
        if ($id -match 'swamp|bayou|marsh|mangrove') { $notes += 'swamp-family' }
        if ($id -match 'badlands|mesa|outback') { $notes += 'mesa-family' }
        if ($id -match 'beach|shore') { $notes += 'beach (coast override)' }
        if ($id -match 'island|laguna|volcano') { $notes += 'island-relevant' }
        $noteStr = $notes -join '; '
        $lines.Add("| $id | $allowed | $blocked | $suggested | $noteStr |")
    }
    $lines.Add('')
}

$lines.Add('## Potential issues for review')
$lines.Add('')
$issues = [System.Collections.Generic.List[string]]::new()
foreach ($id in ($climate.Keys | Sort-Object)) {
    $allowedCount = @($terrains | Where-Object { Test-Allowed $id $_ }).Count
    if ($allowedCount -eq 0) {
        $issues.Add("- $id - blocked everywhere")
    }
    elseif ($allowedCount -eq 1 -and (Test-Allowed $id 'badlands')) {
        if ($id -notmatch 'badlands|mesa|outback|dryland|lush_desert|volcanic|arid_mountains|barley|desert_canyon|painted|bryce|sandstone|ancient_sands|gravel_desert|red_oasis|savanna_badlands|snowy_badlands|warped_mesa|white_mesa|sierra_badlands|atacama_outback') {
            $issues.Add("- $id - only badlands terrain but name does not look like mesa")
        }
    }
}
if ($issues.Count -eq 0) { $lines.Add('_none found automatically_') } else { foreach ($i in $issues) { $lines.Add($i) } }

$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($outPath, (($lines -join "`n") + "`n"), $utf8NoBom)
Write-Output "Wrote $outPath"
Write-Output "Biomes: $($climate.Count), Rule terrains: $($rules.Keys.Count), Missing: $($missing.Count), Issues: $($issues.Count)"
