package ru.nksk.parentsapp.feature.scanner.di

import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ru.nksk.parentsapp.feature.scanner.ui.MlKitQrFrameAnalyzer
import ru.nksk.parentsapp.feature.scanner.ui.QrFrameAnalyzer

@Module
@InstallIn(SingletonComponent::class)
internal object ScannerProvidesModule {
    /** QR-only detection keeps per-frame work cheap and avoids false positives. */
    @Provides
    @Singleton
    fun provideBarcodeScanner(): BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build(),
    )
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ScannerBindingModule {
    @Binds
    internal abstract fun bindQrFrameAnalyzer(impl: MlKitQrFrameAnalyzer): QrFrameAnalyzer
}
