package com.focuslock.enforcement.deviceowner

import android.app.admin.DeviceAdminReceiver

/**
 * Admin component used for Device Owner provisioning. FocusLock itself performs
 * no policy actions from this receiver; package suspension is driven by
 * [DeviceOwnerEnforcementBackend].
 */
class FocusLockDeviceAdminReceiver : DeviceAdminReceiver()
