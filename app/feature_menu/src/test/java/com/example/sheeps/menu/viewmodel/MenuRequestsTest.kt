package com.example.sheeps.menu.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.sheeps.core.cache.ShopCache
import com.example.sheeps.core.game.TileIconProvider
import com.example.sheeps.data.model.ShopItem
import com.example.sheeps.data.local.UserProfileEntity
import com.example.sheeps.data.local.LocalDao
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.data.preference.UserPreferences
import com.example.sheeps.data.repository.UserRepository
import com.example.sheeps.data.result.TokenRefresher
import com.example.sheeps.menu.state.PersonalSection
import com.example.sheeps.menu.state.PersonalLoadState
import com.example.sheeps.data.repository.SyncRepository
import com.example.sheeps.data.utils.NetworkMonitor
import com.example.sheeps.menu.state.MenuViewIntent
import mockwebserver3.MockWebServer
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import mockwebserver3.Dispatcher
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.Executors

@OptIn(ExperimentalCoroutinesApi::class)
class MenuRequestsTest {
    @Test fun coldResumeAndExplicitRefreshEachIssueOneRequestPerResource() = runBlocking {
        val paths = CopyOnWriteArrayList<String>()
        val server = MockWebServer()
        val locale = AtomicReference("zh")
        val account = AtomicReference<String?>("first")
        val personalPaths = setOf("task/daily", "user/points-history", "user/exchange-history")
        val holdExchange = java.util.concurrent.atomic.AtomicBoolean(false)
        val exchangeStarted = CountDownLatch(1)
        val releaseExchange = CountDownLatch(1)
        val refreshStatus = java.util.concurrent.atomic.AtomicInteger(200)
        val historyError = java.util.concurrent.atomic.AtomicBoolean(false)
        val delayedLocale = AtomicReference<String?>(null)
        val delayedNotice = CountDownLatch(1)
        val releaseNotice = CountDownLatch(1)
        val shopStarted = CountDownLatch(1)
        val releaseShop = CountDownLatch(1)
        val shopStatus = java.util.concurrent.atomic.AtomicInteger(200)
        val profile = """{"success":true,"user":{"id":"OWNER","username":"test","points":0},
            "unlocked_levels":[],"items":[],"today_signed":false,"sign_streak":0,"highest_level_cleared":0}"""
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.url.encodedPath.substringAfter("/api/")
                paths += path
                val access = request.headers["Authorization"].orEmpty().removePrefix("Bearer ")
                if ((path in personalPaths || path == "user/profile") && access == "second-expired") return MockResponse.Builder().code(401).build()
                if (path == "auth/refresh" && refreshStatus.get() != 200) return MockResponse.Builder().code(refreshStatus.get()).build()
                if (path == "user/exchange-history" && holdExchange.get()) {
                    exchangeStarted.countDown()
                    check(releaseExchange.await(5, TimeUnit.SECONDS))
                }
                if (path == "user/exchange-history" && historyError.get()) return MockResponse.Builder().code(503).build()
                val text = when (path) {
                    "auth/refresh" -> """{"success":true,"token":"second-renewed","refreshToken":"refresh-next"}"""
                    "user/profile" -> profile.replace("OWNER", access.substringBefore('-'))
                    "notice/list" -> {
                        val lang = request.headers["Accept-Language"]
                        if (lang == delayedLocale.get()) {
                            delayedNotice.countDown()
                            check(releaseNotice.await(10, TimeUnit.SECONDS))
                        }
                        """[{"id":1,"title":"$lang","content":"","type":"ACTIVITY","created_at":0}]"""
                    }
                    "user/exchange-history" -> {
                        val owner = request.headers["Authorization"].orEmpty().removePrefix("Bearer ")
                        """[{"shop_item_id":1,"item_type":"$owner","count":1,"points_cost":5,"created_at":0}]"""
                    }
                    "shop/items" -> {
                        shopStarted.countDown()
                        check(releaseShop.await(10, TimeUnit.SECONDS))
                        if (shopStatus.get() != 200) return MockResponse.Builder().code(shopStatus.get()).build()
                        "[]"
                    }
                    "user/sync" -> """{"success":true,"user":{"id":"first","username":"test","points":3},"unlocked_levels":[],"items":[]}"""
                    "app/check-update" -> "{}"
                    else -> "[]"
                }
                return MockResponse.Builder().body(text).build()
            }
        }
        server.start()
        val main = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        Dispatchers.setMain(main)
        val ctx = mockk<Context>(relaxed = true)
        val dao = mockk<LocalDao>(relaxed = true)
        val prefs = mockk<UserPreferences>(relaxed = true)
        val shop = mockk<ShopCache>(relaxed = true)
        val monitor = mockk<NetworkMonitor>(relaxed = true)
        every { prefs.isLoggedIn() } answers { account.get() != null }
        every { prefs.getToken() } answers { account.get() }
        every { prefs.accountId(any()) } answers { firstArg<String?>()?.substringBefore('-') }
        every { prefs.getRefreshToken() } returns "refresh-original"
        every { prefs.replaceTokens(any(), any(), any(), any()) } answers { account.compareAndSet(firstArg(), thirdArg()) }
        every { prefs.getLanguage() } answers { locale.get() }
        every { prefs.setLanguage(any()) } answers { locale.set(firstArg()) }
        every { prefs.getCurrentSkin() } returns "test"
        every { dao.observeProfile() } returns emptyFlow()
        every { dao.observeAllItems() } returns emptyFlow()
        every { dao.observeAllProgress() } returns emptyFlow()
        coEvery { dao.hasProfile(any()) } returns true
        coEvery { dao.getDirtyProgress() } returns emptyList()
        coEvery { dao.getDirtyProfiles() } returns emptyList()
        coEvery { dao.getDirtyItems() } returns emptyList()
        coEvery { dao.pendingConsumptions(any()) } returns emptyList()
        every { shop.getCachedItems() } returns emptyList()
        every { monitor.status } returns kotlinx.coroutines.flow.MutableStateFlow(com.example.sheeps.data.utils.NetworkStatus.ONLINE)
        every { monitor.isOnline() } returns true
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("Accept-Language", locale.get()).build())
        }.build()
        val api = Retrofit.Builder().client(http).baseUrl(server.url("/"))
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build().create(ApiService::class.java)
        val tokens = TokenRefresher(api, prefs)
        val sync = SyncRepository(api, dao, prefs, monitor, tokens, mockk(relaxed = true))
        val users = UserRepository(api, prefs, dao, sync, tokens)
        var vm: MenuViewModel? = null
        try {
            val menu = withContext(main) {
                MenuViewModel(ctx, api, prefs, shop, dao, sync, users, monitor,
                    mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
            }
            vm = menu
            suspend fun refresh(intent: MenuViewIntent): List<Job> = withContext(main) {
                val root = menu.viewModelScope.coroutineContext.job
                val existing = root.children.toSet()
                menu.sendIntent(intent)
                menu.refreshGameModes()
                yield()
                root.children.filterNot { it in existing }.toList()
            }

            withTimeout(15000) {
                val cold = refresh(MenuViewIntent.LoadData)
                check(shopStarted.await(5, TimeUnit.SECONDS))
                assertEquals(false, menu.viewState.value.isLoading)
                assertEquals(true, menu.viewState.value.isShopLoading)
                releaseShop.countDown()
                cold.joinAll()
                assertEquals(false, menu.viewState.value.isShopLoading)
                while ("app/check-update" !in paths) delay(10)
                val expected = listOf("shop/items", "notice/list", "user/profile", "task/daily",
                    "user/points-history", "user/exchange-history", "app/check-update").associateWith { 1 }
                assertEquals(expected, paths.groupingBy { it }.eachCount())
                assertEquals("first", menu.viewState.value.exchangeHistory.single().item_type)

                coEvery { dao.getDirtyProfiles() } returnsMany listOf(
                    listOf(UserProfileEntity("first", "test", 3, true, 1L)), emptyList()
                )
                val beforeSync = paths.size
                refresh(MenuViewIntent.LoadData).joinAll()
                val privateRequests = paths.drop(beforeSync).filter { it == "user/sync" || it == "user/profile" }
                assertEquals(listOf("user/sync", "user/profile"), privateRequests)

                // Hold an old localized response while a different account and language start a new refresh.
                delayedLocale.set("zh")
                val oldRefresh = refresh(MenuViewIntent.LoadData)
                check(delayedNotice.await(5, TimeUnit.SECONDS))
                assertEquals(false, menu.viewState.value.isLoading)
                account.set("second")
                val newRefresh = refresh(MenuViewIntent.ChangeLanguage("en"))
                newRefresh.joinAll()
                releaseNotice.countDown()
                oldRefresh.joinAll()
                assertEquals("en", menu.viewState.value.notices.single().title)
                assertEquals("second", menu.viewState.value.exchangeHistory.single().item_type)
                assertEquals(4, paths.count { it == "shop/items" })

                account.set("second-expired")
                val beforeExpiry = paths.size
                refresh(MenuViewIntent.LoadData).joinAll()
                val renewal = paths.drop(beforeExpiry)
                assertEquals("renew a shared expired token once", 1, renewal.count { it == "auth/refresh" })
                personalPaths.forEach { assertEquals("retry $it once", 2, renewal.count { path -> path == it }) }
                assertEquals("second-renewed", menu.viewState.value.exchangeHistory.single().item_type)

                assertEquals(2, renewal.count { it == "user/profile" })
                holdExchange.set(true)
                historyError.set(true)
                val pendingHistory = refresh(MenuViewIntent.RetryPersonalSection(PersonalSection.Exchanges))
                check(exchangeStarted.await(5, TimeUnit.SECONDS))
                assertEquals(PersonalLoadState.Loading, menu.viewState.value.personalStatus[PersonalSection.Exchanges])
                releaseExchange.countDown()
                pendingHistory.joinAll()
                assertEquals(PersonalLoadState.Failed, menu.viewState.value.personalStatus[PersonalSection.Exchanges])
                assertEquals("second-renewed", menu.viewState.value.exchangeHistory.single().item_type)
                historyError.set(false)
                refresh(MenuViewIntent.RetryPersonalSection(PersonalSection.Exchanges)).joinAll()
                assertEquals(PersonalLoadState.Ready, menu.viewState.value.personalStatus[PersonalSection.Exchanges])
                assertEquals(PersonalLoadState.Ready, menu.viewState.value.personalStatus[PersonalSection.Points])
                assertEquals(emptyList<Any>(), menu.viewState.value.pointsHistory)

                account.set("second-expired")
                refreshStatus.set(401)
                val beforeReauth = paths.size
                refresh(MenuViewIntent.LoadData).joinAll()
                assertEquals(1, paths.drop(beforeReauth).count { it == "auth/refresh" })
                PersonalSection.entries.forEach { section ->
                    assertEquals(PersonalLoadState.SignInRequired, menu.viewState.value.personalStatus[section])
                }

                val beforeGuest = paths.size
                account.set(null)
                shopStatus.set(503)
                refresh(MenuViewIntent.LoadData).joinAll()
                assertEquals(false, menu.viewState.value.isLoggedIn)
                assertEquals(false, menu.viewState.value.isShopLoading)
                assertEquals(emptyList<Any>(), menu.viewState.value.exchangeHistory)
                assertEquals(listOf("notice/list", "shop/items"), paths.drop(beforeGuest).sorted())

                // A second ViewModel after process startup can render stored URLs without any network response.
                val cached = ShopItem(7, "Undo", image_url = "https://example.invalid/cached.png",
                    item_type = "UNDO", points_price = 5, stock = 1)
                every { shop.getCachedItems() } returns listOf(cached)
                every { monitor.isOnline() } returns false
                withContext(main) {
                    val fromCache = MenuViewModel(ctx, api, prefs, shop, dao, sync, users, monitor,
                        mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
                    try {
                        assertEquals(listOf(cached), fromCache.viewState.value.shopItems)
                        assertEquals(cached.image_url, TileIconProvider.getItemIconUrl("UNDO"))
                    } finally {
                        fromCache.viewModelScope.cancel()
                        TileIconProvider.setShopItems(emptyList())
                    }
                }
            }
        } finally {
            releaseShop.countDown()
            releaseExchange.countDown()
            releaseNotice.countDown()
            vm?.viewModelScope?.cancel()
            server.close()
            Dispatchers.resetMain()
            main.close()
        }
    }
}
