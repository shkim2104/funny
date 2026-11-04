package game.model;

import game.model.Story.Scene;
import game.model.Story.Speaker;

/**
 * The first-character tutorial: 촌장 몽당 할아버지 walks the new hero around town, each house's keeper
 * explains what it is for, then the gatekeeper takes them into their first fight and their first
 * augment. Player.getTutorialStep() says how far along they are, so it resumes after a reload.
 */
public final class Tutorial {
    private Tutorial() {}

    /** A place on the town map the tour points at, and who keeps it. */
    public enum Place {
        INN("여관", "여관을", Speaker.INNKEEPER), SHOP("상점", "상점을", Speaker.MERCHANT), SMITH("대장간", "대장간을", Speaker.SMITH),
        QUEST("의뢰 게시판", "의뢰 게시판을", Speaker.QUEST_CLERK), GACHA("보물 뽑기", "보물 뽑기를", Speaker.GACHA_FAIRY),
        JOB("전직소", "전직소를", Speaker.JOB_MASTER), MUSEUM("박물관", "박물관을", Speaker.CURATOR),
        PORTAL("???", "보라색 \"???\" 집을", null), GATE("던전 입구", "던전 입구를", Speaker.GATEKEEPER);

        public final String label;
        /** The place with its object particle, for "…을 눌러 보세요". */
        public final String object;
        /** Who explains it; null for the house nobody keeps. */
        public final Speaker keeper;

        Place(String label, String object, Speaker keeper) {
            this.label = label;
            this.object = object;
            this.keeper = keeper;
        }
    }

    // ---------- steps (Player.getTutorialStep) ----------
    public static final int NOT_STARTED = 0;
    /** Steps 1~8: the town tour, pointing at TOUR[step - 1]. */
    public static final Place[] TOUR = {Place.INN, Place.SHOP, Place.SMITH, Place.QUEST, Place.GACHA, Place.JOB, Place.MUSEUM, Place.PORTAL};
    /** Then the dungeon gate, the first region, and the first augment. */
    public static final int GATE = TOUR.length + 1, FIRST_DUNGEON = GATE + 1, FIRST_AUGMENT = FIRST_DUNGEON + 1;
    public static final int DONE = 100;

    /** Finishing the whole tutorial pays this, once per character. */
    public static final int REWARD_POTIONS = 5, REWARD_GOLD = 200;

    public static boolean active(int step) {
        return step > NOT_STARTED && step < DONE;
    }

    /** The place the tour points at now, or null when it isn't pointing at one in town. */
    public static Place target(int step) {
        if (step >= 1 && step <= TOUR.length) return TOUR[step - 1];
        if (step == GATE) return Place.GATE;
        return null;
    }

    /** The line over the map, e.g. "여관을 눌러 보세요". */
    public static String objective(int step) {
        Place p = target(step);
        if (p != null) return p.object + " 눌러 보세요";
        if (step == FIRST_DUNGEON) return "1지역 초원을 눌러 들어가 보세요";
        if (step == FIRST_AUGMENT) return "3스테이지까지 싸워 첫 증강을 골라 보세요";
        return "";
    }

    private static final Speaker M = Speaker.MAYOR, H = Speaker.HERO;

    public static Scene welcome() {
        return Story.scene()
                .say(M, "어서 오게, 새로 온 모험가로구먼! 여기는 섬나라 아방가르드의 작은 마을이라네.")
                .say(M, "나는 이 마을 촌장, 몽당 할아버지일세. 연필처럼 짧아도 마을 일은 길게 알지, 허허.")
                .say(H, "안녕하세요! 여기서 뭘 하면 되나요?")
                .say(M, "모험을 떠나기 전에 마을부터 둘러보세. 집마다 자네를 도와줄 사람들이 있다네.")
                .say(M, "내가 연필로 동그라미 쳐 둔 곳을 눌러 보게. 먼저 저기 여관일세.")
                .end("둘러볼게요!");
    }

    /** What the keeper of `p` says on the tour. */
    public static Scene explain(Place p) {
        Speaker k = p.keeper;
        switch (p) {
            case INN:
                return Story.scene()
                        .say(k, "어이구, 손님이네... 하암. 여기는 여관이야.")
                        .say(k, "골드를 조금 내고 쉬면 HP와 MP가 전부 차오른단다. 레벨이 오를수록 값도 조금씩 올라.")
                        .say(k, "그리고 쉬는 동안 모험도 기록해 줄게. 여관에서 쉬면 저장이 된다는 것, 잊지 마.")
                        .end("고마워요!");
            case SHOP:
                return Story.scene()
                        .say(k, "어서 와요! 지우네 상점이에요. 포션, 에테르, 무기와 방어구를 팔아요.")
                        .say(k, "포션은 HP, 에테르는 MP를 채워요. 던전 들어가기 전에 꼭 챙겨 가요!")
                        .say(k, "필요 없는 물건은 판매 탭에서 팔 수 있어요. 여러 개를 골라 한 번에 팔 수도 있고요.")
                        .end("다음에 들를게요.");
            case SMITH:
                return Story.scene()
                        .say(k, "쾅! 쾅! ...오, 신참이군. 여긴 대장간이다.")
                        .say(k, "장비는 +20까지 강화할 수 있어. 골드랑 흑연 조각, 높은 단계에선 단단한 심도 필요하지.")
                        .say(k, "강화는 성공할 수도, 실패할 수도 있고, +13부터는 장비가 파괴될 수도 있어. 지우개 가루가 있으면 파괴를 한 번 막아 준다.")
                        .say(k, "파괴된 장비는 같은 장비를 재료로 +5까지 복구할 수 있고, 남는 장비는 분해해서 재료로 바꿀 수 있지. 아끼는 장비는 잠가 둬!")
                        .end("조심해서 강화할게요.");
            case QUEST:
                return Story.scene()
                        .say(k, "안녕하세요! 의뢰 게시판 담당 책갈피예요.")
                        .say(k, "마을 사람들이 몬스터 사냥이나 보스 토벌을 부탁해요. 끝내고 오면 골드와 경험치를 드려요.")
                        .say(k, "마음에 안 드는 의뢰는 골드를 조금 내고 다른 의뢰로 바꿀 수 있어요.")
                        .end("의뢰도 해 볼게요.");
            case GACHA:
                return Story.scene()
                        .say(k, "반짝반짝~! 보물 뽑기에 온 걸 환영해!")
                        .say(k, "골드를 내고 상자를 고르면 룰렛이 돌아가서 장비가 나와. 비싼 상자일수록 좋은 장비가 잘 나와!")
                        .say(k, "상자마다 등급별 확률은 미리 다 보여 줄게. 운을 시험해 봐~")
                        .end("한번 해 볼게요!");
            case JOB:
                return Story.scene()
                        .say(k, "어서 오너라. 이곳은 전직소다.")
                        .say(k, "Lv.10이 되면 1차 전직을 할 수 있고, Lv.20, 50, 70에 다음 전직이 열린다.")
                        .say(k, "차수마다 전사, 마법사, 궁수, 도적 중 다른 길을 골라도 된다. 고른 직업마다 스킬을 하나씩 배우지.")
                        .say(k, "한 번 고른 길은 되돌릴 수 없으니 신중히 정하거라.")
                        .end("잘 생각해 볼게요.");
            case MUSEUM:
                return Story.scene()
                        .say(k, "콜록, 콜록... 먼지가 많지? 여기는 박물관이라네.")
                        .say(k, "아직 전시를 준비하는 중이야. 언젠가 자네의 모험 이야기도 여기 걸리길 바라네.")
                        .end("기대할게요!");
            case PORTAL:
                return Story.scene()
                        .say(M, "...저 보라색 집 말인가? 문패에는 \"???\"라고만 쓰여 있지.")
                        .say(M, "마을에서 아무도 들어가 본 적 없는 집이라네. 안에서 가끔 이상한 빛이 새어 나온다는 소문만 있지.")
                        .say(M, "자네라면... 언젠가 그 안을 알게 될지도 모르겠구먼.")
                        .end("궁금하네요...");
            default:
                return Story.scene()
                        .say(k, "멈춰라! ...아, 모험가였군. 나는 던전 입구를 지키는 문지기 단단이다.")
                        .say(k, "이 너머에는 7개 지역이 있다. 지역마다 30스테이지를 지나면 마지막에 보스가 기다리지.")
                        .say(k, "보스를 쓰러뜨리면 다음 지역이 열린다. 처음엔 1지역 초원부터 가 보도록!")
                        .end("초원으로 갈게요!");
        }
    }

    /** After the last house: the menu on the right. */
    public static Scene menu() {
        return Story.scene()
                .say(M, "마을은 다 둘러봤구먼! 마지막으로 오른쪽 메뉴를 알려 주지.")
                .say(M, "'상태 보기'는 능력치와 장비, 스킬을 보여 주고, '장비 변경'에서 무기와 방어구를 바꿀 수 있다네.")
                .say(M, "레벨이 오르면 스탯 포인트를 4씩 받아. '스탯 분배'에서 HP, 공격력, 방어력 같은 곳에 나눠 주게.")
                .say(M, "'저장하기'로 언제든 저장할 수 있고, '설정'에서는 자동 전투, 자동 포션, 단축키 같은 편의 기능을 켤 수 있지.")
                .say(M, "이제 진짜 모험일세! 마을 오른쪽 아래 던전 입구로 가 보게. 문지기가 기다리고 있을 걸세.")
                .end("다녀올게요!");
    }

    /** Right before the first fight of the first region. */
    public static Scene firstBattle() {
        Speaker k = Speaker.GATEKEEPER;
        return Story.scene()
                .say(k, "첫 전투다! 잘 들어라.")
                .say(k, "'공격'을 누르면 공격하고, 전직한 뒤에는 '공격' 안에서 스킬도 고를 수 있다. 스킬은 MP를 쓴다.")
                .say(k, "'회복'에서는 포션과 에테르를 바로 쓸 수 있고, 위험하면 '도망'칠 수도 있다. 반반 확률이지만.")
                .say(k, "쓰러지면 마을로 실려 가고 골드의 1/4을 잃는다. 설정에서 자동 전투와 자동 포션을 켜 두면 편하다.")
                .end("싸워 볼게요!");
    }

    /** Right before the first augment window. */
    public static Scene firstAugment() {
        Speaker k = Speaker.GATEKEEPER;
        return Story.scene()
                .say(k, "3스테이지를 넘었군! 던전에서는 3스테이지마다 '증강'을 하나 고를 수 있다.")
                .say(k, "세 장 중 하나를 골라라. 이 던전을 나갈 때까지만 유지되는 힘이다. 마음에 안 들면 새로고침을 두 번 쓸 수 있다.")
                .say(k, "보스전에서는 보스의 위압 때문에 증강 효과가 조금 약해진다. 잘 골라 봐라!")
                .end("골라 볼게요!");
    }

    public static Scene done(boolean rewarded) {
        Story.Builder b = Story.scene()
                .say(M, "허허, 증강까지 골라 봤구먼! 이제 자네도 어엿한 모험가일세.");
        if (rewarded) b.say(M, "마을에서 주는 작은 선물이네. 포션 " + REWARD_POTIONS + "개와 " + REWARD_GOLD + "G를 받게.");
        return b.say(M, "궁금한 게 생기면 언제든 마을 사람들을 눌러 보게. 다들 기꺼이 알려 줄 걸세.")
                .end("고마워요, 촌장님!");
    }

    /** Clicking the 촌장 outside the tour. */
    public static Scene mayorTalk() {
        return Story.scene()
                .say(M, "오, 자네 왔는가. 마을 안내를 다시 들려줄까?")
                .end("다시 들을래요", "괜찮아요");
    }

    /** Clicking the 촌장 during the tour: where to go next. */
    public static Scene hint(int step) {
        Place p = target(step);
        String where = p == null ? "던전" : p == Place.PORTAL ? "보라색 \"???\" 집" : p.label;
        return Story.scene().say(M, "지금은 " + where + " 차례일세. 연필 동그라미를 따라가 보게.").end();
    }

    /** A keeper's short reminder outside the tour. */
    public static Scene tip(Place p) {
        Speaker k = p.keeper;
        String text;
        switch (p) {
            case INN: text = "피곤하면 언제든 와. 쉬면 HP·MP가 다 차고 모험도 저장된단다."; break;
            case SHOP: text = "포션은 넉넉히! 판매 탭에서 '전체 선택'으로 한꺼번에 팔 수도 있어요."; break;
            case SMITH: text = "+13부터는 파괴 위험이 있다. 지우개 가루를 챙기고, 아끼는 장비는 잠가 둬!"; break;
            case QUEST: text = "끝낸 의뢰는 여기 와서 보상을 받아 가세요. 마을에 돌아오면 알려 드려요!"; break;
            case GACHA: text = "비싼 상자일수록 좋은 장비 확률이 올라가! 확률표는 상자마다 볼 수 있어~"; break;
            case JOB: text = "Lv.10, 20, 50, 70. 그때마다 새 길을 고를 수 있다."; break;
            case MUSEUM: text = "전시는 아직 준비 중이라네. 조금만 더 기다려 주게."; break;
            default: text = "지역마다 30스테이지, 그리고 보스다. 3스테이지마다 증강을 고를 수 있다."; break;
        }
        return Story.scene().say(k, text).end();
    }
}
