package com.selflock.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.selflock.app.BlockOverlayActivity
import com.selflock.app.data.repository.AppRuleRepository
import com.selflock.app.data.repository.WebsiteRuleRepository
import com.selflock.app.domain.usecase.CheckBlockStatusUseCase
import com.selflock.app.util.BrowserUrlParser
import com.selflock.app.vpn.BlocklistManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SelfLockAccessibilityService : AccessibilityService() {

    @Inject lateinit var appRuleRepository: AppRuleRepository
    @Inject lateinit var websiteRuleRepository: WebsiteRuleRepository
    @Inject lateinit var checkBlockStatusUseCase: CheckBlockStatusUseCase
    @Inject lateinit var blocklistManager: BlocklistManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentForegroundPackage: String? = null
    private var lastWebsiteBlockTime: Long = 0L

    companion object {
        private const val WEBSITE_BLOCK_COOLDOWN_MS = 3000L

        var instance: SelfLockAccessibilityService? = null
            private set

        val browserUrlBarIds = mapOf(
            "com.android.chrome" to listOf("com.android.chrome:id/url_bar", "com.android.chrome:id/search_box_text"),
            "org.mozilla.firefox" to listOf("org.mozilla.firefox:id/url_bar_title", "org.mozilla.fenix:id/mozac_browser_toolbar_url_view"),
            "com.sec.android.app.sbrowser" to listOf("com.sec.android.app.sbrowser:id/location_bar_edit_text"),
            "com.microsoft.emmx" to listOf("com.microsoft.emmx:id/url_bar")
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString() ?: return
                if (packageName == "com.selflock.app") return
                currentForegroundPackage = packageName
                checkAndBlockApp(packageName)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val packageName = event.packageName?.toString() ?: return
                if (packageName in browserUrlBarIds) {
                    extractBrowserUrl(event, packageName)
                }
            }
        }
    }

    private fun checkAndBlockApp(packageName: String) {
        scope.launch {
            val rule = appRuleRepository.getRuleByPackageName(packageName) ?: return@launch
            val status = checkBlockStatusUseCase.checkAppRule(rule)
            if (status.isActive) {
                launchBlockOverlay(packageName, rule.appName, status)
            }
        }
    }

    private fun launchBlockOverlay(packageName: String?, appName: String, status: com.selflock.app.domain.model.RuleStatus, domain: String? = null) {
        val intent = Intent(this, BlockOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(BlockOverlayActivity.EXTRA_APP_NAME, appName)
            putExtra(BlockOverlayActivity.EXTRA_REMAINING_MINUTES, status.remainingTimeMinutes ?: 0L)
            if (packageName != null) {
                putExtra(BlockOverlayActivity.EXTRA_PACKAGE_NAME, packageName)
            }
            if (domain != null) {
                putExtra(BlockOverlayActivity.EXTRA_DOMAIN, domain)
            }
        }
        startActivity(intent)
    }

    private fun extractBrowserUrl(event: AccessibilityEvent, packageName: String) {
        val urlBarIds = browserUrlBarIds[packageName] ?: return
        val rootNode = rootInActiveWindow ?: return

        for (viewId in urlBarIds) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
            if (nodes != null && nodes.isNotEmpty()) {
                val node = nodes[0]
                if (node.isFocused || node.isSelected) return
                val text = node.text?.toString()
                if (!text.isNullOrEmpty() && looksLikeNavigatedUrl(text)) {
                    MonitoringService.onBrowserUrlDetected(text, packageName)
                    checkAndBlockWebsite(text, packageName)
                    break
                }
            }
        }
    }

    private fun looksLikeNavigatedUrl(text: String): Boolean {
        if (text.contains(" ")) return false
        if (text.startsWith("http://") || text.startsWith("https://")) return true
        if (text.contains(".") && !text.startsWith(".")) {
            val parts = text.split("/")
            val hostPart = parts[0]
            return hostPart.contains(".") && hostPart.substringAfterLast(".").length >= 2
        }
        return false
    }

    private fun checkAndBlockWebsite(url: String, browserPackage: String) {
        val domain = BrowserUrlParser.extractDomain(url) ?: return
        if (blocklistManager.isDomainBlocked(domain)) {
            val now = System.currentTimeMillis()
            if (now - lastWebsiteBlockTime < WEBSITE_BLOCK_COOLDOWN_MS) return
            lastWebsiteBlockTime = now

            performGlobalAction(GLOBAL_ACTION_BACK)

            scope.launch {
                val rules = websiteRuleRepository.getActiveRulesList()
                val matchedRule = rules.firstOrNull { rule ->
                    domain.equals(rule.domain, ignoreCase = true) ||
                        domain.endsWith(".${rule.domain}", ignoreCase = true)
                } ?: return@launch
                val status = checkBlockStatusUseCase.checkWebsiteRule(matchedRule)
                if (status.isActive) {
                    launchBlockOverlay(null, matchedRule.domain, status, matchedRule.domain)
                }
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
