package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.backends.headless.HeadlessFiles;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAggression;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import org.junit.jupiter.api.*;
import java.util.HashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class MobLoggingTest2 {
    private final List<String> logs = new ArrayList<>();
    private static TestLevel testLevel;

    @BeforeAll
    static void initLocale() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @BeforeAll
    static void initEnvironment() {
        // locale
        Locale.setDefault(Locale.ENGLISH);
        // headless Gdx setup
        HeadlessApplicationConfiguration cfg = new HeadlessApplicationConfiguration();
        new HeadlessApplication(new ApplicationAdapter() {}, cfg);
        Gdx.files = new HeadlessFiles();

        // Create a test level
        testLevel = new TestLevel();
        Dungeon.level = testLevel;

        // Initialize hero
        Dungeon.hero = new Hero();
        Dungeon.hero.pos = 50; // Set a position away from edges
        Dungeon.hero.live();

        // Initialize Actor system
        Actor.clear();
        Actor.add(Dungeon.hero);
    }

    @AfterAll
    static void tearDownGdx() {
        if (Gdx.app != null) {
            Gdx.app.exit();
        }
    }

    @BeforeEach
    void setUp() {
        // Clear all GLog update listeners and register one to collect logs
        GLog.update.removeAll();
        GLog.update.add(text -> logs.add(text));
        logs.clear();

        // Clear mobs from level
        testLevel.mobs.clear();
    }

    @AfterEach
    void tearDown() {
        GLog.update.removeAll();
        logs.clear();
        Actor.clear();
        Actor.add(Dungeon.hero);
    }

    /**
     * Test level implementation for testing
     */
    private static class TestLevel extends Level {
        public TestLevel() {
            setSize(20, 20);
            for (int i = 0; i < length(); i++) {
                map[i] = Terrain.EMPTY;
                passable[i] = true;
                avoid[i] = false;
                solid[i] = false;
                losBlocking[i] = false;
            }
            // Ensure blobs is non-null for buildFlagMaps()
            this.blobs = new java.util.HashMap<>();
            // Ensure mobs is non-null for test setup
            this.mobs = new HashSet<>();
            buildFlagMaps();
            heroFOV = new boolean[length()];
            for (int i = 0; i < heroFOV.length; i++) {
                heroFOV[i] = true; // Everything visible for testing
            }
        }

        @Override
        protected boolean build() { return true; }
        @Override
        protected void createMobs() {}
        @Override
        protected void createItems() {}
        @Override
        public String tilesTex() { return null; }
        @Override
        public String waterTex() { return null; }

        @Override
        public Mob createMob() {
            return new TestMob();
        }
    }

    /**
     * Test mob implementation
     */
    private static class TestMob extends Mob {
        public TestMob() {
            pos = 100; // Default position
            HP = HT = 10;
            defenseSkill = 5;
            state = SLEEPING; // Default state
            alignment = Alignment.ENEMY;
            fieldOfView = new boolean[Dungeon.level.length()];
        }

        @Override
        public int attackSkill(Char target) { return 10; }
        @Override
        public int damageRoll() { return 5; }
        @Override
        public int drRoll() { return 1; }
        @Override
        public void die(Object cause) { super.die(cause); }

        // Make onAdd public for testing
        @Override
        public void onAdd() { super.onAdd(); }

        // Expose state for testing
        public void setState(AiState newState) { this.state = newState; }
        public AiState getState() { return this.state; }
    }

    // Test 1: Spawn logging
    @Test
    void testSpawnLogging() {
        TestMob mob = new TestMob();
        mob.onAdd();

        assertTrue(logs.stream().anyMatch(
                s -> s.contains("spawned at") && s.contains("Mob#" + mob.id())
        ), "Should log mob spawn with position");
    }

    // Test 2: Buff state transitions
    @Test
    void testBuffStateTransitions() {
        TestMob mob = new TestMob();

        // Test Amok → HUNTING
        logs.clear();
        Buff.affect(mob, Amok.class);
        assertTrue(logs.stream().anyMatch(
                s -> s.contains("name State changed → HUNTING (Amok/AllyBuff)")
        ), "Amok should transition to HUNTING");

        // Test Terror → FLEEING
        logs.clear();
        Buff.affect(mob, Terror.class);
        assertTrue(logs.stream().anyMatch(
                s -> s.contains("State changed → FLEEING ") && s.contains("Fear")
        ), "Terror should transition to FLEEING");

        // Test Sleep → SLEEPING
        logs.clear();
        mob.setState(mob.HUNTING); // Change state first
        Buff.affect(mob, Sleep.class);
        assertTrue(logs.stream().anyMatch(
                s -> s.contains("State changed → SLEEPING")
        ), "Sleep should transition to SLEEPING");
    }

    // Test 3: Remove buff state transitions
    @Test
    void testRemoveBuffStateTransitions() {
        TestMob mob = new TestMob();
        Terror terror = Buff.affect(mob, Terror.class);
        mob.enemySeen = true;

        logs.clear();
        mob.remove(terror);

        assertTrue(logs.stream().anyMatch(
                s -> s.contains("State changed → HUNTING ") && s.contains("Fear removed, remaining alert")
        ), "Removing terror with enemy seen should transition to HUNTING");
    }

    // Test 4: Act method Terror/Dread transition
    @Test
    void testActTerrorTransition() {
        TestMob mob = new TestMob();
        Actor.add(mob);
        Buff.affect(mob, Terror.class);

        logs.clear();
        mob.act();

        assertTrue(logs.stream().anyMatch(
                s -> s.contains("State changed → FLEEING")
        ), "Act should log FLEEING transition when Terror is present");
    }

    // Test 5: Aggro logging
    @Test
    void testAggroLogging() {
        TestMob mob = new TestMob();
        mob.setState(mob.WANDERING);

        logs.clear();
        mob.aggro(Dungeon.hero);



        assertTrue(logs.stream().anyMatch(
                s -> s.contains("Not in PASSIVE state, switching state → HUNTING")
        ), "Aggro should log state change to HUNTING");
    }

    // Test 6: Clear enemy logging
    @Test
    void testClearEnemyLogging() {
        TestMob mob = new TestMob();
        mob.enemy = Dungeon.hero;
        mob.setState(mob.HUNTING);

        logs.clear();
        mob.clearEnemy();

        assertTrue(logs.stream().anyMatch(
                s -> s.contains("Target lost, state changed → WANDERING")
        ), "Clear enemy should log state change to WANDERING");
    }

    // Test 7: Damage wake up logging
    @Test
    void testDamageWakeUpLogging() {
        TestMob mob = new TestMob();
        mob.setState(mob.SLEEPING);

        logs.clear();
        mob.damage(1, new WandOfBlastWave());

        assertTrue(logs.stream().anyMatch(
                s -> s.contains("Woken by attack, state changed → WANDERING")
        ), "Damage should log wake up transition");
    }

    // Test 8: Damage alert logging
    @Test
    void testDamageAlertLogging() {
        TestMob mob = new TestMob();
        mob.setState(mob.WANDERING);

        logs.clear();
        mob.damage(1, new WandOfBlastWave());

        assertTrue(logs.stream().anyMatch(
                s -> s.contains("is alerted!")
        ), "Damage should log alert status");
    }

    // Test 9: DefenseProc target switching
    @Test
    void testDefenseProcTargetSwitching() {
        TestMob mob = new TestMob();
        mob.setState(mob.WANDERING);

        logs.clear();
        mob.defenseProc(Dungeon.hero, 5);

        assertTrue(logs.stream().anyMatch(
                s -> s.contains("mob is now switch targeting ROGUE!")
        ), "DefenseProc should log target switching");


    }

    // Test 10: Beckon logging
    @Test
    void testBeckonLogging() {
        TestMob mob = new TestMob();
        mob.setState(mob.SLEEPING);
        int targetCell = 123;

        logs.clear();
        mob.beckon(targetCell);

        assertTrue(logs.stream().anyMatch(
                s -> s.contains("name switching state → WANDERING")
        ), "Beckon should log state change");


    }

    // Test 11: Choose enemy state transitions
    @Test
    void testChooseEnemyStateTransitions() {
        TestMob mob = new TestMob();
        mob.setState(mob.HUNTING);
        mob.alignment = Char.Alignment.ENEMY;

        // Create a mob with aggression
        TestMob targetMob = new TestMob();
        targetMob.pos = 101;
        Buff.affect(targetMob, StoneOfAggression.Aggression.class);
        testLevel.mobs.add(targetMob);
        Actor.add(targetMob);

        // Set up field of view
        mob.fieldOfView[targetMob.pos] = true;

        logs.clear();
        mob.chooseEnemy();

        assertTrue(logs.stream().anyMatch(
                s -> s.contains("name State changed → HUNTING")
        ), "Choose enemy should log HUNTING state when finding aggression target");
    }

    // Test 12: Level spawnMob logging
    @Test
    void testLevelSpawnMobLogging() {
        // Mock GameScene.add to avoid null pointer
        GameScene.scene = null; // Ensure it's null so GameScene.add does nothing

        logs.clear();
        boolean spawned = testLevel.spawnMob(5);

        if (spawned) {
            assertTrue(logs.stream().anyMatch(
                    s -> s.contains("emerges from the shadows!")
            ), "SpawnMob should log mob emergence");
        }
    }

    // Test 13: State transition in AI states
    @Test
    void testSleepingAwaken() {
        TestMob mob = new TestMob();
        mob.setState(mob.SLEEPING);
        mob.enemy = Dungeon.hero;

        logs.clear();
        mob.SLEEPING.awaken(true);

        assertTrue(logs.stream().anyMatch(
                s -> s.contains("name SLEEPING → Awakened, state changed → HUNTING")
        ), "Awaken with enemy in FOV should log HUNTING transition");
    }

    // Test 14: Wandering notice enemy
    @Test
    void testWanderingNoticeEnemy() {
        TestMob mob = new TestMob();
        mob.setState(mob.WANDERING);
        mob.enemy = Dungeon.hero;

        logs.clear();
        mob.WANDERING.noticeEnemy();

        assertTrue(logs.stream().anyMatch(
                s -> s.contains("is alerted and notices")
        ), "Notice enemy should log alert");

        assertTrue(logs.stream().anyMatch(
                s -> s.contains("WANDERING → Target spotted, state changed → HUNTING")
        ), "Notice enemy should log state transition");
    }

    // Test 15: Multiple state transitions in sequence
    @Test
    void testStateTransitionSequence() {
        TestMob mob = new TestMob();
        Actor.add(mob);

        // Sleep → Wake → Hunt → Flee → Wander
        mob.setState(mob.SLEEPING);
        logs.clear();

        // Wake up
        mob.damage(1, Dungeon.hero);
        assertTrue(logs.stream().anyMatch(s -> s.contains("WANDERING")));

        // Hunt
        mob.aggro(Dungeon.hero);
        assertTrue(logs.stream().anyMatch(s -> s.contains("HUNTING")));

        // Flee
        Buff.affect(mob, Terror.class);
        assertTrue(logs.stream().anyMatch(s -> s.contains("FLEEING")));

        // Back to wandering
        mob.remove(mob.buff(Terror.class));
        assertTrue(logs.stream().anyMatch(s -> s.contains("WANDERING") || s.contains("HUNTING")));
    }
}
