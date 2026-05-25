package com.autonomous.phone.di

import android.content.Context
import com.autonomous.phone.device.DeviceController
import com.autonomous.phone.device.ScreenCaptureManager
import com.autonomous.phone.model.ModelManager
import com.autonomous.phone.core.AutonomousEngine
import com.autonomous.phone.core.VisionAnalyzer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDeviceController(): DeviceController {
        return DeviceController()
    }

    @Provides
    @Singleton
    fun provideScreenCaptureManager(@ApplicationContext context: Context): ScreenCaptureManager {
        return ScreenCaptureManager(context)
    }

    @Provides
    @Singleton
    fun provideModelManager(@ApplicationContext context: Context): ModelManager {
        return ModelManager(context)
    }

    @Provides
    @Singleton
    fun provideVisionAnalyzer(
        modelManager: ModelManager
    ): VisionAnalyzer {
        return VisionAnalyzer(modelManager)
    }

    @Provides
    @Singleton
    fun provideAutonomousEngine(
        deviceController: DeviceController,
        screenCaptureManager: ScreenCaptureManager,
        visionAnalyzer: VisionAnalyzer
    ): AutonomousEngine {
        return AutonomousEngine(deviceController, screenCaptureManager, visionAnalyzer)
    }
}
