package com.shadowcam.xposed

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Minimal LSPosed/Xposed legacy-module entrypoint.
 *
 * This exists so ShadowCam can appear in LSPosed's module list and provide
 * basic diagnostics. Hooking implementation is intentionally minimal here.
 */
class ShadowCamXposedInit : IXposedHookZygoteInit, IXposedHookLoadPackage {
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        XposedBridge.log("ShadowCam: initZygote modulePath=${startupParam.modulePath}")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Avoid doing anything heavy; just emit a breadcrumb for debugging.
        // Users can verify the module is active by checking LSPosed logs.
        XposedBridge.log("ShadowCam: loaded package=${lpparam.packageName} process=${lpparam.processName}")
    }
}

