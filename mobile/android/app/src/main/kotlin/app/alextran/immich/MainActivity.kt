package com.tanitweb.orangecloudsync

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.ext.SdkExtensions
import com.tanitweb.orangecloudsync.background.BackgroundEngineLock
import com.tanitweb.orangecloudsync.background.BackgroundWorkerApiImpl
import com.tanitweb.orangecloudsync.background.BackgroundWorkerFgHostApi
import com.tanitweb.orangecloudsync.background.BackgroundWorkerLockApi
import com.tanitweb.orangecloudsync.connectivity.ConnectivityApi
import com.tanitweb.orangecloudsync.connectivity.ConnectivityApiImpl
import com.tanitweb.orangecloudsync.core.HttpClientManager
import com.tanitweb.orangecloudsync.core.ImmichPlugin
import com.tanitweb.orangecloudsync.core.NetworkApiPlugin
import me.albemala.native_video_player.NativeVideoPlayerPlugin
import com.tanitweb.orangecloudsync.images.LocalImageApi
import com.tanitweb.orangecloudsync.images.LocalImagesImpl
import com.tanitweb.orangecloudsync.images.RemoteImageApi
import com.tanitweb.orangecloudsync.images.RemoteImagesImpl
import com.tanitweb.orangecloudsync.passkey.PasskeyPlugin
import com.tanitweb.orangecloudsync.permission.PermissionApi
import com.tanitweb.orangecloudsync.permission.PermissionApiImpl
import com.tanitweb.orangecloudsync.sync.NativeSyncApi
import com.tanitweb.orangecloudsync.sync.NativeSyncApiImpl26
import com.tanitweb.orangecloudsync.sync.NativeSyncApiImpl30
import com.tanitweb.orangecloudsync.viewintent.ViewIntentPlugin
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine

class MainActivity : FlutterFragmentActivity() {
  override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
    super.configureFlutterEngine(flutterEngine)
    registerPlugins(this, flutterEngine)
    flutterEngine.plugins.add(PasskeyPlugin())
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
  }

  companion object {
    fun registerPlugins(ctx: Context, flutterEngine: FlutterEngine) {
      HttpClientManager.initialize(ctx)
      NativeVideoPlayerPlugin.dataSourceFactory = HttpClientManager::createDataSourceFactory
      flutterEngine.plugins.add(NetworkApiPlugin())

      val messenger = flutterEngine.dartExecutor.binaryMessenger
      val backgroundEngineLockImpl = BackgroundEngineLock(ctx)
      BackgroundWorkerLockApi.setUp(messenger, backgroundEngineLockImpl)
      val nativeSyncApiImpl =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R) < 1) {
          NativeSyncApiImpl26(ctx)
        } else {
          NativeSyncApiImpl30(ctx)
        }
      val permissionApiImpl = PermissionApiImpl(ctx)
      NativeSyncApi.setUp(messenger, nativeSyncApiImpl)
      PermissionApi.setUp(messenger, permissionApiImpl)
      LocalImageApi.setUp(messenger, LocalImagesImpl(ctx))
      RemoteImageApi.setUp(messenger, RemoteImagesImpl(ctx))

      BackgroundWorkerFgHostApi.setUp(messenger, BackgroundWorkerApiImpl(ctx))
      ConnectivityApi.setUp(messenger, ConnectivityApiImpl(ctx))

      flutterEngine.plugins.add(ViewIntentPlugin())
      flutterEngine.plugins.add(backgroundEngineLockImpl)
      flutterEngine.plugins.add(nativeSyncApiImpl)
      flutterEngine.plugins.add(permissionApiImpl)
    }

    fun cancelPlugins(flutterEngine: FlutterEngine) {
      val nativeApi =
        flutterEngine.plugins.get(NativeSyncApiImpl26::class.java) as ImmichPlugin?
          ?: flutterEngine.plugins.get(NativeSyncApiImpl30::class.java) as ImmichPlugin?
      nativeApi?.detachFromEngine()
      val permissionApi = flutterEngine.plugins.get(PermissionApiImpl::class.java) as ImmichPlugin?
      permissionApi?.detachFromEngine()
    }
  }
}
