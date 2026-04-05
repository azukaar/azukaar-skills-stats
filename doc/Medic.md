# MEDIC *(Nature → Healing & Vitality)*

## **Core Philosophy:**
**Battlefield Healer** - Keeps allies alive through active healing and potion mastery
**Radiance Engine** - Healing emits damaging pulses to nearby hostiles, rewarding aggressive support
**Resilient Specialist** - Builds endurance through constitution, turning debuffs into offensive triggers

---

## **Core Mechanic: Radiance**
Every healing action emits a damaging pulse to nearby hostile mobs. No cooldown — the healing abilities themselves have cooldowns. Invest in the Constitution tree to amplify its radius and damage, and in the Potion tree to trigger burst pulses via Cocktail.

---

## **Main Tree** *(No Aspect Requirements)*

### **Reactive Heal** (1sp, max 3)
- **Effect:** Passive — when hit, trigger Regen I (level 2 → Regen II, level 3 → Regen III)
- **Purpose:** Automatic sustain on taking damage, bridges naturally into Rogue self-damage playstyle

### **Undying Resolve** (1sp, max 3)
- **Prerequisite:** Reactive Heal
- **Effect:** Extends Reactive Heal's Regeneration duration by 3 seconds per level
- **Max Benefit:** +9 seconds (5s base → 14s at level 3)
- **Purpose:** Sustain investment — longer Regen means more total healing per hit

### **Infused Self Heal** (2sp)
- **Prerequisite:** Reactive Heal
- **Effect:** Self Heal also applies the effect of your left-hand potion without consuming it
- **Purpose:** Cross-tree synergy with Potion tree

### **✦ Radiance** (2sp, max 5)
- **Prerequisite:** Undying Resolve
- **Effect:** Passive — two triggers:
  - **Per heal:** Each heal >= 1 HP fires golden homing missiles at nearby angry mobs, dealing 10-50% of heal amount as damage (scales with level)
  - **On Regen activation:** Burst dealing 5-25% of max health as flat damage (scales with level)
- **Targets:** 2 to 10 closest angry mobs within 16 blocks (scales with level)
- **Purpose:** Core mechanic of the Medic tree — turns healing into offense

### **Touch Heal** (2sp, max 5)
- **Prerequisite:** Radiance
- **Effect:** Heal a targeted ally for 8+4/level HP (1min cooldown). Triggers Radiance from healed ally's position. Harms undead
- **Max Benefit:** 28 HP (14 hearts) at level 5
- **Purpose:** Single-target ally healing + offensive Radiance

### **Infused Touch Heal** (2sp)
- **Prerequisite:** Touch Heal
- **Effect:** Touch Heal also applies your off-hand potion effect to the target without consuming it
- **Purpose:** Potion delivery to allies

### **AoE Heal** (2sp, max 5)
- **Prerequisite:** Radiance
- **Effect:** Heal all nearby allies for 6+2/level HP in a 5+2/level block radius (1min cooldown). Triggers Radiance from each healed ally's position. Harms undead
- **Max Benefit:** 16 HP, 15 block radius at level 5
- **Purpose:** Group healing + multi-point Radiance

### **Infused AoE Heal** (2sp)
- **Prerequisite:** AoE Heal
- **Effect:** AoE Heal also applies your off-hand potion effect to all healed allies without consuming it
- **Purpose:** Mass potion distribution

---

## **Potion Tree**

### **Alchemy** (2sp, max 3)
- **Effect:** Passive — all brewed potions last 20% longer per level
- **Purpose:** Entry point and quality foundation for the branch

### **Alchemy's Boon** (2sp, max 3) 🔒
- **Prerequisite:** Alchemy
- **Aspect Requirement:** Nature 10
- **Effect:** Active — brew a Healing potion from nothing (5min cooldown). Additional brew skills add their potion type to this single action
- **Purpose:** The one active brew skill

### **Brew Regen** (1sp, max 3)
- **Prerequisite:** Alchemy's Boon
- **Effect:** Passive — Alchemy's Boon also produces a Regeneration potion. Level = potion tier (Regen I → II → III)

### **Brew Resistance** (1sp, max 3)
- **Prerequisite:** Alchemy's Boon
- **Effect:** Passive — Alchemy's Boon also produces a Resistance potion. Level = potion tier (Resistance I → II → III)

### **Brew Strength** (1sp, max 3)
- **Prerequisite:** Alchemy's Boon
- **Effect:** Passive — Alchemy's Boon also produces a Strength potion. Level = potion tier (Strength I → II → III)

### **Brew o' plenty** (3sp, max 2)
- **Prerequisite:** Brew Regen lvl 3
- **Effect:** Alchemy's Boon brews one more potion per level

### **Cocktail** (3sp) 🔒
- **Prerequisites:** Brew Regen + Brew Resistance + Brew Strength
- **Aspect Requirement:** Nature 20
- **Effect:** Active — consume one of each potion type from inventory simultaneously. Triggers a Radiance pulse multiplied by the number of potion types consumed. Damage is 5 + (5 x potion level) (so starts at 10) (5min cooldown)
- **Purpose:** Burst Radiance trigger, rewards maintaining all four potion types

### **Overdose** (3sp) 🔒
- **Prerequisite:** Cocktail
- **Aspect Requirement:** Nature 50
- **Effect:** Active — consume ALL potions in your inventory simultaneously, triggering a massive Radiance burst. Scales with total potions consumed (10min cooldown)
- **Purpose:** Powerful burst opener — stays relevant even after Expert Cocktail automates regular Cocktail

### **Radiance Potion** (2sp, max 3)
- **Prerequisite:** Cocktail
- **Effect:** Alchemy's Boon also produces a Radiance Potion — when consumed, temporarily boosts Radiance damage and max distance for 30 seconds. (Cocktail and Overdose seek those first to ensure the effect is applied to current usage)
- **Purpose:** On-demand Radiance amplification

---

## **Capstone**

### **Expert Infused Heal** (3sp) 🔒
- **Prerequisites:** Infused Self Heal + Infused Touch Heal + Infused AoE Heal + Radiance Potion
- **Aspect Requirement:** Nature 80
- **Effect:** All Infused healing skills now trigger cocktail, consuming the potion to the (player only) target(s) and triggering radiance
- **Purpose:** Removes left-hand restriction, enables fluid multi-potion usage

---

## **Constitution Tree**

### **Hardy** (1sp, max 5)
- **Effect:** Healing or Regen triggers absorption hearts (1min cooldown). Level = absorption hearts granted per 5 hearts healed (1 → 2 → 3 → 4 → 5) / for Regen X heart per level of regen
- **Purpose:** Converts healing output into a defensive buffer

### **Cleanse** (2sp)
- **Prerequisite:** Hardy
- **Effect:** Active — remove all negative effects from yourself, triggers a Radiance pulse that damages 3hp per effects level per effects (3min cooldown)
- **Purpose:** Personal debuff removal that feeds into Radiance offensively

### **Heal Expert** (1sp, max 10)
- **Prerequisite:** Cleanse
- **Effect:** Increase all healing abilities (heal from stats tree, reactive (for +duration on regen) touch and AoE) potency by 20% per level
- **Max Benefit:** +200% at level 10

### **Blinding Light** (2sp)
- **Prerequisite:** Heal Expert
- **Effect:** Radiance pulses also apply Blindness to hit mobs for 3 seconds
- **Purpose:** Offensive utility on top of Radiance damage

### **Reactive Cleanse** (3sp) 🔒
- **Prerequisite:** Cleanse
- **Aspect Requirement:** Nature 40
- **Effect:** Passive — automatically cleanse debuffs as they are applied. Each blocked debuff triggers a Radiance pulse (Same damage as Cleanse)
- **Purpose:** Immunity that converts debuffs into offensive Radiance triggers — strong synergy with Rogue's self-inflicted bleed/poison

### **Venomous Rebound** (2sp)
- **Prerequisite:** Reactive Cleanse
- **Effect:** Cleansed debuffs (via Cleanse or Reactive Cleanse) have their effect added to the Radiance burst that follows the cleanse
- **Purpose:** Punishes enemies for applying debuffs, rewards Rogue self-poisoning loop

### **Radiance Amplifier** (3sp, max 3)
- **Prerequisites:** Blinding Light + Venomous Rebound
- **Effect:** Flat damage multiplier on all Radiance pulses per level (×1.10 → ×1.20 → ×1.30)
- **Purpose:** Raw Radiance damage investment, required for Expert Cocktail capstone

---

## **Progression Gates Summary**

| Nature Aspect | Unlocked |
|--------------|----------|
| **10** | Alchemy's Boon |
| **20** | Cocktail |
| **40** | Reactive Cleanse |
| **60** | Overdose + Expert Heal |
| **80** | Expert Cocktail |

---

## **Synergy Examples**

### **Radiance Maximiser:**
1. **Radiance** → pulse on every heal
2. **Radiance Radius** (max) + **Radiance Amplifier** (max) → wide, hard-hitting pulses
3. **Reactive Cleanse** → every blocked debuff fires a pulse
4. **Overdose** opener → massive burst, then Cocktail mid-fight

### **Full Potion Medic:**
1. **Alchemy's Boon** + all Brew skills + **Skillful Brewer** → produce 3 of every potion per cast
2. **Infused Touch/AoE Heal** → deliver potions to allies without consuming
3. **Expert Heal** → draw from hotbar freely
4. **Expert Cocktail** → Cocktail fires automatically on every Infused heal

### **Rogue Synergy Loop:**
1. **Reactive Heal** → Rogue self-damage triggers Regen passively
2. **Reactive Cleanse** → Rogue's self-inflicted bleed/poison is auto-cleansed, firing Radiance
3. **Venomous Rebound** → cleansed poison reflected back to attacker
4. **Hardy** → healing from Self Heal generates absorption buffer

### **Combat Opener:**
1. **Overdose** → dump all inventory potions for massive Radiance burst
2. **Cocktail** → mid-fight burst pulses
3. **Expert Cocktail** → passive Cocktail on every Infused heal throughout fight
4. **Alchemy's Boon** + **Skillful Brewer** → restock 3× potions for next fight

---

## **Total Investment**
- **Main Tree only:** 12-18 SP for core healing + Radiance
- **Potion focused:** 20-28 SP for full brewing + Cocktail
- **Constitution focused:** 15-22 SP for full debuff/Radiance scaling
- **Full capstone build:** 50+ SP across all three trees
