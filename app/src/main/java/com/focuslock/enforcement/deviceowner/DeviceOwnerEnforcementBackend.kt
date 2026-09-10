package com.focuslock.enforcement.deviceowner

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import com.focuslock.enforcement.EnforcementBackend
import com.focuslock.enforcement.EnforcementBackendType
import com.focuslock.enforcement.EnforcementResult
import com.focuslock.enforcement.Suspendability
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hard-lock backend: suspends packages at the OS level via
 * [DevicePolicyManager.setPackagesSuspended]. Suspended-package state survives
 * FocusLock process death.
 */
@Singleton
class DeviceOwnerEnforcementBackend @Inject constructor(
    @ApplicationContext private val context: Context,
) : EnforcementBackend {

    private val dpm = context.getSystemService(DevicePolicyManager::class.java)
    private val adminComponent = ComponentName(context, FocusLockDeviceAdminReceiver::class.java)

    override suspend fun suspendPackages(packages: Set<String>): EnforcementResult =
        withContext(Dispatchers.IO) {
            if (!isDeviceOwner()) {
                return@withContext EnforcementResult(successful = emptySet(), failed = packages)
            }
            val array = packages.toTypedArray()
            if (array.isEmpty()) return@withContext EnforcementResult(successful = emptySet(), failed = emptySet())

            val applied = runCatching {
                dpm.setPackagesSuspended(adminComponent, array, true)
            }.isSuccess

            if (!applied) {
                return@withContext EnforcementResult(successful = emptySet(), failed = packages)
            }

            val successful = packages.filter { dpm.isPackageSuspended(adminComponent, it) }.toSet()
            val failed = packages - successful
            EnforcementResult(successful = successful, failed = failed)
        }

    override suspend fun resumePackages(packages: Set<String>): EnforcementResult =
        withContext(Dispatchers.IO) {
            if (!isDeviceOwner()) {
                return@withContext EnforcementResult(successful = emptySet(), failed = packages)
            }
            val array = packages.toTypedArray()
            if (array.isEmpty()) return@withContext EnforcementResult(successful = emptySet(), failed = emptySet())

            runCatching {
                dpm.setPackagesSuspended(adminComponent, array, false)
            }
            val stillSuspended = packages.filter { dpm.isPackageSuspended(adminComponent, it) }.toSet()
            EnforcementResult(successful = packages - stillSuspended, failed = stillSuspended)
        }

    override fun suspendability(packageName: String): Suspendability =
        if (isDeviceOwner()) Suspendability.SUSPENDABLE else Suspendability.NOT_SUSPENDABLE

    override fun isAvailable(): Boolean = isDeviceOwner()

    override fun backendType(): EnforcementBackendType = EnforcementBackendType.DEVICE_OWNER

    fun isDeviceOwner(): Boolean = dpm?.isDeviceOwnerApp(context.packageName) == true

    fun isAdminActive(): Boolean = dpm?.isAdminActive(adminComponent) == true

    /** Which of [packages] are actually suspended right now. */
    fun actuallySuspended(packages: Set<String>): Set<String> =
        if (!isDeviceOwner()) emptySet()
        else packages.filter { dpm.isPackageSuspended(adminComponent, it) }.toSet()
}
