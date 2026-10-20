import io, re, sys
sys.stdout.reconfigure(encoding="utf-8")
files = [
    r"app\src\main\java\com\intima\log\data\backup\BackupManager.kt",
    r"app\src\main\java\com\intima\log\data\db\Daos.kt",
    r"app\src\main\java\com\intima\log\data\db\IntimaDatabase.kt",
    r"app\src\main\java\com\intima\log\data\db\Partner.kt",
    r"app\src\main\java\com\intima\log\data\db\SceneTemplate.kt",
    r"app\src\main\java\com\intima\log\data\db\SexRecord.kt",
    r"app\src\main\java\com\intima\log\data\repo\RecordRepository.kt",
    r"app\src\main\java\com\intima\log\data\repo\RecordRepositoryImpl.kt",
    r"app\src\main\java\com\intima\log\domain\usecase\RecordUseCases.kt",
    r"app\src\main\java\com\intima\log\ui\home\HomeComponents.kt",
    r"app\src\main\java\com\intima\log\ui\IntimaNavHost.kt",
    r"app\src\main\java\com\intima\log\ui\me\MeScreen.kt",
    r"app\src\main\java\com\intima\log\ui\me\TagsManageScreen.kt",
    r"app\src\main\java\com\intima\log\ui\quickadd\QuickAddActivity.kt",
    r"app\src\main\java\com\intima\log\ui\record\RecordEditScreen.kt",
    r"app\src\main\java\com\intima\log\ui\record\RecordEditViewModel.kt",
    r"app\src\main\java\com\intima\log\ui\stats\StatsScreen.kt",
    r"app\src\main\java\com\intima\log\ui\template\TemplateScreen.kt",
]
for p in files:
    s = io.open(p, encoding="utf-8").read()
    n0 = len(s)
    # 删除 Protection 相关 import
    s = re.sub(r'^.*import [^\n]*Protection[^\n]*\n', '', s, flags=re.M)
    io.open(p, "w", encoding="utf-8", newline="").write(s)
    print(p, n0-len(s))
