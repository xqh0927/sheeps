DROP TABLE IF EXISTS levels;
DROP TABLE IF EXISTS user_task;
DROP TABLE IF EXISTS task;
DROP TABLE IF EXISTS sign_record;
DROP TABLE IF EXISTS point_record;
DROP TABLE IF EXISTS exchange_record;
DROP TABLE IF EXISTS shop_items;
DROP TABLE IF EXISTS user_items;
DROP TABLE IF EXISTS level_unlock;
DROP TABLE IF EXISTS login_token;
DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS config;
DROP TABLE IF EXISTS leaderboard;
DROP TABLE IF EXISTS backup_save_log;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id TEXT PRIMARY KEY,
    phone TEXT UNIQUE,
    username TEXT NOT NULL,
    avatar TEXT,
    points INTEGER DEFAULT 0,
    created_at INTEGER NOT NULL
);

CREATE TABLE login_token (
    phone TEXT PRIMARY KEY,
    code TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

CREATE TABLE level_unlock (
    user_id TEXT,
    level_id INTEGER,
    unlocked_at INTEGER NOT NULL,
    PRIMARY KEY (user_id, level_id),
    FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE user_items (
    user_id TEXT,
    item_type TEXT,
    count INTEGER DEFAULT 0,
    PRIMARY KEY (user_id, item_type),
    FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE shop_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    name_en TEXT,
    description_en TEXT,
    name_tw TEXT,
    description_tw TEXT,
    name_ja TEXT,
    description_ja TEXT,
    name_ko TEXT,
    description_ko TEXT,
    image_url TEXT,
    item_type TEXT NOT NULL,
    points_price INTEGER NOT NULL,
    stock INTEGER DEFAULT 100
);

CREATE TABLE exchange_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL,
    shop_item_id INTEGER NOT NULL,
    item_type TEXT NOT NULL,
    count INTEGER NOT NULL,
    points_cost INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY(user_id) REFERENCES users(id),
    FOREIGN KEY(shop_item_id) REFERENCES shop_items(id)
);

CREATE TABLE point_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL,
    type TEXT NOT NULL,
    amount INTEGER NOT NULL,
    source TEXT NOT NULL,
    remaining_points INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE sign_record (
    user_id TEXT,
    sign_date TEXT,
    streak INTEGER NOT NULL,
    points_rewarded INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    PRIMARY KEY (user_id, sign_date),
    FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE task (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    name_en TEXT,
    description_en TEXT,
    name_tw TEXT,
    description_tw TEXT,
    name_ja TEXT,
    description_ja TEXT,
    name_ko TEXT,
    description_ko TEXT,
    target_count INTEGER NOT NULL,
    points_reward INTEGER NOT NULL
);

CREATE TABLE user_task (
    user_id TEXT,
    task_id TEXT,
    task_date TEXT,
    progress INTEGER DEFAULT 0,
    is_completed INTEGER DEFAULT 0,
    is_rewarded INTEGER DEFAULT 0,
    PRIMARY KEY (user_id, task_id, task_date),
    FOREIGN KEY(user_id) REFERENCES users(id),
    FOREIGN KEY(task_id) REFERENCES task(id)
);

CREATE TABLE notice (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    title_en TEXT,
    content_en TEXT,
    title_tw TEXT,
    content_tw TEXT,
    title_ja TEXT,
    content_ja TEXT,
    title_ko TEXT,
    content_ko TEXT,
    type TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

CREATE TABLE config (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

CREATE TABLE leaderboard (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL,
    level_id INTEGER NOT NULL,
    score INTEGER NOT NULL,
    clear_time_ms INTEGER NOT NULL,
    achieved_at INTEGER NOT NULL,
    FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE levels (
    level_id INTEGER PRIMARY KEY,
    difficulty INTEGER,
    layout_data TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

CREATE TABLE backup_save_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL,
    save_data TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY(user_id) REFERENCES users(id)
);

-- 导入初始多语言配置数据
INSERT INTO task (id, name, description, name_en, description_en, name_tw, description_tw, name_ja, description_ja, name_ko, description_ko, target_count, points_reward) VALUES 
('PLAY_3_GAMES', '小试牛刀', '游玩3局游戏', 'First Steps', 'Play 3 games', '小試牛刀', '遊玩3局遊戲', '初めの一歩', '3回ゲームをプレイする', '첫걸음', '게임 3회 플레이', 3, 20),
('PLAY_5_GAMES', '大显身手', '游玩5局游戏', 'Master Show', 'Play 5 games', '大顯身手', '遊玩5局遊戲', '実力発揮', '5回ゲームをプレイする', '기량 발휘', '게임 5회 플레이', 5, 40),
('SIGN_IN_ONCE', '每日晨曦', '完成一次签到', 'Daily Dawn', 'Complete a sign-in', '每日晨曦', '完成一次簽到', '毎日の夜明け', 'ログインチェックを完了する', '매일 아침', '로그인 체크 완료', 1, 10);

INSERT INTO notice (title, content, title_en, content_en, title_tw, content_tw, title_ja, content_ja, title_ko, content_ko, type, created_at) VALUES 
('国风消消乐2.0盛大开启', '喜迎全新水墨修真版本，多重福利送不停！', 'Folk Match 2.0 Grand Opening', 'Celebrate the new Ink-and-Wash cultivation version with multiple benefits!', '國風消消樂2.0盛大開啟', '喜迎全新水墨修真版本，多重福利送不停！', '国風パズル2.0グランドオープン', '新しい水墨修行バージョンを迎え、複数の特典が満載！', '국풍 퍼즐 2.0 그랜드 오픈', '새로운 수묵 수행 버전을 맞이하여 다양한 혜택이 가득!', 'ACTIVITY', 1720000000000),
('版本优化公告', '优化了层叠卡牌飞入和重叠状态计算性能', 'Version Optimization Notice', 'Optimized performance for cascading card entry and overlapping state calculation', '版本優化公告', '優化了層疊卡牌飛入和重疊狀態計算性能', 'バージョン最適化のお知らせ', '重ね合わせカードの入場と重複状態の計算パフォーマンスを最適化しました', '버전 최적화 안내', '버전 최적화 안내', 'UPDATE', 1720000000100);

INSERT INTO shop_items (name, description, name_en, description_en, name_tw, description_tw, name_ja, description_ja, name_ko, description_ko, item_type, points_price, stock) VALUES 
('乾坤符 (Undo)', '撤销上一步操作', 'Qiankun Charm', 'Undo the last operation', '乾坤符', '撤銷上一步操作', '乾坤符', '前の操作を取り消す', '건곤부', '이전 조작 취소', 'UNDO', 20, 100),
('缩地咒 (MoveOut)', '从卡槽移出前三张卡牌', 'Earth-Shrink Spell', 'Move the first three cards out of the slot', '縮地咒', '從卡槽移出前三張卡牌', '縮地呪', 'スロットから最初の3枚のカードを移動する', '축지법', '슬롯에서 앞의 카드 3장 꺼내기', 'MOVEOUT', 30, 80),
('流沙契 (Shuffle)', '重新打乱桌面剩余卡牌', 'Quicksand Treaty', 'Reshuffle remaining cards on the board', '流沙契', '重新打亂桌面剩餘卡牌', '流砂契', 'ボード上に残ったカードを再シャッフルする', '유사계', '보드에 남은 카드 섞기', 'SHUFFLE', 20, 99),
('还魂丹 (Revive)', '消除失败时免除惩罚复活', 'Resurrection Pill', 'Resurrect and avoid penalties when you fail', '還魂丹', '消除失敗時免除懲罰復活', '還魂丹', '失敗時にペナルティなしで復活する', '환혼단', '실패 시 페널티 없이 부활', 'REVIVE', 50, 20),
('天眼符 (Hint)', '自动高亮出一组可消卡牌', 'Heaven-Eye Charm', 'Automatically highlight a matching set', '天眼符', '自動高亮出一組可消卡牌', '天眼符', '一致するセットを自动的にハイライトする', '천안부', '일치하는 세트를 자동으로 하이라이트', 'HINT', 15, 150),
('雷震子 (Bomb)', '直接炸毁卡槽中最后2张卡牌', 'Thunder Strike', 'Directly destroy the last two cards in the slot', '雷震子', '直接炸毀卡槽中最後2張卡牌', '雷震子', 'スロット内の最後の2枚のカードを直接破壊する', '뇌진자', '슬롯의 마지막 카드 2장 파괴', 'BOMB', 40, 30),
('太极牌 (Joker)', '作为任意图案卡牌与前两张直接凑对消除', 'Tai Chi Joker', 'Acts as any tile to complete a match', '太極牌', '作為任意圖案卡牌與前兩張直接湊對消除', '太極牌', '任意のカードとしてマッチを完了する', '태극패', '임의의 카드로 간주하여 매치 완료', 'JOKER', 60, 10),
('双倍符 (Double)', '结算时获得双倍积分卡', 'Double Charm', 'Double points earned upon level clearance', '雙倍符', '結算時獲得雙倍積分卡', '倍増符', 'クリア時に獲得ポイントが2倍になります', '더블 부적', '클리어 시 획득 포인트가 2배가 됩니다', 'DOUBLE_POINTS', 25, 50);

INSERT INTO config (key, value) VALUES 
('level_2_unlock_points', '50'),
('level_3_unlock_points', '100'),
('level_4_unlock_points', '200'),
('sign_rewards', '20,20,30,30,40,50,100');

CREATE TABLE IF NOT EXISTS app_version (
    version_code INTEGER PRIMARY KEY,
    version_name TEXT NOT NULL,
    apk_url TEXT NOT NULL,
    update_log TEXT,
    is_force_update INTEGER DEFAULT 0,
    created_at INTEGER NOT NULL
);

INSERT INTO app_version (version_code, version_name, apk_url, update_log, is_force_update, created_at) VALUES
(1, '1.0.0', 'https://pub-xxxxxx.r2.dev/sheeps_v1.0.0.apk', '初始版本发布', 0, 1720000000000);

INSERT INTO shop_items (name, description, name_en, description_en, name_tw, description_tw, name_ja, description_ja, name_ko, description_ko, item_type, points_price, stock) VALUES 
('水墨江山 (卡牌皮肤)', '古典水墨底色与墨金边框', 'Ink Landscape Skin', 'Classic ink wash background with dark gold frame', '水墨江山', '古典水墨底色與墨金邊框', '水墨山水', '古典的な水墨画の背景とダークゴールドのフレーム', '수묵강산', '고전적인 수묵화 배경과 다크 골드 프레임', 'SKIN_INK', 200, 9999),
('赛博霓虹 (卡牌皮肤)', '电子科幻线条与极光发光外框', 'Cyber Neon Skin', 'Electronic sci-fi lines with neon glowing frame', '賽博霓虹', '電子科幻線條與極光發光外框', 'サイバーネオン', 'ネオンに光るフレームを備えた電子SFライン', '사이버 네온', '네온으로 빛나는 프레임이 있는 전자 공상 과학 라인', 'SKIN_CYBER', 500, 9999);


-- 联机对战匹配队列
CREATE TABLE IF NOT EXISTS matchmaking_queue (
    player_id TEXT PRIMARY KEY,
    joined_at INTEGER NOT NULL,
    matched_game_id TEXT,
    matched_opponent TEXT,
    game_seed INTEGER,
    duel_level INTEGER
);

-- 跨实例 WebSocket 消息转发队列
CREATE TABLE IF NOT EXISTS game_commands (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    game_id TEXT NOT NULL,
    sender_id TEXT NOT NULL,
    command_data TEXT NOT NULL,
    created_at INTEGER NOT NULL
);
