package com.armutlu.apporganizer.domain.usecase

import com.armutlu.apporganizer.data.remote.AppDatabaseService
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppClassifier @Inject constructor(
    private val appDatabaseService: AppDatabaseService
) {

    // Paket adına göre kesin kategori eşlemesi — keyword'den önce kontrol edilir
    private val exactMatchMap = mapOf(
        // ── SOCIAL ──────────────────────────────────────────────────────────
        "com.facebook.katana"                        to Category.CAT_SOCIAL,
        "com.facebook.lite"                          to Category.CAT_SOCIAL,
        "com.instagram.android"                      to Category.CAT_SOCIAL,
        "com.twitter.android"                        to Category.CAT_SOCIAL,
        "com.whatsapp"                               to Category.CAT_SOCIAL,
        "com.whatsapp.w4b"                           to Category.CAT_SOCIAL,
        "org.telegram.messenger"                     to Category.CAT_SOCIAL,
        "org.telegram.messenger.web"                 to Category.CAT_SOCIAL,
        "org.thunderdog.challegram"                  to Category.CAT_SOCIAL,
        "com.discord"                                to Category.CAT_SOCIAL,
        "com.snapchat.android"                       to Category.CAT_SOCIAL,
        "com.zhiliaoapp.musically"                   to Category.CAT_SOCIAL,
        "com.ss.android.ugc.trill"                   to Category.CAT_SOCIAL,
        "com.facebook.orca"                          to Category.CAT_SOCIAL,
        "com.linkedin.android"                       to Category.CAT_SOCIAL,
        "com.reddit.frontpage"                       to Category.CAT_SOCIAL,
        "com.pinterest"                              to Category.CAT_SOCIAL,
        "com.tumblr"                                 to Category.CAT_SOCIAL,
        "com.quora.android"                          to Category.CAT_SOCIAL,
        "kik.android"                                to Category.CAT_SOCIAL,
        "com.viber.voip"                             to Category.CAT_SOCIAL,
        "com.skype.raider"                           to Category.CAT_SOCIAL,
        "com.google.android.talk"                    to Category.CAT_SOCIAL,
        "com.bereal.ft"                              to Category.CAT_SOCIAL,
        "com.nostr.project.nostromo"                 to Category.CAT_SOCIAL,
        "app.bsky.social"                            to Category.CAT_SOCIAL,
        "com.instagram.threads"                      to Category.CAT_SOCIAL,
        "com.vkontakte.android"                      to Category.CAT_SOCIAL,
        "ru.ok.android"                              to Category.CAT_SOCIAL,
        "jp.naver.line.android"                      to Category.CAT_SOCIAL,
        "com.kakao.talk"                             to Category.CAT_SOCIAL,
        "com.tencent.mm"                             to Category.CAT_SOCIAL,
        "org.thoughtcrime.securesms"                 to Category.CAT_SOCIAL,
        "chat.signal"                                to Category.CAT_SOCIAL,
        "com.wire"                                   to Category.CAT_SOCIAL,
        "com.element.messenger.android"              to Category.CAT_SOCIAL,
        "com.mastodon.android"                       to Category.CAT_SOCIAL,
        "com.twitter.android.lite"                   to Category.CAT_SOCIAL,
        "com.lemon.lvoice"                           to Category.CAT_SOCIAL,
        "com.truecaller"                             to Category.CAT_SOCIAL,
        // TR sosyal
        "com.turkcell.bip"                           to Category.CAT_SOCIAL,
        "com.turkcell.bip.lite"                      to Category.CAT_SOCIAL,

        // ── GAMES ────────────────────────────────────────────────────────────
        "com.valvesoftware.android.steam.steamlink"  to Category.CAT_GAMES,
        "com.playstation.mobilegames"                to Category.CAT_GAMES,
        "com.supercell.clashofclans"                 to Category.CAT_GAMES,
        "com.supercell.clashroyale"                  to Category.CAT_GAMES,
        "com.supercell.brawlstars"                   to Category.CAT_GAMES,
        "com.supercell.hayday"                       to Category.CAT_GAMES,
        "com.king.candycrushsaga"                    to Category.CAT_GAMES,
        "com.king.candycrushsodasaga"                to Category.CAT_GAMES,
        "com.mojang.minecraftpe"                     to Category.CAT_GAMES,
        "com.roblox.client"                          to Category.CAT_GAMES,
        "com.pubg.krmobile"                          to Category.CAT_GAMES,
        "com.tencent.ig"                             to Category.CAT_GAMES,
        "com.garena.game.freefire"                   to Category.CAT_GAMES,
        "com.epicgames.fortnite"                     to Category.CAT_GAMES,
        "com.activision.callofduty.shooter"          to Category.CAT_GAMES,
        "com.miHoYo.GenshinImpact"                  to Category.CAT_GAMES,
        "com.miHoYo.HonkaiStarRail"                 to Category.CAT_GAMES,
        "com.plarium.raidlegends"                    to Category.CAT_GAMES,
        "com.gamedevltd.modernstrike"                to Category.CAT_GAMES,
        "com.ea.game.pvzfree.row"                    to Category.CAT_GAMES,
        "com.ea.games.r3_row"                        to Category.CAT_GAMES,
        "com.ea.game.nfs14_row"                      to Category.CAT_GAMES,
        "com.ea.game.fifa14_row"                     to Category.CAT_GAMES,
        "com.fc.lc.lite"                             to Category.CAT_GAMES,
        "com.miniclip.eightballpool"                 to Category.CAT_GAMES,
        "com.outfit7.mytalkingtom2"                  to Category.CAT_GAMES,
        "com.imangi.templerun2"                      to Category.CAT_GAMES,
        "com.kiloo.subwaysurf"                       to Category.CAT_GAMES,
        "com.halfbrick.fruitninja"                   to Category.CAT_GAMES,
        "com.rovio.angrybirdsreloaded"               to Category.CAT_GAMES,
        "com.nianticlabs.pokemongo"                  to Category.CAT_GAMES,
        "com.zynga.livepoker"                        to Category.CAT_GAMES,
        "com.playrix.gardenscapes"                   to Category.CAT_GAMES,
        "com.playrix.homescapes"                     to Category.CAT_GAMES,
        "com.bigfish.gamesmanager"                   to Category.CAT_GAMES,
        "com.chess"                                  to Category.CAT_GAMES,
        "com.lichess.mobileapp"                      to Category.CAT_GAMES,
        "com.nintendo.zara"                          to Category.CAT_GAMES,
        "com.sega.soniccd.free"                      to Category.CAT_GAMES,
        "com.gameloft.android.ANMP.GloftA9HM"        to Category.CAT_GAMES,
        "com.yodo1.crossyroad"                       to Category.CAT_GAMES,
        "me.games.wordle"                            to Category.CAT_GAMES,
        "com.scopely.monopolygo"                     to Category.CAT_GAMES,
        "com.sandboxol.indiegame.block"              to Category.CAT_GAMES,

        // ── ENTERTAINMENT ────────────────────────────────────────────────────
        "com.netflix.mediaclient"                    to Category.CAT_ENTERTAINMENT,
        "com.spotify.music"                          to Category.CAT_ENTERTAINMENT,
        "com.google.android.youtube"                 to Category.CAT_ENTERTAINMENT,
        "com.google.android.youtube.tv"              to Category.CAT_ENTERTAINMENT,
        "com.amazon.avod.thirdpartyclient"           to Category.CAT_ENTERTAINMENT,
        "com.disney.disneyplus"                      to Category.CAT_ENTERTAINMENT,
        "tv.twitch.android.app"                      to Category.CAT_ENTERTAINMENT,
        "com.zhiliaoapp.musically.go"                to Category.CAT_ENTERTAINMENT,
        "com.hulu.plus"                              to Category.CAT_ENTERTAINMENT,
        "com.apple.android.music"                    to Category.CAT_ENTERTAINMENT,
        "com.soundcloud.android"                     to Category.CAT_ENTERTAINMENT,
        "com.deezer.android"                         to Category.CAT_ENTERTAINMENT,
        "com.tidal.android"                          to Category.CAT_ENTERTAINMENT,
        "com.bandsintown.app"                        to Category.CAT_ENTERTAINMENT,
        "com.shazam.android"                         to Category.CAT_ENTERTAINMENT,
        "tunein.player"                              to Category.CAT_ENTERTAINMENT,
        "com.iheartradio.android"                    to Category.CAT_ENTERTAINMENT,
        "com.pandora.android"                        to Category.CAT_ENTERTAINMENT,
        "com.google.android.apps.youtube.music"      to Category.CAT_ENTERTAINMENT,
        "com.vanced.android.youtube"                 to Category.CAT_ENTERTAINMENT,
        "org.videolan.vlc"                           to Category.CAT_ENTERTAINMENT,
        "com.mxtech.videoplayer.ad"                  to Category.CAT_ENTERTAINMENT,
        "com.crunchyroll.crunchyroid"                to Category.CAT_ENTERTAINMENT,
        "com.funimation.android"                     to Category.CAT_ENTERTAINMENT,
        "com.ivi.tv"                                 to Category.CAT_ENTERTAINMENT,
        "com.kinopoisk.android"                      to Category.CAT_ENTERTAINMENT,
        "com.imdb.mobile"                            to Category.CAT_ENTERTAINMENT,
        "com.google.android.videos"                  to Category.CAT_ENTERTAINMENT,
        "com.plex.android"                           to Category.CAT_ENTERTAINMENT,
        "com.jellyfin.androidtv"                     to Category.CAT_ENTERTAINMENT,
        "com.emby.androidmobile"                     to Category.CAT_ENTERTAINMENT,
        "com.kodi"                                   to Category.CAT_ENTERTAINMENT,
        "com.audible.application"                    to Category.CAT_ENTERTAINMENT,
        "com.podbean.app"                            to Category.CAT_ENTERTAINMENT,
        "com.pocket.casts"                           to Category.CAT_ENTERTAINMENT,
        "fm.player"                                  to Category.CAT_ENTERTAINMENT,
        // TR eğlence
        "com.blu.tv"                                 to Category.CAT_ENTERTAINMENT,
        "com.exxen.android"                          to Category.CAT_ENTERTAINMENT,
        "com.gain.tv"                                to Category.CAT_ENTERTAINMENT,
        "com.mubi.android"                           to Category.CAT_ENTERTAINMENT,
        "com.karnaval.android"                       to Category.CAT_ENTERTAINMENT,

        // ── SHOPPING ─────────────────────────────────────────────────────────
        "com.amazon.mShop.android.shopping"          to Category.CAT_SHOPPING,
        "com.ebay.mobile"                            to Category.CAT_SHOPPING,
        "com.alibaba.aliexpresshd"                   to Category.CAT_SHOPPING,
        "com.trendyol.android"                       to Category.CAT_SHOPPING,
        "com.hepsiburada.android"                    to Category.CAT_SHOPPING,
        "com.n11.android"                            to Category.CAT_SHOPPING,
        "com.sahibinden.android"                     to Category.CAT_SHOPPING,
        "com.letgo.android"                          to Category.CAT_SHOPPING,
        "com.shein.shopping"                         to Category.CAT_SHOPPING,
        "com.wish.android"                           to Category.CAT_SHOPPING,
        "com.joom.shopping"                          to Category.CAT_SHOPPING,
        "com.shopify.android"                        to Category.CAT_SHOPPING,
        "com.etsy.android"                           to Category.CAT_SHOPPING,
        "com.mercadolibre"                           to Category.CAT_SHOPPING,
        "com.flipkart.android"                       to Category.CAT_SHOPPING,
        "com.myntra.android"                         to Category.CAT_SHOPPING,
        "com.walmart.android"                        to Category.CAT_SHOPPING,
        "com.target.ui"                              to Category.CAT_SHOPPING,
        "com.groupon.android"                        to Category.CAT_SHOPPING,
        "com.vinted.android"                         to Category.CAT_SHOPPING,
        "com.depop"                                  to Category.CAT_SHOPPING,
        "com.poshmark.app"                           to Category.CAT_SHOPPING,
        "com.mercari.android"                        to Category.CAT_SHOPPING,
        "com.offerup"                                to Category.CAT_SHOPPING,
        "com.olx.sa.android"                         to Category.CAT_SHOPPING,
        "com.lazada.android"                         to Category.CAT_SHOPPING,
        "com.shopee.sg"                              to Category.CAT_SHOPPING,
        "com.tokopedia.tkpd"                         to Category.CAT_SHOPPING,
        "com.bukalapak.android"                      to Category.CAT_SHOPPING,
        "com.pinduoduo.android"                      to Category.CAT_SHOPPING,
        "com.jd.android.activity"                    to Category.CAT_SHOPPING,
        "com.ikea.companion"                         to Category.CAT_SHOPPING,
        "com.zara.android"                           to Category.CAT_SHOPPING,
        "com.hm.android"                             to Category.CAT_SHOPPING,
        "com.uniqlo.global"                          to Category.CAT_SHOPPING,
        // TR alışveriş
        "com.gittigidiyor.android"                   to Category.CAT_SHOPPING,
        "com.modanisa.android"                       to Category.CAT_SHOPPING,
        "com.lcwaikiki.mobile"                       to Category.CAT_SHOPPING,
        "com.boyner.android"                         to Category.CAT_SHOPPING,
        "com.getir.mobile"                           to Category.CAT_SHOPPING,

        // ── FINANCE ──────────────────────────────────────────────────────────
        "com.paypal.android.p2pmobile"               to Category.CAT_FINANCE,
        "com.venmo"                                  to Category.CAT_FINANCE,
        "com.cashapp"                                to Category.CAT_FINANCE,
        "com.robinhood.android"                      to Category.CAT_FINANCE,
        "com.etrade.android"                         to Category.CAT_FINANCE,
        "com.fidelity.android"                       to Category.CAT_FINANCE,
        "com.tdbank"                                 to Category.CAT_FINANCE,
        "com.chase.sig.android"                      to Category.CAT_FINANCE,
        "com.bankofamerica.onlinebanking"             to Category.CAT_FINANCE,
        "com.wellsfargo.mobile"                      to Category.CAT_FINANCE,
        "com.coinbase.android"                       to Category.CAT_FINANCE,
        "com.binance.dev"                            to Category.CAT_FINANCE,
        "com.kraken.invest.app"                      to Category.CAT_FINANCE,
        "com.kucoin.exchange"                        to Category.CAT_FINANCE,
        "io.metamask"                                to Category.CAT_FINANCE,
        "com.trustwallet.app"                        to Category.CAT_FINANCE,
        "com.bitcoin.mwallet"                        to Category.CAT_FINANCE,
        "com.exodus.bitcoin.cryptocurrency.wallet"   to Category.CAT_FINANCE,
        "com.revolut.revolut"                        to Category.CAT_FINANCE,
        "com.n26.android"                            to Category.CAT_FINANCE,
        "com.wise.android"                           to Category.CAT_FINANCE,
        "com.remitly.android"                        to Category.CAT_FINANCE,
        "com.westernunion.app"                       to Category.CAT_FINANCE,
        "com.intuit.quickbooks.android"              to Category.CAT_FINANCE,
        "com.intuit.mobile.ios.turbotax"             to Category.CAT_FINANCE,
        "com.acorns.android"                         to Category.CAT_FINANCE,
        "com.mint.android"                           to Category.CAT_FINANCE,
        "com.ynab.ynab"                              to Category.CAT_FINANCE,
        "com.monzo.android"                          to Category.CAT_FINANCE,
        "com.starlingbank.android"                   to Category.CAT_FINANCE,
        "com.klarna.shopping"                        to Category.CAT_FINANCE,
        "com.afterpay.android"                       to Category.CAT_FINANCE,
        // TR finans
        "com.ziraat.android.phone"                   to Category.CAT_FINANCE,
        "com.akbank.android"                         to Category.CAT_FINANCE,
        "com.yapikredibanka.android"                 to Category.CAT_FINANCE,
        "com.garantibbva.android.phone"              to Category.CAT_FINANCE,
        "com.halkbank.android"                       to Category.CAT_FINANCE,
        "com.vakifbank.mobile"                       to Category.CAT_FINANCE,
        "com.isbankasi.android"                      to Category.CAT_FINANCE,
        "com.turkiyefinans.android"                  to Category.CAT_FINANCE,
        "com.param.android"                          to Category.CAT_FINANCE,
        "tr.com.papara"                              to Category.CAT_FINANCE,
        "com.moka.android"                           to Category.CAT_FINANCE,
        "com.iyzico.android"                         to Category.CAT_FINANCE,
        "com.paribu.android"                         to Category.CAT_FINANCE,
        "com.btcturk.android"                        to Category.CAT_FINANCE,

        // ── HEALTH ───────────────────────────────────────────────────────────
        "com.google.android.apps.fitness"            to Category.CAT_HEALTH,
        "com.fitbit.FitbitMobile"                    to Category.CAT_HEALTH,
        "com.samsung.health"                         to Category.CAT_HEALTH,
        "com.garmin.android.apps.connectmobile"      to Category.CAT_HEALTH,
        "com.strava.android"                         to Category.CAT_HEALTH,
        "com.nike.plusgps"                           to Category.CAT_HEALTH,
        "com.adidas.running"                         to Category.CAT_HEALTH,
        "com.runtastic.android"                      to Category.CAT_HEALTH,
        "com.endomondo.android"                      to Category.CAT_HEALTH,
        "com.myfitnesspal.android"                   to Category.CAT_HEALTH,
        "com.loseit"                                 to Category.CAT_HEALTH,
        "com.noom.android.v2"                        to Category.CAT_HEALTH,
        "com.headspace.android"                      to Category.CAT_HEALTH,
        "com.calm.android"                           to Category.CAT_HEALTH,
        "com.insight.timer"                          to Category.CAT_HEALTH,
        "com.betterme.health"                        to Category.CAT_HEALTH,
        "com.peloton.android"                        to Category.CAT_HEALTH,
        "com.jefit.android"                          to Category.CAT_HEALTH,
        "com.strongapp.android"                      to Category.CAT_HEALTH,
        "com.freska.android"                         to Category.CAT_HEALTH,
        "com.webmd.android"                          to Category.CAT_HEALTH,
        "com.ada.app"                                to Category.CAT_HEALTH,
        "com.practo.android"                         to Category.CAT_HEALTH,
        "com.clue.android"                           to Category.CAT_HEALTH,
        "com.flo.health"                             to Category.CAT_HEALTH,
        "com.glow.android"                           to Category.CAT_HEALTH,
        "com.lifesum.android"                        to Category.CAT_HEALTH,
        "com.happycow.android"                       to Category.CAT_HEALTH,
        "com.seven"                                  to Category.CAT_HEALTH,
        "com.freeletics.mobile"                      to Category.CAT_HEALTH,
        "com.dailyburn.android"                      to Category.CAT_HEALTH,
        "com.cronometer.android"                     to Category.CAT_HEALTH,
        "com.google.android.apps.wellbeing"          to Category.CAT_HEALTH,
        // TR sağlık
        "com.doktortakvimi.doktortakvimi"            to Category.CAT_HEALTH,

        // ── EDUCATION ────────────────────────────────────────────────────────
        "com.duolingo"                               to Category.CAT_EDUCATION,
        "com.duolingo.duolingoabc"                   to Category.CAT_EDUCATION,
        "com.busuu.android.sp"                       to Category.CAT_EDUCATION,
        "com.babbel.mobile.android.default"          to Category.CAT_EDUCATION,
        "com.memrise.app"                            to Category.CAT_EDUCATION,
        "com.drops.android.dropsapp"                 to Category.CAT_EDUCATION,
        "com.pimsleur.android"                       to Category.CAT_EDUCATION,
        "com.rosettastone.mobile.courseware"         to Category.CAT_EDUCATION,
        "com.udemy.android"                          to Category.CAT_EDUCATION,
        "com.coursera.android"                       to Category.CAT_EDUCATION,
        "com.skillshare.skillshare"                  to Category.CAT_EDUCATION,
        "com.edx.mobile"                             to Category.CAT_EDUCATION,
        "org.khanacademy.android"                    to Category.CAT_EDUCATION,
        "com.brilliant.android"                      to Category.CAT_EDUCATION,
        "com.codecademy.android"                     to Category.CAT_EDUCATION,
        "com.sololearn"                              to Category.CAT_EDUCATION,
        "io.flutter.app.sololearn"                   to Category.CAT_EDUCATION,
        "com.mimo.android"                           to Category.CAT_EDUCATION,
        "com.datacamp.android"                       to Category.CAT_EDUCATION,
        "com.linkedin.android.learning"              to Category.CAT_EDUCATION,
        "com.classdojo.android"                      to Category.CAT_EDUCATION,
        "com.chegg.android"                          to Category.CAT_EDUCATION,
        "com.photomath.android"                      to Category.CAT_EDUCATION,
        "com.mathway.app"                            to Category.CAT_EDUCATION,
        "com.wolfram.wolframalpha"                   to Category.CAT_EDUCATION,
        "com.anki.android"                           to Category.CAT_EDUCATION,
        "com.cram.flashcard"                         to Category.CAT_EDUCATION,
        "com.quizlet.quizletandroid"                 to Category.CAT_EDUCATION,
        "com.readera.book.reader"                    to Category.CAT_EDUCATION,
        "com.scribd.app.reader0"                     to Category.CAT_EDUCATION,
        "com.blinkist.android"                       to Category.CAT_EDUCATION,
        "com.ted.android"                            to Category.CAT_EDUCATION,
        "com.nationalgeographic.android"             to Category.CAT_EDUCATION,
        // TR eğitim
        "com.eba.android"                            to Category.CAT_EDUCATION,
        "com.turkcell.akademi"                       to Category.CAT_EDUCATION,
        "com.kelimekartlari.android"                 to Category.CAT_EDUCATION,

        // ── TRAVEL ───────────────────────────────────────────────────────────
        "com.ubercab"                                to Category.CAT_TRAVEL,
        "com.ubercab.driver"                         to Category.CAT_TRAVEL,
        "com.lyft.android"                           to Category.CAT_TRAVEL,
        "com.booking"                                to Category.CAT_TRAVEL,
        "com.airbnb.android"                         to Category.CAT_TRAVEL,
        "com.flightradar24.free"                     to Category.CAT_TRAVEL,
        "com.tripadvisor.tripadvisor"                to Category.CAT_TRAVEL,
        "com.google.android.apps.maps"               to Category.CAT_TRAVEL,
        "com.yandex.maps"                            to Category.CAT_TRAVEL,
        "com.waze"                                   to Category.CAT_TRAVEL,
        "com.expedia.android"                        to Category.CAT_TRAVEL,
        "com.kayak.android"                          to Category.CAT_TRAVEL,
        "net.skyscanner.android.main"                to Category.CAT_TRAVEL,
        "com.hotels.android"                         to Category.CAT_TRAVEL,
        "com.hostelworld.android"                    to Category.CAT_TRAVEL,
        "com.google.android.apps.navi"               to Category.CAT_TRAVEL,
        "com.here.app.maps"                          to Category.CAT_TRAVEL,
        "com.sygic.aura"                             to Category.CAT_TRAVEL,
        "com.tomtom.gplay.speedcams"                 to Category.CAT_TRAVEL,
        "com.maps.offline.navigator"                 to Category.CAT_TRAVEL,
        "com.transit.android"                        to Category.CAT_TRAVEL,
        "com.citymapper.android"                     to Category.CAT_TRAVEL,
        "com.moovit.moovitapp"                       to Category.CAT_TRAVEL,
        "com.rome2rio.android"                       to Category.CAT_TRAVEL,
        "de.schildbach.oeffi"                        to Category.CAT_TRAVEL,
        "com.ryanair.cheapflights"                   to Category.CAT_TRAVEL,
        "com.turkish.airlines"                       to Category.CAT_TRAVEL,
        "com.delta.mobile"                           to Category.CAT_TRAVEL,
        "com.united.mobile"                          to Category.CAT_TRAVEL,
        "com.ihg.apps.android"                       to Category.CAT_TRAVEL,
        "com.marriott.android"                       to Category.CAT_TRAVEL,
        "com.hilton.hiltonandroid"                   to Category.CAT_TRAVEL,
        "com.xe.currency"                            to Category.CAT_TRAVEL,
        "com.passportparking.mobile"                 to Category.CAT_TRAVEL,
        // TR seyahat
        "com.obilet.android"                         to Category.CAT_TRAVEL,
        "com.biletall.android"                       to Category.CAT_TRAVEL,
        "com.enuygun.android"                        to Category.CAT_TRAVEL,
        "com.tatilbudur.android"                     to Category.CAT_TRAVEL,
        "com.voltran.iett"                           to Category.CAT_TRAVEL,
        "com.ist.istanbul"                           to Category.CAT_TRAVEL,
        "com.belbim.istanbulkart"                    to Category.CAT_TRAVEL,
        "tr.gov.iett.android"                        to Category.CAT_TRAVEL,
        "com.ankamobil.android"                      to Category.CAT_TRAVEL,
        "com.bitaksi.android"                        to Category.CAT_TRAVEL,
        "com.invio.shofer"                           to Category.CAT_TRAVEL,

        // ── PRODUCTIVITY ─────────────────────────────────────────────────────
        "com.openai.chatgpt"                         to Category.CAT_PRODUCTIVITY,
        "com.openai.android"                         to Category.CAT_PRODUCTIVITY,
        "com.deepseek.app"                           to Category.CAT_PRODUCTIVITY,
        "ai.deepseek.app"                            to Category.CAT_PRODUCTIVITY,
        "com.anthropic.claude"                       to Category.CAT_PRODUCTIVITY,
        "com.google.android.apps.bard"               to Category.CAT_PRODUCTIVITY,
        "com.google.android.apps.gemini"             to Category.CAT_PRODUCTIVITY,
        "com.microsoft.copilot"                      to Category.CAT_PRODUCTIVITY,
        "com.microsoft.bing"                         to Category.CAT_PRODUCTIVITY,
        "com.perplexity.app"                         to Category.CAT_PRODUCTIVITY,
        "io.character.ai"                            to Category.CAT_PRODUCTIVITY,
        "com.inflection.pi"                          to Category.CAT_PRODUCTIVITY,
        "com.google.android.gm"                      to Category.CAT_PRODUCTIVITY,
        "com.google.android.calendar"                to Category.CAT_PRODUCTIVITY,
        "com.google.android.keep"                    to Category.CAT_PRODUCTIVITY,
        "com.google.android.apps.docs"               to Category.CAT_PRODUCTIVITY,
        "com.google.android.apps.sheets"             to Category.CAT_PRODUCTIVITY,
        "com.google.android.apps.presentations"      to Category.CAT_PRODUCTIVITY,
        "com.google.android.apps.tasks"              to Category.CAT_PRODUCTIVITY,
        "com.google.android.apps.drive"              to Category.CAT_PRODUCTIVITY,
        "com.microsoft.office.word"                  to Category.CAT_PRODUCTIVITY,
        "com.microsoft.office.excel"                 to Category.CAT_PRODUCTIVITY,
        "com.microsoft.office.powerpoint"            to Category.CAT_PRODUCTIVITY,
        "com.microsoft.office.outlook"               to Category.CAT_PRODUCTIVITY,
        "com.microsoft.teams"                        to Category.CAT_PRODUCTIVITY,
        "com.microsoft.onenote"                      to Category.CAT_PRODUCTIVITY,
        "com.microsoft.skydrive"                     to Category.CAT_PRODUCTIVITY,
        "com.microsoft.launcher"                     to Category.CAT_PRODUCTIVITY,
        "us.zoom.videomeetings"                      to Category.CAT_PRODUCTIVITY,
        "com.slack"                                  to Category.CAT_PRODUCTIVITY,
        "com.notion.id"                              to Category.CAT_PRODUCTIVITY,
        "com.evernote"                               to Category.CAT_PRODUCTIVITY,
        "com.todoist.android.Todoist"                to Category.CAT_PRODUCTIVITY,
        "com.asana.android"                          to Category.CAT_PRODUCTIVITY,
        "com.trello"                                 to Category.CAT_PRODUCTIVITY,
        "com.atlassian.android.jira.core"            to Category.CAT_PRODUCTIVITY,
        "com.basecamp.android"                       to Category.CAT_PRODUCTIVITY,
        "com.monday.android"                         to Category.CAT_PRODUCTIVITY,
        "io.clickup.mobile.android"                  to Category.CAT_PRODUCTIVITY,
        "com.obsidian.mobile"                        to Category.CAT_PRODUCTIVITY,
        "md.obsidian"                                to Category.CAT_PRODUCTIVITY,
        "com.bear.write"                             to Category.CAT_PRODUCTIVITY,
        "com.standard.notes"                         to Category.CAT_PRODUCTIVITY,
        "net.joplinapp.mobile"                       to Category.CAT_PRODUCTIVITY,
        "com.agilebits.onepassword"                  to Category.CAT_PRODUCTIVITY,
        "com.lastpass.lpandroid"                     to Category.CAT_PRODUCTIVITY,
        "com.bitwarden.mobile"                       to Category.CAT_PRODUCTIVITY,
        "com.dropbox.android"                        to Category.CAT_PRODUCTIVITY,
        "com.box.android"                            to Category.CAT_PRODUCTIVITY,
        "com.adobe.acrobat.reader"                   to Category.CAT_PRODUCTIVITY,
        "jp.co.cybozu.garoon"                        to Category.CAT_PRODUCTIVITY,
        "com.grammarly.android"                      to Category.CAT_PRODUCTIVITY,
        "com.hemingwayapp.android"                   to Category.CAT_PRODUCTIVITY,
        "com.google.android.apps.meetme"             to Category.CAT_PRODUCTIVITY,
        "com.webex.meetings"                         to Category.CAT_PRODUCTIVITY,
        "com.ringcentral.android"                    to Category.CAT_PRODUCTIVITY,
        "com.miro.mobile"                            to Category.CAT_PRODUCTIVITY,
        "com.figma.android"                          to Category.CAT_PRODUCTIVITY,
        "com.google.android.apps.photos"             to Category.CAT_PRODUCTIVITY,
        "com.github.android"                         to Category.CAT_PRODUCTIVITY,
        "com.gitlab.android"                         to Category.CAT_PRODUCTIVITY,
        "com.codeium.android"                        to Category.CAT_PRODUCTIVITY,
        "com.replit.android"                         to Category.CAT_PRODUCTIVITY,

        // ── NEWS ─────────────────────────────────────────────────────────────
        "com.google.android.apps.news"               to Category.CAT_NEWS,
        "com.flipboard.app"                          to Category.CAT_NEWS,
        "com.feedly.android"                         to Category.CAT_NEWS,
        "com.medium.android"                         to Category.CAT_NEWS,
        "com.substack.app"                           to Category.CAT_NEWS,
        "com.bbc.news"                               to Category.CAT_NEWS,
        "com.cnn.mobile.android.phone"               to Category.CAT_NEWS,
        "com.foxnews.android"                        to Category.CAT_NEWS,
        "com.nytimes.android"                        to Category.CAT_NEWS,
        "com.washingtonpost.android"                 to Category.CAT_NEWS,
        "com.theguardian.android"                    to Category.CAT_NEWS,
        "com.reuters.news"                           to Category.CAT_NEWS,
        "com.bloomberg.android.bloomberg"            to Category.CAT_NEWS,
        "com.wsj.android"                            to Category.CAT_NEWS,
        "com.economist.android"                      to Category.CAT_NEWS,
        "com.appliedzen.hackernews"                  to Category.CAT_NEWS,
        "com.ap.news"                                to Category.CAT_NEWS,
        "com.axios.android"                          to Category.CAT_NEWS,
        "com.theatlantic.android"                    to Category.CAT_NEWS,
        "com.time.android"                           to Category.CAT_NEWS,
        "com.buzzfeed.android"                       to Category.CAT_NEWS,
        "com.vox.android"                            to Category.CAT_NEWS,
        "com.aljazeera.english"                      to Category.CAT_NEWS,
        "com.dw.learngerman.android"                 to Category.CAT_NEWS,
        // TR haber
        "com.ntv.ntvandroid"                         to Category.CAT_NEWS,
        "com.sabah.android"                          to Category.CAT_NEWS,
        "com.haberturk.android"                      to Category.CAT_NEWS,
        "com.hurriyet.android"                       to Category.CAT_NEWS,
        "com.milliyet.android"                       to Category.CAT_NEWS,
        "com.sozcu.android"                          to Category.CAT_NEWS,
        "com.aa.android"                             to Category.CAT_NEWS,
        "com.dha.android"                            to Category.CAT_NEWS,
        "com.haber.son.android"                      to Category.CAT_NEWS,

        // ── UTILITIES ────────────────────────────────────────────────────────
        "com.android.chrome"                         to Category.CAT_UTILITIES,
        "org.mozilla.firefox"                        to Category.CAT_UTILITIES,
        "com.opera.browser"                          to Category.CAT_UTILITIES,
        "com.opera.mini.native"                      to Category.CAT_UTILITIES,
        "com.microsoft.emmx"                         to Category.CAT_UTILITIES,
        "com.brave.browser"                          to Category.CAT_UTILITIES,
        "com.duckduckgo.mobile.android"              to Category.CAT_UTILITIES,
        "com.sec.android.app.sbrowser"               to Category.CAT_UTILITIES,
        "com.vivaldi.browser"                        to Category.CAT_UTILITIES,
        "org.torproject.torbrowser"                  to Category.CAT_UTILITIES,
        "com.expressvpn.vpn"                         to Category.CAT_UTILITIES,
        "com.nordvpn.android"                        to Category.CAT_UTILITIES,
        "com.surfshark.vpnclient.android"            to Category.CAT_UTILITIES,
        "com.privateinternetaccess.android"          to Category.CAT_UTILITIES,
        "com.proton.android.vpn"                     to Category.CAT_UTILITIES,
        "com.proton.android"                         to Category.CAT_UTILITIES,
        "com.google.android.inputmethod.latin"       to Category.CAT_UTILITIES,
        "com.swiftkey.languagepack.service"          to Category.CAT_UTILITIES,
        "com.touchtype.swiftkey"                     to Category.CAT_UTILITIES,
        "com.gboard"                                 to Category.CAT_UTILITIES,
        "com.nuance.swype.android"                   to Category.CAT_UTILITIES,
        "com.cootek.smartinputv5"                    to Category.CAT_UTILITIES,
        "com.google.android.apps.authenticator2"     to Category.CAT_UTILITIES,
        "com.authy.authy"                            to Category.CAT_UTILITIES,
        "com.microsoft.authenticator"                to Category.CAT_UTILITIES,
        "com.qrcodereader.android"                   to Category.CAT_UTILITIES,
        "la.droid.qr"                                to Category.CAT_UTILITIES,
        "com.google.android.apps.translate"          to Category.CAT_UTILITIES,
        "com.deepl.android"                          to Category.CAT_UTILITIES,
        "com.microsoft.translator"                   to Category.CAT_UTILITIES,
        "com.whatsapp.transfer"                      to Category.CAT_UTILITIES,
        "com.cleanmaster.mguard"                     to Category.CAT_UTILITIES,
        "com.avast.android.mobilesecurity"           to Category.CAT_UTILITIES,
        "com.bitdefender.security"                   to Category.CAT_UTILITIES,
        "com.kaspersky.kes.android"                  to Category.CAT_UTILITIES,
        "com.malwarebytes.antimalware"               to Category.CAT_UTILITIES,
        "com.google.android.calculator"              to Category.CAT_UTILITIES,
        "com.google.android.apps.cloudprint"         to Category.CAT_UTILITIES,
        "com.x.files"                                to Category.CAT_UTILITIES,
        "com.ghisler.android.TotalCommander"         to Category.CAT_UTILITIES,
        "com.speedtest.by.ookla"                     to Category.CAT_UTILITIES,
        "com.nperf.internet.speed.test"              to Category.CAT_UTILITIES,
        "com.flashlight.android"                     to Category.CAT_UTILITIES,
        "com.google.android.apps.walletnfcrel"       to Category.CAT_UTILITIES,
        "com.samsung.android.aircommandmanager"      to Category.CAT_UTILITIES,
        "com.oneplus.store"                          to Category.CAT_UTILITIES,
        "com.uc.browser.en"                          to Category.CAT_UTILITIES,

        // ── FOOD ─────────────────────────────────────────────────────────────
        "com.doordash.ddshopper"                     to Category.CAT_FOOD,
        "com.ubereats"                               to Category.CAT_FOOD,
        "com.grubhub.android"                        to Category.CAT_FOOD,
        "com.deliveryhero.vendor"                    to Category.CAT_FOOD,
        "com.just.eat.android"                       to Category.CAT_FOOD,
        "com.global.deliveroo.orderapp"              to Category.CAT_FOOD,
        "com.instacart.groceries"                    to Category.CAT_FOOD,
        "com.starbucks.mobilecard"                   to Category.CAT_FOOD,
        "com.mcdonalds.app"                          to Category.CAT_FOOD,
        "com.burgerking.android"                     to Category.CAT_FOOD,
        "com.dominos.android"                        to Category.CAT_FOOD,
        "com.pizzahut.android"                       to Category.CAT_FOOD,
        "com.yelp.android"                           to Category.CAT_FOOD,
        "com.zomato.android"                         to Category.CAT_FOOD,
        "com.whisk.android"                          to Category.CAT_FOOD,
        "com.yummly.android"                         to Category.CAT_FOOD,
        "com.allrecipes.android"                     to Category.CAT_FOOD,
        "com.tasty.android"                          to Category.CAT_FOOD,
        "com.bigoven.android"                        to Category.CAT_FOOD,
        "com.mealime.app"                            to Category.CAT_FOOD,
        "com.sideproject.foodpanda"                  to Category.CAT_FOOD,
        "com.grab.rider"                             to Category.CAT_FOOD,
        "com.gojek.app"                              to Category.CAT_FOOD,
        "com.swiggy.android"                         to Category.CAT_FOOD,
        "com.rappi.android"                          to Category.CAT_FOOD,
        "br.com.ifood"                               to Category.CAT_FOOD,
        // TR yemek
        "com.yemeksepeti.android"                    to Category.CAT_FOOD,
        "com.getir.mobile"                           to Category.CAT_FOOD,
        "com.trendyolexpress.android"                to Category.CAT_FOOD,
        "com.migros.sanal.market"                    to Category.CAT_FOOD,
        "com.bim.android"                            to Category.CAT_FOOD,
        "com.a101.android"                           to Category.CAT_FOOD,
        "com.carrefoursa.android"                    to Category.CAT_FOOD,

        // ── PHOTOGRAPHY ──────────────────────────────────────────────────────
        "com.google.android.apps.photos"             to Category.CAT_PHOTOGRAPHY,
        "com.samsung.android.gallery3d"              to Category.CAT_PHOTOGRAPHY,
        "com.adobe.lightroom"                        to Category.CAT_PHOTOGRAPHY,
        "com.adobe.photoshop"                        to Category.CAT_PHOTOGRAPHY,
        "com.adobe.premiere.rush"                    to Category.CAT_PHOTOGRAPHY,
        "com.adobe.spark.post"                       to Category.CAT_PHOTOGRAPHY,
        "com.canva.android"                          to Category.CAT_PHOTOGRAPHY,
        "com.picsart.studio"                         to Category.CAT_PHOTOGRAPHY,
        "com.vsco.cam"                               to Category.CAT_PHOTOGRAPHY,
        "com.lightx.photo.filters"                   to Category.CAT_PHOTOGRAPHY,
        "com.snapseed"                               to Category.CAT_PHOTOGRAPHY,
        "com.google.android.apps.photoeditor.paper"  to Category.CAT_PHOTOGRAPHY,
        "com.inshot.android"                         to Category.CAT_PHOTOGRAPHY,
        "com.viamaker.videoeditormobile"             to Category.CAT_PHOTOGRAPHY,
        "com.capcut.video.editor"                    to Category.CAT_PHOTOGRAPHY,
        "com.wondershare.videoedit"                  to Category.CAT_PHOTOGRAPHY,
        "com.kwai.video"                             to Category.CAT_PHOTOGRAPHY,
        "com.youcam.beauty"                          to Category.CAT_PHOTOGRAPHY,
        "com.faceapp"                                to Category.CAT_PHOTOGRAPHY,
        "com.retouch4me.android"                     to Category.CAT_PHOTOGRAPHY,
        "com.jw.android.camera"                      to Category.CAT_PHOTOGRAPHY,
        "com.gcam.android"                           to Category.CAT_PHOTOGRAPHY,
        "com.polarr.photo.editor"                    to Category.CAT_PHOTOGRAPHY,
        "com.fotor.android"                          to Category.CAT_PHOTOGRAPHY,
        "com.pixlr.android"                          to Category.CAT_PHOTOGRAPHY,
        "com.lens.blur.android"                      to Category.CAT_PHOTOGRAPHY,
        "com.unfold.collage.maker"                   to Category.CAT_PHOTOGRAPHY,
        "com.flipagram"                              to Category.CAT_PHOTOGRAPHY,
        "com.tinyplanet.android"                     to Category.CAT_PHOTOGRAPHY,
        "com.nightmode.android"                      to Category.CAT_PHOTOGRAPHY,
        "com.beautycamera.selfie.android"            to Category.CAT_PHOTOGRAPHY,
        "com.picmonkey.android"                      to Category.CAT_PHOTOGRAPHY,
    )

    /**
     * Classify a single app into a category
     */
    fun classifyApp(appInfo: AppInfo): String {
        // 1. Online veritabanı (Play Store kategorileri) — en yüksek öncelik
        appDatabaseService.getCategoryForPackage(appInfo.packageName)?.let { return it }
        // 2. Yerel exact match tablosu
        exactMatchMap[appInfo.packageName]?.let { return it }
        // 3. Keyword eşleşmesi
        return classifyByKeywords(appInfo.appName, appInfo.packageName) ?: Category.CAT_OTHER
    }
    
    /**
     * Classify multiple apps at once
     */
    fun classifyApps(apps: List<AppInfo>): Map<String, String> {
        return apps.associateBy(
            { it.packageName },
            { classifyApp(it) }
        )
    }
    
    /**
     * Get classification confidence (0-100)
     * 100 = high confidence, 0 = low confidence
     */
    fun getConfidence(appInfo: AppInfo, categoryId: String): Int {
        return when {
            categoryId == Category.CAT_OTHER -> 30
            hasExactMatch(appInfo.packageName, categoryId) -> 95
            hasKeywordMatch(appInfo.appName, categoryId) -> 80
            hasPackageKeywordMatch(appInfo.packageName, categoryId) -> 70
            else -> 50
        }
    }
    
    /**
     * Main classification method using keywords
     */
    private fun classifyByKeywords(appName: String, packageName: String): String? {
        val lowerAppName = appName.lowercase()
        val lowerPackage = packageName.lowercase()
        
        // Check against keyword database
        val database = KeywordDatabase.getKeywordMap()
        
        database.forEach { (category, keywords) ->
            keywords.forEach { keyword ->
                if (lowerAppName.contains(keyword) || lowerPackage.contains(keyword)) {
                    return category
                }
            }
        }
        
        return null // Will default to CAT_OTHER
    }
    
    /**
     * Check if there's an exact match in package name
     */
    private fun hasExactMatch(packageName: String, categoryId: String): Boolean {
        return exactMatchMap[packageName] == categoryId
    }
    
    /**
     * Check if app name contains category keywords
     */
    private fun hasKeywordMatch(appName: String, categoryId: String): Boolean {
        return KeywordDatabase.getKeywordMap()[categoryId]?.any { keyword ->
            appName.lowercase().contains(keyword)
        } ?: false
    }
    
    /**
     * Check if package name contains category keywords
     */
    private fun hasPackageKeywordMatch(packageName: String, categoryId: String): Boolean {
        return KeywordDatabase.getKeywordMap()[categoryId]?.any { keyword ->
            packageName.lowercase().contains(keyword)
        } ?: false
    }
}

/**
 * Keyword database for app classification.
 * Maps category IDs to lists of keywords that identify that category.
 */
object KeywordDatabase {
    
    private val keywordMap = mapOf(
        Category.CAT_SOCIAL to listOf(
            "social", "facebook", "twitter", "instagram", "whatsapp", "telegram",
            "tiktok", "snapchat", "discord", "messenger", "viber", "linkedin",
            "reddit", "quora", "mastodon", "bluesky", "threads", "tumblr",
            "wechat", "line", "kakaotalk", "signal"
        ),

        Category.CAT_PRODUCTIVITY to listOf(
            "productivity", "office", "calendar", "notes", "todo", "task",
            "mail", "email", "drive", "cloud", "storage", "document",
            "sheet", "excel", "word", "presentation", "notion", "obsidian",
            "evernote", "onenote", "todoist", "asana", "trello", "slack",
            "teams", "zoom", "meet", "google", "microsoft", "amazon",
            // AI asistanlar
            "chatgpt", "openai", "deepseek", "claude", "gemini", "copilot",
            "perplexity", "bard", "gpt", "llm", "assistant", "ai",
            "character.ai", "inflection", "mistral", "groq"
        ),
        
        Category.CAT_GAMES to listOf(
            "game", "games", "gaming", "play", "battle", "royal", "chess",
            "candy", "clash", "strike", "legends", "mobile", "puzzle",
            "racing", "shooting", "action", "adventure", "rpg", "mmo",
            "fortnite", "minecraft", "roblox", "steam", "epic"
        ),
        
        Category.CAT_SHOPPING to listOf(
            "shop", "shopping", "store", "market", "buy", "sell", "cart",
            "payment", "checkout", "price", "discount", "amazon", "ebay",
            "aliexpress", "trendyol", "hepsiburada", "n11", "sahibinden",
            "letgo", "mercari", "walmart", "target", "costco", "alibaba"
        ),
        
        Category.CAT_NEWS to listOf(
            "news", "newspaper", "article", "press", "tribune", "gazette",
            "daily", "breaking", "headline", "media", "journalist", "reader",
            "rss", "feed", "bbc", "cnn", "reuters", "bloomberg", "anadolu",
            "dha", "ntvmsnbc", "habertürk", "milliyet", "hürriyet"
        ),
        
        Category.CAT_HEALTH to listOf(
            "health", "fitness", "workout", "gym", "exercise", "sport",
            "medical", "doctor", "hospital", "clinic", "medicine", "pharma",
            "wellness", "yoga", "diet", "nutrition", "calorie", "step",
            "heart", "run", "walk", "cycle", "bike", "swim"
        ),
        
        Category.CAT_FINANCE to listOf(
            "finance", "bank", "payment", "money", "invest", "stock",
            "crypto", "bitcoin", "wallet", "card", "credit", "loan",
            "tax", "accounting", "trading", "forex", "commodity", "bitcoin",
            "ethereum", "ripple", "trading", "thinkorswim"
        ),
        
        Category.CAT_EDUCATION to listOf(
            "education", "learn", "course", "class", "school", "university",
            "exam", "test", "quiz", "study", "lesson", "tutorial",
            "udemy", "coursera", "skillshare", "duolingo", "babbel",
            "memrise", "brilliant", "codecademy", "edx", "khan"
        ),
        
        Category.CAT_UTILITIES to listOf(
            "utility", "tools", "tool", "manager", "cleaner", "antivirus",
            "security", "lock", "safe", "backup", "restore", "file",
            "explorer", "download", "torrent", "vpn", "proxy", "browser",
            "keyboard", "launcher", "theme", "widget", "widget"
        ),

        Category.CAT_TRAVEL to listOf(
            "travel", "trip", "flight", "hotel", "booking", "airbnb", "hostel",
            "vacation", "holiday", "tour", "map", "navigation", "gps", "route",
            "taxi", "uber", "lyft", "bus", "train", "subway", "airline",
            "airport", "passport", "visa", "seyahat", "uçuş", "otel",
            "flightradar", "tripadvisor", "expedia", "skyscanner", "kayak"
        ),

        Category.CAT_ENTERTAINMENT to listOf(
            "entertainment", "movie", "film", "video", "stream", "watch",
            "netflix", "youtube", "twitch", "disney", "hulu", "prime",
            "music", "spotify", "podcast", "radio", "audio", "sound",
            "tv", "series", "show", "cinema", "theater", "concert",
            "tiktok", "reels", "shorts", "anime", "manga", "webtoon",
            "eğlence", "dizi", "film", "müzik"
        ),

        Category.CAT_OTHER to listOf()
    )
    
    fun getKeywordMap(): Map<String, List<String>> = keywordMap
    
    /**
     * Get keywords for a specific category
     */
    fun getKeywords(categoryId: String): List<String> {
        return keywordMap[categoryId] ?: emptyList()
    }
    
    /**
     * Get total keyword count
     */
    fun getTotalKeywords(): Int {
        return keywordMap.values.sumOf { it.size }
    }
    
    /**
     * Add custom keyword to a category
     */
    fun addKeywordToCategory(categoryId: String, keyword: String) {
        val currentKeywords = keywordMap[categoryId] ?: emptyList()
        if (!currentKeywords.contains(keyword)) {
            (keywordMap as MutableMap)[categoryId] = currentKeywords + keyword
        }
    }
}
