package com.mainpack.locketcameraclone.core.di

import com.mainpack.locketcameraclone.feature.camera.data.CameraPhotoProcessor
import com.mainpack.locketcameraclone.feature.camera.domain.PhotoProcessor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

	@Binds
	@Singleton
	abstract fun bindPhotoProcessor(
		cameraPhotoProcessor: CameraPhotoProcessor
	): PhotoProcessor
}
