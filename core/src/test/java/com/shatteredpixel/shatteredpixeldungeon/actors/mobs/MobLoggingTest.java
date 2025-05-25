package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.backends.headless.HeadlessFiles;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Sleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MobLoggingTest {
    private final List<String> logs = new ArrayList<>();

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
        // dungeon and hero
        Dungeon.level = Dungeon.newLevel();
        Dungeon.hero = new Hero();
        Dungeon.hero.live();
    }


    @AfterAll
    static void tearDownGdx() {
        if (Gdx.app != null) {
            Gdx.app.exit();
        }
    }

    @BeforeEach
    void setUp() {
        // 清空所有 GLog 更新监听器，并注册一个收集日志的监听
        GLog.update.removeAll();
        GLog.update.add(text -> logs.add(text));
    }

    @AfterEach
    void tearDown() {
        GLog.update.removeAll();
        logs.clear();
    }

    /**
     * 一个简单的 DummyMob，用于触发日志。
     * 只重写最基本的抽象方法以便编译通过。
     */
    private static class DummyMob extends Mob {
        public DummyMob() {
            pos = 0;                // 位置随意
            firstAdded = true;      // 确保 onAdd() 输出日志
        }
        @Override public int defenseSkill(Char enemy) { return 0; }
        @Override public int defenseProc(Char enemy, int damage) { return damage; }

        @Override public void onAttackComplete() {}
        @Override public void die(Object cause) {}

        @Override public float lootChance() { return 0; }
        //@Override public void beckon(int cell) {}
        @Override public boolean reset() { return false; }
        public void callOnAdd() { super.onAdd(); }

        @Override
        public void notice() {
            // override to avoid null sprite in tests
        }

    }

    @Test
    void testSpawnLogging() {
        DummyMob m = new DummyMob();
        m.callOnAdd();
        assertTrue(
                logs.stream().anyMatch(
                        s -> s.contains("onAdd") && s.contains("Mob#" + m.id())
                ),
                "日志中应包含 onAdd 和 Mob#ID"
        );
    }



    @Test
    void testBuffAddAmokLogging() {
        DummyMob m = new DummyMob();
        logs.clear();
        // Buff Amok should transition to HUNTING
        Buff.affect(m, Amok.class);
        assertTrue(
                logs.stream().anyMatch(s -> s.contains("状态变更") && s.contains("HUNTING")),
                "日志中应包含 Amok 添加后状态变更到 HUNTING"
        );
    }

    @Test
    void testBuffAddTerrorLogging() {
        DummyMob m = new DummyMob();
        logs.clear();
        // Buff Terror should transition to FLEEING
        Buff.affect(m, Terror.class);
        assertTrue(
                logs.stream().anyMatch(s -> s.contains("状态变更") && s.contains("FLEEING")),
                "日志中应包含 Terror 添加后状态变更到 FLEEING"
        );
    }

    @Test
    void testDamageWakesUpLogging() {
        DummyMob m = new DummyMob();
        // put mob to sleeping
        m.state = m.SLEEPING;
        logs.clear();
        // damage should wake up and transition to WANDERING
        m.damage(1, new WandOfBlastWave());
        assertTrue(
                logs.stream().anyMatch(s -> s.contains("被打醒") && s.contains("WANDERING")),
                "日志中应包含 damage 唤醒后状态变更到 WANDERING"
        );
    }

    @Test
    void testBeckonLogging() {
        DummyMob m = new DummyMob();
        logs.clear();
        // beckon should log Wandering state change and target switch
        int targetCell = 7;
        m.beckon(targetCell);
        assertTrue(
                logs.stream().anyMatch(s -> s.contains("状态变更") && s.contains("WANDERING")),
                "日志中应包含 beckon 导致状态变更到 WANDERING"
        );
        assertTrue(
                logs.stream().anyMatch(s -> s.contains("switches target to " + targetCell)),
                "日志中应包含 beckon 切换目标到 " + targetCell
        );
    }
}
