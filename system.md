# Comprehensive Summary: Minecraft Skills System for NeoForge 1.21.1

## Core System Structure
The skills system is designed to enhance the vanilla Minecraft experience by introducing a structured progression system that rewards players for engaging with various aspects of the game. The system is divided into different paths. The player gets attributed multiple paths, and each path has its own expertise levels. The player can choose to focus on one or multiple paths, allowing for a personalized gameplay experience.

### Progression Mechanics per Expertise
Each Expertise uses a level-based progression system (1-100) with several key features:

1. **Diminishing Returns**
   - Higher expertise levels provide diminishing XP returns (100% at level 1, 50% at level 50, 1% at level 99)
   - Creates natural soft caps without arbitrary limits
   - Encourages diversification without forcing it

2. **Difficulty-Based Hard Caps**
   - Easy difficulty: Maximum level 50 in any expertise
   - Normal difficulty: Maximum level 75 in any expertise
   - Hard difficulty: Maximum level 100 in any expertise
   - Preserves game balance while rewarding players who take on greater challenges

3. **Expertise-Specific Requirements**
   - Advanced abilities within each tree require minimum levels in that specific expertise
   - Prevents players from grinding one expertise to power up in another
   - Creates authentic specialization based on playstyle

### Main Level

The expertise levels contribute player level. This level is not capped, and is not 1:1 with the expertise levels. It is a separate level that is used for other systems in the modpack. The contribution of each pack falls off depending on how many levels in how many path the player has unlocked, to prevent spamming a few levels in each paths.

## Experience Acquisition per tree

Those are the three default paths in the mods, but they can be expanded with custom paths.

### Mining Expertise
- Gained primarily through mining different block types
- More valuable/rare blocks provide more expertise XP
- Diminishing returns apply to repetitive mining of the same block type

### Combat Expertise
- Earned through defeating hostile mobs
- Stronger/rarer mobs provide more expertise XP
- Boss mobs offer significant expertise boosts

### Exploration Expertise
- Based on first-time discoveries (biomes, structures, mobs, etc.)
- Each player tracks their own discoveries independently
- Rarer discoveries provide proportionally higher expertise XP

## Skill Tree Design

Each expertise features a branching skill tree with abilities that unlock at specific level thresholds:

### Level Tier Structure
- **Levels 1-25**: Basic abilities accessible to all players
- **Levels 26-50**: Advanced abilities (accessible in all difficulties)
- **Levels 51-75**: Expert abilities (Normal and Hard difficulties only)
- **Levels 76-100**: Master abilities (Hard difficulty only)

### Skill Unlocking System
- Players earn skill points when leveling up expertises
- Points can be spent to unlock abilities within that expertise's tree
- Each ability has prerequisites (both level requirements and parent skills)

## Anti-AFK & Balance Mechanisms

### Natural Deterrents
- Diminishing returns on repetitive actions discourage grinding
- Level-based requirements for advanced skills ensure authentic progression
- Difficulty-based caps prevent overpowered abilities in easier game modes

### Exploration Balance
- While exploration has finite discoveries, the quantity is balanced to reach similar progression points as other expertises
- Discovery XP is weighted to create a smooth progression curve

## UI and Player Experience

### Expertise Tracking
- Players can view current expertise levels, progress, and available skill points
- UI clearly indicates level caps based on current difficulty
- Visual representation of skill trees shows prerequisites and requirements

### Feedback Systems
- Level-up notifications inform players of new skill points and opportunities
- Clear indicators when expertise caps are reached due to difficulty
- Guidance on how to unlock more potential (increase difficulty)

## Game Integration

### Vanilla Compatibility
- System enhances vanilla gameplay without replacing core mechanics
- Skills complement existing progression systems (advancements, etc.)
- Balance ensures vanilla content remains relevant

### Multiplayer Considerations
- Per-player expertise tracking works seamlessly in multiplayer
- Personal discovery system ensures all players have progression opportunities
- Difficulty-based caps apply world-wide for consistent multiplayer experience

---

This skills system creates meaningful long-term progression while maintaining Minecraft's balance. The diminishing returns and difficulty-based caps naturally prevent exploitation without complex anti-AFK mechanisms. By requiring expertise-specific levels for advanced abilities, the system encourages authentic specialization while still rewarding diverse gameplay styles.

The result is an elegant progression system that enhances vanilla Minecraft without overwhelming it, providing players with clear goals and meaningful choices throughout their gameplay experience.