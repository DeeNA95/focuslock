package com.focuslock.ui

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.focuslock.domain.model.FocusProfile
import com.focuslock.domain.model.SessionEvent
import com.focuslock.domain.nfc.TagResolution
import com.focuslock.domain.nfc.TagResolver
import com.focuslock.domain.repository.EventLogRepository
import com.focuslock.domain.session.ActivationFailure
import com.focuslock.domain.session.SessionActivationResult
import com.focuslock.domain.session.SessionManager
import com.focuslock.domain.session.SessionReconciler
import com.focuslock.domain.time.TimeAuthority
import com.focuslock.nfc.NfcEventBus
import com.focuslock.nfc.NfcIntentParser
import com.focuslock.nfc.NfcManager
import com.focuslock.nfc.NfcTagEvent
import com.focuslock.ui.navigation.FocusLockNavHost
import com.focuslock.ui.theme.FocusLockTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var nfcManager: NfcManager
    @Inject lateinit var nfcEventBus: NfcEventBus
    @Inject lateinit var tagResolver: TagResolver
    @Inject lateinit var eventLogRepository: EventLogRepository
    @Inject lateinit var timeAuthority: TimeAuthority
    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var sessionReconciler: SessionReconciler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FocusLockTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FocusLockNavHost()
                }
            }
        }
        handleNfcIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        enableForegroundDispatch()
        lifecycleScope.launch { sessionReconciler.reconcile() }
    }

    override fun onPause() {
        super.onPause()
        nfcManager.adapter()?.disableForegroundDispatch(this)
    }

    private fun enableForegroundDispatch() {
        val adapter = nfcManager.adapter() ?: return
        if (!adapter.isEnabled) return
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE,
        )
        val filters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply { addDataType("*/*") },
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
        )
        adapter.enableForegroundDispatch(this, pendingIntent, filters, null)
    }

    private fun handleNfcIntent(intent: Intent?) {
        @Suppress("DEPRECATION")
        val tag = intent?.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
        val token = NfcIntentParser.extractToken(intent)
        nfcEventBus.onTag(NfcTagEvent(tag, token))

        if (token != null) {
            resolveAndDisplay(token)
        }
    }

    private fun resolveAndDisplay(token: String) {
        lifecycleScope.launch {
            eventLogRepository.log(
                SessionEvent(
                    timestamp = timeAuthority.now(),
                    type = SessionEvent.TYPE_NFC_TAG_DETECTED,
                    detail = token,
                )
            )
            when (val result = tagResolver.resolve(token)) {
                is TagResolution.Resolved -> {
                    eventLogRepository.log(
                        SessionEvent(
                            timestamp = timeAuthority.now(),
                            type = SessionEvent.TYPE_PROFILE_RESOLVED,
                            detail = result.profile.name,
                        )
                    )
                    activate(result.profile)
                }
                TagResolution.UnknownTag -> {
                    Toast.makeText(this@MainActivity, "Unknown tag", Toast.LENGTH_SHORT).show()
                }
                TagResolution.ProfileMissing -> {
                    Toast.makeText(this@MainActivity, "Profile missing for tag", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun activate(profile: FocusProfile) {
        when (val result = sessionManager.activate(profile)) {
            is SessionActivationResult.Activated -> {
                Toast.makeText(
                    this@MainActivity,
                    "${profile.name} active until ${formatTime(result.session.expiresAtWallClock)}",
                    Toast.LENGTH_SHORT,
                ).show()
            }
            is SessionActivationResult.Failed -> {
                Toast.makeText(
                    this@MainActivity,
                    activationFailureMessage(result.reason),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun activationFailureMessage(reason: ActivationFailure): String = when (reason) {
        ActivationFailure.SESSION_ALREADY_ACTIVE -> "A session is already active"
        ActivationFailure.OUTSIDE_ALLOWED_WINDOW -> "Outside the allowed activation window"
        ActivationFailure.PROFILE_DISABLED -> "This profile is disabled"
        ActivationFailure.ENFORCEMENT_UNAVAILABLE -> "Enforcement backend unavailable"
        ActivationFailure.UNSAFE_CONFIGURATION -> "This profile would block an unsafe app"
        ActivationFailure.ENFORCEMENT_FAILURE -> "Failed to apply blocking"
    }

    private fun formatTime(instant: java.time.Instant): String =
        java.time.format.DateTimeFormatter.ofPattern("HH:mm")
            .withZone(java.time.ZoneId.systemDefault())
            .format(instant)
}
