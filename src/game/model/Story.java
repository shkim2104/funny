package game.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The final story: after the last dungeon, 이계의 존재 invades 아방가르드 and the mysterious ??? guides
 * the hero through three requests for 이계의 단서, brews a weakening potion, and sends them into the
 * fight. All the lines live here so the wording can be changed in one place.
 */
public final class Story {
    private Story() {}

    // ---------- progress (Player.getStoryStage) ----------
    public static final int NOT_STARTED = 0;
    /** Stages 1~3: that request of ??? is open. */
    public static final int FIRST_REQUEST = 1, LAST_REQUEST = 3;
    /** The potion is made: the rift can be entered. */
    public static final int POTION_MADE = 4;
    /** The boss has fallen once: the ending is seen and the passive earned. */
    public static final int ENDED = 5;

    public static boolean invaded(int stage) {
        return stage >= FIRST_REQUEST && stage <= POTION_MADE;
    }

    // ---------- the requests ----------
    public static final int PER_MATERIAL = 10;

    /** What request `stage` (1~3) asks for: material → count. */
    public static Map<StoryMaterial, Integer> request(int stage) {
        Map<StoryMaterial, Integer> need = new LinkedHashMap<>();
        List<StoryMaterial> list;
        switch (stage) {
            case 1: list = Arrays.asList(StoryMaterial.STARLIGHT_DEW, StoryMaterial.METEOR_SHARD, StoryMaterial.CONSTELLATION_SLAB); break;
            case 2: list = Arrays.asList(StoryMaterial.DARK_CORE, StoryMaterial.ETERNAL_ICE); break;
            case 3: list = Arrays.asList(StoryMaterial.SUN_EMBER, StoryMaterial.FALLEN_FEATHER); break;
            default: list = Collections.emptyList();
        }
        for (StoryMaterial m : list) need.put(m, PER_MATERIAL);
        return need;
    }

    public static String requestTitle(int stage) {
        switch (stage) {
            case 1: return "1단계 · 별이 지나간 흔적";
            case 2: return "2단계 · 힘을 담을 그릇";
            case 3: return "3단계 · 빛과 하늘의 마지막 조각";
            default: return "";
        }
    }

    public static String requestHint(int stage) {
        switch (stage) {
            case 1: return "초원, 어두운 동굴, 잊혀진 폐허의 몬스터에게서 모을 수 있어.";
            case 2: return "마왕성과 얼음 협곡의 몬스터에게서 모을 수 있어.";
            case 3: return "불지옥 화산과 천공의 성채의 몬스터에게서 모을 수 있어.";
            default: return "";
        }
    }

    public static boolean canHandIn(Player p, int stage) {
        for (Map.Entry<StoryMaterial, Integer> e : request(stage).entrySet()) {
            if (p.getStoryMaterial(e.getKey()) < e.getValue()) return false;
        }
        return true;
    }

    /** Takes the request's materials and moves to the next stage. False (nothing taken) if short. */
    public static boolean handIn(Player p) {
        int stage = p.getStoryStage();
        if (stage < FIRST_REQUEST || stage > LAST_REQUEST || !canHandIn(p, stage)) return false;
        for (Map.Entry<StoryMaterial, Integer> e : request(stage).entrySet()) p.spendStoryMaterial(e.getKey(), e.getValue());
        p.setStoryStage(stage + 1);
        return true;
    }

    // ---------- the lines ----------
    public enum Speaker {
        MYSTERY("???"), BOSS("이계의 존재"), HERO(null), NARRATION(""),
        // The town folk of the tutorial; their pictures are images/npc/{portrait}_face.png.
        MAYOR("촌장 몽당 할아버지", "mayor"), INNKEEPER("여관 주인 졸음 할머니", "innkeeper"), MERCHANT("상인 지우 아주머니", "merchant"),
        SMITH("대장장이 쇠심 아저씨", "smith"), QUEST_CLERK("의뢰소 직원 책갈피", "quest_clerk"), GACHA_FAIRY("뽑기 요정 반짝이", "gacha_fairy"),
        JOB_MASTER("전직관 붓 스승", "job_master"), CURATOR("관장 먼지 박사", "curator"), GATEKEEPER("문지기 단단이", "gatekeeper");

        private final String name;
        /** Picture key of a town NPC; null for the others. */
        public final String portrait;

        Speaker(String name) {
            this(name, null);
        }

        Speaker(String name, String portrait) {
            this.name = name;
            this.portrait = portrait;
        }

        /** The name over the line; the hero's is the player's own. */
        public String name(Player p) {
            return this == HERO ? p.getName() : name;
        }
    }

    public static final class Line {
        public final Speaker speaker;
        public final String text;

        Line(Speaker speaker, String text) {
            this.speaker = speaker;
            this.text = text;
        }
    }

    /** A run of lines, and the buttons on the last one (none = just a "다음" to close). */
    public static final class Scene {
        public final List<Line> lines;
        public final List<String> choices;

        Scene(List<Line> lines, String... choices) {
            this.lines = lines;
            this.choices = Arrays.asList(choices);
        }
    }

    static final class Builder {
        final List<Line> lines = new ArrayList<>();

        public Builder say(Speaker s, String text) {
            lines.add(new Line(s, text));
            return this;
        }

        public Scene end(String... choices) {
            return new Scene(lines, choices);
        }
    }

    static Builder scene() {
        return new Builder();
    }

    private static final Speaker M = Speaker.MYSTERY, B = Speaker.BOSS, H = Speaker.HERO, N = Speaker.NARRATION;

    /** At the end of the last dungeon's boss fight. */
    public static final String CRACK = "...그때, 하늘이 갈라지는 소리가 들렸다.";

    /** Arriving in the invaded town for the first time. */
    public static Scene intro() {
        return scene()
                .say(N, "마을로 돌아오자, 하늘 한가운데가 갈라져 있었다. 그 틈으로 별빛이 쏟아지고 있었다.")
                .say(N, "사람들이 비명을 지르며 흩어졌다. 그리고 광장에, 처음 보는 누군가가 서 있었다.")
                .say(M, "...늦지 않았구나. 천공의 성채를 넘어선 영웅, 너를 기다리고 있었어.")
                .say(H, "당신은 누구죠? 저 하늘은 대체...")
                .say(M, "내 이름은 중요하지 않아. 중요한 건 저 균열 너머에 있는 것이지.")
                .say(M, "별과 별 사이, 아무것도 없는 곳에서 태어난 신. 우리는 그것을 '이계의 존재'라고 불러.")
                .say(M, "그것은 칼로 베지 않아. 별을 떨어뜨리고, 빛을 삼키고, 우주 그 자체를 휘두르지.")
                .say(H, "그럼 지금 당장 막으러 가야죠!")
                .say(M, "지금의 너로는 닿기도 전에 사라질 거야. 그것의 힘은 이 세계의 법칙 밖에 있으니까.")
                .say(M, "하지만 방법이 있어. 그것의 힘을 깎아 내는 약, '이계 약화 물약'.")
                .say(M, "약을 만들려면 이계의 힘이 스며든 재료가 필요해. 그 힘은 네가 지나온 던전의 몬스터들에게 흘러들었지.")
                .say(M, "재료를 모아 와. 내가 약을 만들게. 첫 번째는 별이 지나간 흔적이야.")
                .end("알겠어요. 모아 올게요.");
    }

    /** Talking to ??? while a request is open. */
    public static Scene requestTalk(int stage) {
        switch (stage) {
            case 1:
                return scene()
                        .say(M, "별빛 이슬, 운석 조각, 별자리 석판 조각. 그것이 이 세계로 내려온 길에 떨어뜨린 흔적이야.")
                        .say(M, "흔적을 모으면 그것이 어디서 왔는지 알 수 있어. 초원과 동굴, 그리고 폐허를 둘러봐.")
                        .end();
            case 2:
                return scene()
                        .say(M, "이제 우주의 힘을 붙잡아 둘 그릇이 필요해. 어둠의 핵과 영원빙 결정이라면 버틸 수 있을 거야.")
                        .say(M, "마왕성과 얼음 협곡이야. 조심해, 그곳 몬스터들도 균열의 기운에 들떠 있어.")
                        .end();
            default:
                return scene()
                        .say(M, "마지막이야. 태양의 불씨와 타락한 날개깃. 빛과 하늘의 힘을 담으면 약이 완성돼.")
                        .say(M, "불지옥 화산과 천공의 성채로 가. ...거기까지 다녀온 너라면 할 수 있어.")
                        .end();
        }
    }

    /** Handing in request `stage` (1~3). */
    public static Scene handedIn(int stage) {
        switch (stage) {
            case 1:
                return scene()
                        .say(M, "...역시. 흔적이 가리키는 곳은 별의 바깥이야. 그것은 정말로 우주에서 왔어.")
                        .say(M, "고마워. 다음은 그 힘을 담아 둘 그릇이 필요해.")
                        .end();
            case 2:
                return scene()
                        .say(M, "좋아, 그릇이 단단해. 이 정도면 이계의 힘도 새어 나가지 못할 거야.")
                        .say(H, "당신은 어떻게 이런 걸 다 알고 있죠?")
                        .say(M, "...언젠가 이야기해 줄게. 지금은 마지막 재료가 먼저야.")
                        .end();
            default:
                return scene()
                        .say(N, "???가 재료를 하나씩 그릇에 담자, 그릇 안에서 작은 별이 태어나듯 빛이 일렁였다.")
                        .say(M, "...완성이야. 이계 약화 물약.")
                        .say(M, "이걸 균열에 부으면 그것의 힘이 크게 꺾일 거야. 한번 꺾인 힘은 쉽게 돌아오지 않아.")
                        .say(M, "준비가 되면 하늘의 균열로 와. 그때 내 힘도 조금 빌려줄게.")
                        .end("준비하고 올게요.");
        }
    }

    /** Clicking the rift before the potion is made. */
    public static Scene notReady() {
        return scene()
                .say(M, "지금은 무리야. 약 없이 들어가면 그것에게 닿기도 전에 별빛에 삼켜질 거야.")
                .end();
    }

    /** Clicking the rift once the potion is made: the last chance to turn back. */
    public static Scene beforeFinale() {
        return scene()
                .say(M, "준비됐어? 균열에 약을 붓는 순간, 그것이 내려올 거야.")
                .end("지금 맞서겠다", "조금 더 준비하겠다");
    }

    /** Right before the fight. */
    public static Scene finaleStart(boolean firstTime) {
        Builder b = scene();
        if (firstTime) b.say(N, "물약이 균열에 스며들자, 하늘 전체가 한 번 크게 떨렸다.");
        return b.say(M, "내 별의 힘을 너에게 걸어 둘게. 별지기의 가호야. 쓰러지더라도 한 번은 다시 일어설 수 있을 거야.")
                .say(N, "균열 너머에서, 별을 몸에 두른 무언가가 천천히 내려왔다.")
                .say(B, "...작은 별의 아이여. 너는 왜 사라질 것을 알면서 빛나려 하는가.")
                .say(H, "사라지지 않아. 여긴 우리 마을이니까!")
                .end("싸운다!");
    }

    /** After the boss falls. */
    public static Scene victory(boolean firstTime) {
        if (!firstTime) {
            return scene()
                    .say(N, "이계의 존재가 다시 별빛이 되어 흩어졌다. 균열의 흔적도 조용해졌다.")
                    .say(M, "역시 너야. 마을은 당분간 안전할 거야.")
                    .end();
        }
        return scene()
                .say(B, "...이토록 작은 빛이, 별을 꺾는가. ...기억하겠다, 아이여.")
                .say(N, "이계의 존재가 수천 개의 별빛으로 부서져 균열 속으로 빨려 들어갔다. 갈라진 하늘이 천천히 아물었다.")
                .say(M, "...해냈구나. 정말로.")
                .say(H, "당신 덕분이에요. 이제 이름을 알려 줄 수 있어요?")
                .say(M, "아직은 아니야. ...하지만 또 만나게 될 거야. 별과 별 사이, 아직 열리지 않은 길에서.")
                .say(N, "???는 미소를 남기고, 바람처럼 사라졌다.")
                .say(N, "별을 꺾은 경험이 몸에 새겨졌다. 패시브 '이계를 넘어선 자'를 얻었다! (모든 능력치 +5%)")
                .end("마을로");
    }

    /** Waking in town after losing the final fight. */
    public static Scene defeat() {
        return scene()
                .say(M, "...괜찮아? 약의 힘은 아직 남아 있어. 그것도 지금은 약해진 그대로야.")
                .say(M, "몸을 추스르고 다시 오면 돼. 초신성이 빛을 모을 때는 꼭 대비해.")
                .end();
    }

    // ---------- the passive ----------
    public static final String PASSIVE_NAME = "이계를 넘어선 자";
    public static final double PASSIVE_PCT = 0.05;

    /** A stat with the passive added: +5%, and at least +1 so small stats still gain. */
    public static int withPassive(int value) {
        return value + Math.max(1, (int) Math.round(value * PASSIVE_PCT));
    }
}
