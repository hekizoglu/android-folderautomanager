import re

f = "app/src/main/java/com/armutlu/apporganizer/domain/usecase/classify/AppClassifier.kt"
content = open(f, encoding='utf-8').read()

comm_keywords = [
    'whatsapp', 'telegram', 'signal', 'viber', 'skype', 'zoom.us', 'meet',
    'messenger', 'line.naver', 'kakao', 'wechat', 'discord', 'slack',
    'teams', 'webex', 'gotomeeting', 'jitsi', 'talkatone', 'textplus',
    'textnow', 'hushed', 'openphone', 'vonage', 'avaya', 'flock',
    'olvid', 'nextcloud.talk', 'zoho.cliq', 'revolt', 'wire',
    'tutanota', 'fastmail', 'protonmail', 'yahoo.mail', 'outlook',
    'gmail', 'spark.mail', 'airmail', 'edison.mail', 'spike.email',
    'bip.', 'turkcell.platinum',
    'ringcentral', 'twilio', 'sinch',
    'eight.by.eight', 'whereby', 'gather.town',
]

maps_keywords = [
    '.maps', 'navigation', 'waze', 'here.maps', 'osmand', 'komoot',
    'alltrails', 'citymapper', 'transit.app', 'moovit', 'rome2rio',
    'omio', 'trainline', 'raileurope', 'wanderu', 'busbud',
    'sygic', 'tomtom', 'copilot.gps', 'navmii',
    'maps.me', 'openstreetmap', 'organic.maps',
    'iett.', 'ankarakart', 'izulas', 'bursaulasimlari',
    'mapy.cz', 'citymapper',
]

music_keywords = [
    'spotify', 'deezer', 'tidal', 'apple.music', 'youtube.music',
    'soundcloud', 'bandcamp', 'napster', 'qobuz', 'amazon.music',
    'pandora', 'iheart', 'tunein', 'podcast',
    'kuku.fm', 'gaana', 'jiosaavn',
    'wynk', 'resso', 'anghami', 'boomplay', 'yandex.music',
    'shazam', 'musixmatch', 'genius.lyrics', 'soundhound',
    'fl.studio', 'bandlab', 'voloco', 'groovepad',
    'walk.band', 'cross.dj', 'djay', 'edjing', 'virtual.dj',
    'simply.piano', 'flowkey', 'yousician',
    'smule', 'fender.tone',
    'radyo7', 'kral.fm', 'powerapp.fm', 'enerji.fm',
]

video_keywords = [
    'vlc', 'mx.player', 'kodi', 'plex', 'bsplayer',
    'video.player', 'nplayer',
    'capcut', 'kinemaster', 'inshot', 'filmorago', 'vn.video',
    'premiere.rush', 'lomotif', 'vivavideo', 'splice.video',
    'magisto', 'unfold.app', 'prequel.app', 'videoleap',
    'actiondirector', 'powerdirector', 'filmmaker.pro',
]

business_keywords = [
    'salesforce', 'hubspot', 'zendesk', 'freshdesk', 'intercom',
    'pipedrive', 'zoho.crm', 'monday.com', 'basecamp', 'wrike',
    'teamwork', 'smartsheet', 'podio', 'servicenow', 'workday',
    'concur', 'expensify', 'bamboohr', 'adp.', 'gusto.',
    'docusign', 'hellosign', 'pandadoc', 'adobe.sign',
    'bionluk', 'youthall', 'kariyer.net',
    'xing.', 'glassdoor', 'indeed.',
]

auto_keywords = [
    'tesla.', 'bmw.connected', 'mercedes.', 'audi.connect', 'volkswagen.',
    'ford.pass', 'toyota.', 'honda.', 'hyundai.', 'kia.connect',
    'renault.', 'peugeot.', 'togg.', 'opel.',
    'gasbuddy', 'bp.app', 'shell.app', 'opet.', 'totalenergies.',
    'sixt.', 'enterprise.rent', 'hertz.', 'avis.', 'budget.car',
    'turo.', 'getaround', 'zipcar',
    'otopark', 'epark.', 'parkbee', 'parkopedia',
]

weather_keywords = [
    'accuweather', 'weather.channel', 'weather.underground',
    'carrot.weather', 'weather.live', 'mgm.gov.tr',
    'yandex.weather', 'rain.alarm', 'windy.com', 'ventusky',
    'weatherpro', 'weather.forecast', 'hava.durumu',
    'meteoblue', 'meteogroup',
]

personal_keywords = [
    'nova.launcher', 'apex.launcher', 'action.launcher', 'lawnchair',
    'niagara.launcher', 'microsoft.launcher', 'lightning.launcher',
    'evie.launcher', 'kiss.launcher', 'total.launcher',
    'zedge', 'walli.', 'backdrops.', 'muzei.',
    'kwgt.', 'klwp.', 'kustom.',
    'iconpack', 'icon.pack', 'delta.icons', 'linecon.',
    'wallpaper.craft', 'live.wallpaper',
]

dating_keywords = [
    'tinder', 'bumble', 'hinge.', 'okcupid', 'match.com', '.pof.',
    'badoo', 'meetic', 'happn', 'grindr', 'scruff.',
    'tastebuds', 'feeld', 'zoosk', 'eharmony',
    'coffee.meets.bagel', 'meetme', 'lovoo.',
]

beauty_keywords = [
    'sephora', 'ulta.beauty', 'lookfantastic', 'cult.beauty', 'nykaa',
    'purplle', 'supergreat', 'ipsy', 'boxycharm',
    'flormar', 'farmasi.', 'gratis.tr', 'watsons.tr',
    'youbeauty', 'glowlab', 'skincare.routine',
]

sports_keywords = [
    '.nba.', '.nfl.', '.mlb.', '.nhl.',
    'onefootball', 'sofascore', 'flashscore', 'livescore.in',
    '.espn.', 'foxsports', 'beinsports', '.dazn.',
    'eurosport', 'sky.sports', 'nbc.sports',
    'fanatik.', 'ntvspor', 'bilyoner', 'nesine.com', 'misli.com',
    '.f1.', 'formula1.', 'motogp.',
    'cricbuzz', 'cricinfo',
    'strava', 'polar.flow', 'wahoo.fitness', 'suunto.',
    'mapmyfitness', 'mapmyrun',
    'endomondo', 'runkeeper', 'sports.tracker',
]

house_keywords = [
    'philips.hue', 'lifx.', 'govee.', 'nanoleaf', 'ikea.home',
    'smartthings', 'tplink.', 'tuya.smart', 'roborock', 'eufy.home',
    'irobot.home', 'neato.robotics', 'ecovacs',
    'ring.doorbell', 'nest.cam', 'arlo.cameras', 'wyze.cam', 'blink.home', 'simplisafe',
    'rover.dog', 'chewy.com', 'petsmart', 'petco.', 'barkbox', 'wag.dog',
    'thumbtack', 'taskrabbit', 'homeadvisor', 'angi.services', 'handy.com',
    'zillow', 'redfin', 'realtor.com', 'trulia',
    'sahibinden', 'hepsiemlak', 'emlakjet', 'zingat',
]

books_keywords = [
    'kindle', 'kobo.reader', 'libby.', 'bookmate', 'pocket.article',
    'goodreads', 'scribd', 'storytel', 'blinkist',
    'overdrive', 'hoopla.digital', 'cloudlibrary',
    'bkmkitap', 'idefix.', 'kitapyurdu', 'dr.kitap',
    'comixology', 'marvel.unlimited', 'dc.universe.infinite',
    'webtoon', 'tapas.io', 'lezhin', 'mangaplus',
    'tachiyomi', 'mihon.reader',
]

parenting_keywords = [
    'youtube.kids', 'nickelodeon.', 'cartoon.network.', 'disney.junior',
    'pbs.kids', 'abcmouse', 'starfall.', 'khanacademykids',
    'toca.boca', 'sago.mini', 'budgemonster', 'pocoyo.',
    'peppa.pig', 'bluey.', 'tinylab', 'lingokids', 'teachyourmonster',
]

lifestyle_keywords = [
    'calm.com', 'headspace.com', 'insight.timer', 'buddhify',
    'ten.percent', 'breethe.', 'simple.habit', 'meditopia.',
    'sleep.cycle', 'sleepasandroid', 'pzizz.', 'noisli',
    'fabulous.coach', 'habitica', 'streaks.app', 'done.habits',
    'co.star', 'pattern.astrology', 'sanctuary.ai',
    'horoscope.', 'moonseer',
    'flo.health.period', 'clue.period', 'natural.cycles',
]

events_keywords = [
    'biletix', 'passo.bilet', 'seturperformans', 'iksv.',
    'eventbrite', 'ticketmaster', 'ticketek', 'livenation',
    'dice.fm', 'bandsintown', 'songkick',
    'stubhub', 'viagogo', 'seatgeek',
]

comics_keywords = [
    'crunchyroll', 'funimation', 'hidive.', '.vrv.',
    'bilibili.', 'niconico', 'anime.planet', 'animelab',
    'comixology', 'webtoon.', 'tapas.',
    'mangaplus', 'shonenjump',
]

art_keywords = [
    'procreate', 'sketchbook.', 'ibispaint', 'medibang', 'artflow',
    'concepts.app', 'affinity.', 'adobe.fresco', 'adobe.express',
    'canva.', 'figma.', 'vectornator',
    'adobe.illustrator', 'infinite.painter',
]

def get_new_cat(pkg, current_cat):
    p = pkg.lower()

    if current_cat in ('CAT_UTILITIES', 'CAT_SOCIAL', 'CAT_PRODUCTIVITY'):
        for kw in comm_keywords:
            if kw in p:
                return 'CAT_COMMUNICATION'

    if current_cat in ('CAT_UTILITIES', 'CAT_TRAVEL'):
        for kw in maps_keywords:
            if kw in p:
                return 'CAT_MAPS'

    if current_cat in ('CAT_ENTERTAINMENT', 'CAT_UTILITIES'):
        for kw in music_keywords:
            if kw in p:
                return 'CAT_MUSIC'

    if current_cat in ('CAT_ENTERTAINMENT', 'CAT_UTILITIES', 'CAT_PHOTOGRAPHY'):
        for kw in video_keywords:
            if kw in p:
                return 'CAT_VIDEO'

    if current_cat in ('CAT_PRODUCTIVITY', 'CAT_UTILITIES', 'CAT_SOCIAL'):
        for kw in business_keywords:
            if kw in p:
                return 'CAT_BUSINESS'

    if current_cat in ('CAT_UTILITIES', 'CAT_TRAVEL'):
        for kw in auto_keywords:
            if kw in p:
                return 'CAT_AUTO'

    if current_cat in ('CAT_UTILITIES',):
        for kw in weather_keywords:
            if kw in p:
                return 'CAT_WEATHER'

    if current_cat in ('CAT_UTILITIES', 'CAT_ENTERTAINMENT'):
        for kw in personal_keywords:
            if kw in p:
                return 'CAT_PERSONALIZATION'

    if current_cat in ('CAT_SOCIAL',):
        for kw in dating_keywords:
            if kw in p:
                return 'CAT_DATING'

    if current_cat in ('CAT_SHOPPING', 'CAT_HEALTH'):
        for kw in beauty_keywords:
            if kw in p:
                return 'CAT_BEAUTY'

    if current_cat in ('CAT_HEALTH', 'CAT_ENTERTAINMENT', 'CAT_UTILITIES'):
        for kw in sports_keywords:
            if kw in p:
                return 'CAT_SPORTS'

    if current_cat in ('CAT_UTILITIES', 'CAT_SHOPPING'):
        for kw in house_keywords:
            if kw in p:
                return 'CAT_HOUSE'

    if current_cat in ('CAT_EDUCATION', 'CAT_ENTERTAINMENT', 'CAT_UTILITIES'):
        for kw in books_keywords:
            if kw in p:
                return 'CAT_BOOKS'

    if current_cat in ('CAT_ENTERTAINMENT', 'CAT_EDUCATION'):
        for kw in parenting_keywords:
            if kw in p:
                return 'CAT_PARENTING'

    if current_cat in ('CAT_HEALTH', 'CAT_UTILITIES', 'CAT_ENTERTAINMENT'):
        for kw in lifestyle_keywords:
            if kw in p:
                return 'CAT_LIFESTYLE'

    if current_cat in ('CAT_ENTERTAINMENT', 'CAT_UTILITIES'):
        for kw in events_keywords:
            if kw in p:
                return 'CAT_EVENTS'

    if current_cat in ('CAT_ENTERTAINMENT',):
        for kw in comics_keywords:
            if kw in p:
                return 'CAT_COMICS'

    if current_cat in ('CAT_UTILITIES', 'CAT_PHOTOGRAPHY', 'CAT_PRODUCTIVITY'):
        for kw in art_keywords:
            if kw in p:
                return 'CAT_ART'

    return None

lines = content.split('\n')
moved = {}
out = []
for line in lines:
    m = re.match(r'(\s+"([^"]+)"\s+to\s+Category\.)(CAT_\w+)(.*)', line)
    if m:
        prefix, pkg, cat, suffix = m.group(1), m.group(2), m.group(3), m.group(4)
        new_cat = get_new_cat(pkg, cat)
        if new_cat:
            moved.setdefault(f"{cat} -> {new_cat}", []).append(pkg)
            line = f"{prefix}{new_cat}{suffix}"
    out.append(line)

open(f, 'w', encoding='utf-8').write('\n'.join(out))

print("Tasinan paketler:")
for transition, pkgs in sorted(moved.items(), key=lambda x: -len(x[1])):
    print(f"  {transition}: {len(pkgs)} paket")
print(f"\nToplam: {sum(len(v) for v in moved.values())} tasinma")
