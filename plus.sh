rm -rf OpenCameraPlus
mkdir -p OpenCameraPlus/gen
cp -r libs OpenCameraPlus
cp -r res OpenCameraPlus
cp -r src OpenCameraPlus
cp -r assets OpenCameraPlus
cp -r camera2libs OpenCameraPlus

cp default.properties OpenCameraPlus
cp project.properties OpenCameraPlus

cp .project_plus OpenCameraPlus/.project
cp .classpath OpenCameraPlus/.classpath
perl plus_code.pl AndroidManifest.xml >OpenCameraPlus/AndroidManifest.xml

mv OpenCameraPlus/src/com/almalence/opencam OpenCameraPlus/src/com/almalence/opencam_plus
rm -rf OpenCameraPlus/src/com/android

#rm OpenCameraPlus/src/com/almalence/opencam_plus/billing/Base64.java
#rm OpenCameraPlus/src/com/almalence/opencam_plus/billing/Base64DecoderException.java
#rm OpenCameraPlus/src/com/almalence/opencam_plus/billing/IabException.java
#rm OpenCameraPlus/src/com/almalence/opencam_plus/billing/IabHelper.java
#rm OpenCameraPlus/src/com/almalence/opencam_plus/billing/IabResult.java
#rm OpenCameraPlus/src/com/almalence/opencam_plus/billing/Inventory.java
#rm OpenCameraPlus/src/com/almalence/opencam_plus/billing/Purchase.java
#rm OpenCameraPlus/src/com/almalence/opencam_plus/billing/Security.java
#rm OpenCameraPlus/src/com/almalence/opencam_plus/billing/SkuDetails.java
rm OpenCameraPlus/src/com/almalence/util/AppRater.java
rm OpenCameraPlus/src/com/almalence/opencam_plus/ui/AlmalenceStore.java

perl plus_code.pl src/com/almalence/opencam/MainScreen.java >OpenCameraPlus/src/com/almalence/opencam_plus/MainScreen.java

perl plus_code.pl src/com/almalence/YuvImage.java >OpenCameraPlus/src/com/almalence/YuvImage.java
perl plus_code.pl src/com/almalence/SwapHeap.java >OpenCameraPlus/src/com/almalence/SwapHeap.java

perl plus_code.pl src/com/almalence/asynctaskmanager/Task.java >OpenCameraPlus/src/com/almalence/asynctaskmanager/Task.java
perl plus_code.pl src/com/almalence/asynctaskmanager/OnTaskCompleteListener.java >OpenCameraPlus/src/com/almalence/asynctaskmanager/OnTaskCompleteListener.java
perl plus_code.pl src/com/almalence/asynctaskmanager/IProgressTracker.java >OpenCameraPlus/src/com/almalence/asynctaskmanager/IProgressTracker.java
perl plus_code.pl src/com/almalence/asynctaskmanager/AsyncTaskManager.java >OpenCameraPlus/src/com/almalence/asynctaskmanager/AsyncTaskManager.java

perl plus_code.pl src/com/almalence/googsharing/Thumbnail.java >OpenCameraPlus/src/com/almalence/googsharing/Thumbnail.java

perl plus_code.pl src/com/almalence/plugins/capture/night/NightCapturePlugin.java >OpenCameraPlus/src/com/almalence/plugins/capture/night/NightCapturePlugin.java
perl plus_code.pl src/com/almalence/plugins/capture/night/GLCameraPreview.java >OpenCameraPlus/src/com/almalence/plugins/capture/night/GLCameraPreview.java
perl plus_code.pl src/com/almalence/plugins/capture/preshot/PreshotCapturePlugin.java >OpenCameraPlus/src/com/almalence/plugins/capture/preshot/PreshotCapturePlugin.java
perl plus_code.pl src/com/almalence/plugins/capture/preshot/PreShot.java >OpenCameraPlus/src/com/almalence/plugins/capture/preshot/PreShot.java
perl plus_code.pl src/com/almalence/plugins/capture/burst/BurstCapturePlugin.java >OpenCameraPlus/src/com/almalence/plugins/capture/burst/BurstCapturePlugin.java
perl plus_code.pl src/com/almalence/plugins/capture/bestshot/BestShotCapturePlugin.java >OpenCameraPlus/src/com/almalence/plugins/capture/bestshot/BestShotCapturePlugin.java

perl plus_code.pl src/com/almalence/plugins/capture/video/VideoCapturePlugin.java >OpenCameraPlus/src/com/almalence/plugins/capture/video/VideoCapturePlugin.java
perl plus_code.pl src/com/almalence/plugins/capture/video/AudioRecorder.java >OpenCameraPlus/src/com/almalence/plugins/capture/video/AudioRecorder.java
perl plus_code.pl src/com/almalence/plugins/capture/video/DROVideoEngine.java >OpenCameraPlus/src/com/almalence/plugins/capture/video/DROVideoEngine.java
perl plus_code.pl src/com/almalence/plugins/capture/video/EglEncoder.java >OpenCameraPlus/src/com/almalence/plugins/capture/video/EglEncoder.java
perl plus_code.pl src/com/almalence/plugins/capture/video/RealtimeDRO.java >OpenCameraPlus/src/com/almalence/plugins/capture/video/RealtimeDRO.java
perl plus_code.pl src/com/almalence/plugins/capture/video/TimeLapseDialog.java >OpenCameraPlus/src/com/almalence/plugins/capture/video/TimeLapseDialog.java

perl plus_code.pl src/com/almalence/plugins/capture/expobracketing/ExpoBracketingCapturePlugin.java >OpenCameraPlus/src/com/almalence/plugins/capture/expobracketing/ExpoBracketingCapturePlugin.java
perl plus_code.pl src/com/almalence/plugins/capture/standard/CapturePlugin.java >OpenCameraPlus/src/com/almalence/plugins/capture/standard/CapturePlugin.java
perl plus_code.pl src/com/almalence/plugins/capture/panoramaaugmented/VfGyroSensor.java >OpenCameraPlus/src/com/almalence/plugins/capture/panoramaaugmented/VfGyroSensor.java
perl plus_code.pl src/com/almalence/plugins/capture/panoramaaugmented/AugmentedPanoramaEngine.java >OpenCameraPlus/src/com/almalence/plugins/capture/panoramaaugmented/AugmentedPanoramaEngine.java
perl plus_code.pl src/com/almalence/plugins/capture/panoramaaugmented/Vector3d.java >OpenCameraPlus/src/com/almalence/plugins/capture/panoramaaugmented/Vector3d.java
perl plus_code.pl src/com/almalence/plugins/capture/panoramaaugmented/AugmentedRotationListener.java >OpenCameraPlus/src/com/almalence/plugins/capture/panoramaaugmented/AugmentedRotationListener.java
perl plus_code.pl src/com/almalence/plugins/capture/panoramaaugmented/PanoramaAugmentedCapturePlugin.java >OpenCameraPlus/src/com/almalence/plugins/capture/panoramaaugmented/PanoramaAugmentedCapturePlugin.java
perl plus_code.pl src/com/almalence/plugins/capture/multishot/MultiShotCapturePlugin.java >OpenCameraPlus/src/com/almalence/plugins/capture/multishot/MultiShotCapturePlugin.java


perl plus_code.pl src/com/almalence/plugins/vf/zoom/ZoomVFPlugin.java >OpenCameraPlus/src/com/almalence/plugins/vf/zoom/ZoomVFPlugin.java
perl plus_code.pl src/com/almalence/plugins/vf/focus/Rotatable.java >OpenCameraPlus/src/com/almalence/plugins/vf/focus/Rotatable.java
perl plus_code.pl src/com/almalence/plugins/vf/focus/FocusIndicatorView.java >OpenCameraPlus/src/com/almalence/plugins/vf/focus/FocusIndicatorView.java
perl plus_code.pl src/com/almalence/plugins/vf/focus/RotateLayout.java >OpenCameraPlus/src/com/almalence/plugins/vf/focus/RotateLayout.java
perl plus_code.pl src/com/almalence/plugins/vf/focus/FocusVFPlugin.java >OpenCameraPlus/src/com/almalence/plugins/vf/focus/FocusVFPlugin.java
perl plus_code.pl src/com/almalence/plugins/vf/focus/FocusIndicator.java >OpenCameraPlus/src/com/almalence/plugins/vf/focus/FocusIndicator.java
perl plus_code.pl src/com/almalence/plugins/vf/infoset/InfosetVFPlugin.java >OpenCameraPlus/src/com/almalence/plugins/vf/infoset/InfosetVFPlugin.java
perl plus_code.pl src/com/almalence/plugins/vf/histogram/HistogramVFPlugin.java >OpenCameraPlus/src/com/almalence/plugins/vf/histogram/HistogramVFPlugin.java
perl plus_code.pl src/com/almalence/plugins/vf/histogram/Histogram.java >OpenCameraPlus/src/com/almalence/plugins/vf/histogram/Histogram.java
perl plus_code.pl src/com/almalence/plugins/vf/grid/GridVFPlugin.java >OpenCameraPlus/src/com/almalence/plugins/vf/grid/GridVFPlugin.java
perl plus_code.pl src/com/almalence/plugins/vf/aeawlock/AeAwLockVFPlugin.java >OpenCameraPlus/src/com/almalence/plugins/vf/aeawlock/AeAwLockVFPlugin.java
perl plus_code.pl src/com/almalence/plugins/vf/barcodescanner/BarcodeScannerVFPlugin.java >OpenCameraPlus/src/com/almalence/plugins/vf/barcodescanner/BarcodeScannerVFPlugin.java
perl plus_code.pl src/com/almalence/plugins/vf/gyro/AugmentedSurfaceView.java >OpenCameraPlus/src/com/almalence/plugins/vf/gyro/AugmentedSurfaceView.java
perl plus_code.pl src/com/almalence/plugins/vf/gyro/GyroVFPlugin.java >OpenCameraPlus/src/com/almalence/plugins/vf/gyro/GyroVFPlugin.java

perl plus_code.pl src/com/almalence/plugins/processing/simple/SimpleProcessingPlugin.java >OpenCameraPlus/src/com/almalence/plugins/processing/simple/SimpleProcessingPlugin.java
perl plus_code.pl src/com/almalence/plugins/processing/night/NightProcessingPlugin.java >OpenCameraPlus/src/com/almalence/plugins/processing/night/NightProcessingPlugin.java
perl plus_code.pl src/com/almalence/plugins/processing/night/AlmaShotNight.java >OpenCameraPlus/src/com/almalence/plugins/processing/night/AlmaShotNight.java
perl plus_code.pl src/com/almalence/plugins/processing/preshot/PreshotProcessingPlugin.java >OpenCameraPlus/src/com/almalence/plugins/processing/preshot/PreshotProcessingPlugin.java
perl plus_code.pl src/com/almalence/plugins/processing/sequence/OrderControl.java >OpenCameraPlus/src/com/almalence/plugins/processing/sequence/OrderControl.java
perl plus_code.pl src/com/almalence/plugins/processing/bestshot/BestshotProcessingPlugin.java >OpenCameraPlus/src/com/almalence/plugins/processing/bestshot/BestshotProcessingPlugin.java
perl plus_code.pl src/com/almalence/plugins/processing/sequence/SequenceProcessingPlugin.java >OpenCameraPlus/src/com/almalence/plugins/processing/sequence/SequenceProcessingPlugin.java
perl plus_code.pl src/com/almalence/plugins/processing/sequence/AlmaCLRShot.java >OpenCameraPlus/src/com/almalence/plugins/processing/sequence/AlmaCLRShot.java
perl plus_code.pl src/com/almalence/plugins/processing/objectremoval/ObjectRemovalProcessingPlugin.java >OpenCameraPlus/src/com/almalence/plugins/processing/objectremoval/ObjectRemovalProcessingPlugin.java
perl plus_code.pl src/com/almalence/plugins/processing/objectremoval/AlmaCLRShot.java >OpenCameraPlus/src/com/almalence/plugins/processing/objectremoval/AlmaCLRShot.java
perl plus_code.pl src/com/almalence/plugins/processing/hdr/Adjustment.java >OpenCameraPlus/src/com/almalence/plugins/processing/hdr/Adjustment.java
perl plus_code.pl src/com/almalence/plugins/processing/hdr/AlmaShotHDR.java >OpenCameraPlus/src/com/almalence/plugins/processing/hdr/AlmaShotHDR.java
perl plus_code.pl src/com/almalence/plugins/processing/hdr/HDRProcessingPlugin.java >OpenCameraPlus/src/com/almalence/plugins/processing/hdr/HDRProcessingPlugin.java
perl plus_code.pl src/com/almalence/plugins/processing/hdr/AdjustmentsPreset.java >OpenCameraPlus/src/com/almalence/plugins/processing/hdr/AdjustmentsPreset.java
perl plus_code.pl src/com/almalence/plugins/processing/panorama/PanoramaProcessingPlugin.java >OpenCameraPlus/src/com/almalence/plugins/processing/panorama/PanoramaProcessingPlugin.java
perl plus_code.pl src/com/almalence/plugins/processing/panorama/AlmashotPanorama.java >OpenCameraPlus/src/com/almalence/plugins/processing/panorama/AlmashotPanorama.java
perl plus_code.pl src/com/almalence/plugins/processing/groupshot/Seamless.java >OpenCameraPlus/src/com/almalence/plugins/processing/groupshot/Seamless.java
perl plus_code.pl src/com/almalence/plugins/processing/groupshot/ImageAdapter.java >OpenCameraPlus/src/com/almalence/plugins/processing/groupshot/ImageAdapter.java
perl plus_code.pl src/com/almalence/plugins/processing/groupshot/GroupShotProcessingPlugin.java >OpenCameraPlus/src/com/almalence/plugins/processing/groupshot/GroupShotProcessingPlugin.java
perl plus_code.pl src/com/almalence/plugins/processing/groupshot/Face.java >OpenCameraPlus/src/com/almalence/plugins/processing/groupshot/Face.java
perl plus_code.pl src/com/almalence/plugins/processing/groupshot/AlmaShotSeamless.java >OpenCameraPlus/src/com/almalence/plugins/processing/groupshot/AlmaShotSeamless.java
perl plus_code.pl src/com/almalence/plugins/processing/multishot/MultiShotProcessingPlugin.java >OpenCameraPlus/src/com/almalence/plugins/processing/multishot/MultiShotProcessingPlugin.java

perl plus_code.pl src/com/almalence/plugins/export/standard/ExportPlugin.java >OpenCameraPlus/src/com/almalence/plugins/export/standard/ExportPlugin.java
perl plus_code.pl src/com/almalence/plugins/export/standard/GPSTagsConverter.java >OpenCameraPlus/src/com/almalence/plugins/export/standard/GPSTagsConverter.java

perl plus_code.pl src/com/almalence/opencam/cameracontroller/CameraController.java >OpenCameraPlus/src/com/almalence/opencam_plus/cameracontroller/CameraController.java
perl plus_code.pl src/com/almalence/opencam/cameracontroller/HALv3.java >OpenCameraPlus/src/com/almalence/opencam_plus/cameracontroller/HALv3.java

perl plus_code.pl src/com/almalence/opencam/PluginManagerInterface.java >OpenCameraPlus/src/com/almalence/opencam_plus/PluginManagerInterface.java
perl plus_code.pl src/com/almalence/opencam/ApplicationInterface.java >OpenCameraPlus/src/com/almalence/opencam_plus/ApplicationInterface.java
perl plus_code.pl src/com/almalence/opencam/AlarmReceiver.java >OpenCameraPlus/src/com/almalence/opencam_plus/AlarmReceiver.java
perl plus_code.pl src/com/almalence/opencam/CameraParameters.java >OpenCameraPlus/src/com/almalence/opencam_plus/CameraParameters.java
perl plus_code.pl src/com/almalence/opencam/Fragment.java >OpenCameraPlus/src/com/almalence/opencam_plus/Fragment.java
perl plus_code.pl src/com/almalence/opencam/PluginCapture.java >OpenCameraPlus/src/com/almalence/opencam_plus/PluginCapture.java
perl plus_code.pl src/com/almalence/opencam/PluginManager.java >OpenCameraPlus/src/com/almalence/opencam_plus/PluginManager.java
perl plus_code.pl src/com/almalence/opencam/MainScreen.java >OpenCameraPlus/src/com/almalence/opencam_plus/MainScreen.java
perl plus_code.pl src/com/almalence/opencam/PluginExport.java >OpenCameraPlus/src/com/almalence/opencam_plus/PluginExport.java
perl plus_code.pl src/com/almalence/opencam/HWButtonStart.java >OpenCameraPlus/src/com/almalence/opencam_plus/HWButtonStart.java
perl plus_code.pl src/com/almalence/opencam/Preferences.java >OpenCameraPlus/src/com/almalence/opencam_plus/Preferences.java
perl plus_code.pl src/com/almalence/opencam/ConfigParser.java >OpenCameraPlus/src/com/almalence/opencam_plus/ConfigParser.java
perl plus_code.pl src/com/almalence/opencam/PluginType.java >OpenCameraPlus/src/com/almalence/opencam_plus/PluginType.java
perl plus_code.pl src/com/almalence/opencam/Mode.java >OpenCameraPlus/src/com/almalence/opencam_plus/Mode.java
perl plus_code.pl src/com/almalence/opencam/Plugin.java >OpenCameraPlus/src/com/almalence/opencam_plus/Plugin.java
perl plus_code.pl src/com/almalence/opencam/FolderPicker.java >OpenCameraPlus/src/com/almalence/opencam_plus/FolderPicker.java
perl plus_code.pl src/com/almalence/opencam/FolderPickerLollipop.java >OpenCameraPlus/src/com/almalence/opencam_plus/FolderPickerLollipop.java
perl plus_code.pl src/com/almalence/opencam/PluginProcessing.java >OpenCameraPlus/src/com/almalence/opencam_plus/PluginProcessing.java
perl plus_code.pl src/com/almalence/opencam/SoundPlayer.java >OpenCameraPlus/src/com/almalence/opencam_plus/SoundPlayer.java
perl plus_code.pl src/com/almalence/opencam/PluginViewfinder.java >OpenCameraPlus/src/com/almalence/opencam_plus/PluginViewfinder.java

#perl plus_code.pl src/com/almalence/opencam/util/Size.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/Size.java
perl plus_code.pl src/com/almalence/util/Util.java >OpenCameraPlus/src/com/almalence/util/Util.java
perl plus_code.pl src/com/almalence/util/AppWidgetNotifier.java >OpenCameraPlus/src/com/almalence/util/AppWidgetNotifier.java
perl plus_code.pl src/com/almalence/util/AppEditorNotifier.java >OpenCameraPlus/src/com/almalence/util/AppEditorNotifier.java
#perl plus_code.pl src/com/almalence/opencam/util/MLocation.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/MLocation.java
perl plus_code.pl src/com/almalence/util/ImageConversion.java >OpenCameraPlus/src/com/almalence/util/ImageConversion.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/imaging/ImageMetadataReader.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/imaging/ImageMetadataReader.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/imaging/PhotographicConversions.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/imaging/PhotographicConversions.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/imaging/ImageProcessingException.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/imaging/ImageProcessingException.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/imaging/jpeg/JpegSegmentReader.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/imaging/jpeg/JpegSegmentReader.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/imaging/jpeg/JpegProcessingException.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/imaging/jpeg/JpegProcessingException.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/imaging/jpeg/JpegMetadataReader.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/imaging/jpeg/JpegMetadataReader.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/imaging/jpeg/JpegSegmentData.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/imaging/jpeg/JpegSegmentData.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/metadata/Directory.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/metadata/Directory.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/metadata/TagDescriptor.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/metadata/TagDescriptor.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/metadata/DefaultTagDescriptor.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/metadata/DefaultTagDescriptor.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/metadata/Tag.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/metadata/Tag.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/metadata/Metadata.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/metadata/Metadata.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/metadata/MetadataException.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/metadata/MetadataException.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/metadata/MetadataReader.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/metadata/MetadataReader.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/metadata/jpeg/JpegDirectory.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/metadata/jpeg/JpegDirectory.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/metadata/jpeg/JpegCommentReader.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/metadata/jpeg/JpegCommentReader.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/metadata/jpeg/JpegCommentDirectory.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/metadata/jpeg/JpegCommentDirectory.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/metadata/jpeg/JpegDescriptor.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/metadata/jpeg/JpegDescriptor.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/metadata/jpeg/JpegCommentDescriptor.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/metadata/jpeg/JpegCommentDescriptor.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/metadata/jpeg/JpegComponent.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/metadata/jpeg/JpegComponent.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/metadata/jpeg/JpegReader.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/metadata/jpeg/JpegReader.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/metadata/exif/ExifReader.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/metadata/exif/ExifReader.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/metadata/exif/ExifIFD0Descriptor.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/metadata/exif/ExifIFD0Descriptor.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/metadata/exif/ExifSubIFDDescriptor.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/metadata/exif/ExifSubIFDDescriptor.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/metadata/exif/DataFormat.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/metadata/exif/DataFormat.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/metadata/exif/ExifIFD0Directory.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/metadata/exif/ExifIFD0Directory.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/metadata/exif/ExifSubIFDDirectory.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/metadata/exif/ExifSubIFDDirectory.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/lang/CompoundException.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/lang/CompoundException.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/lang/RandomAccessFileReader.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/lang/RandomAccessFileReader.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/lang/BufferReader.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/lang/BufferReader.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/lang/ByteArrayReader.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/lang/ByteArrayReader.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/lang/GeoLocation.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/lang/GeoLocation.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/lang/BufferBoundsException.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/lang/BufferBoundsException.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/lang/Rational.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/lang/Rational.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/lang/StringUtil.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/lang/StringUtil.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/lang/NullOutputStream.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/lang/NullOutputStream.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/lang/annotations/SuppressWarnings.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/lang/annotations/SuppressWarnings.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/lang/annotations/NotNull.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/lang/annotations/NotNull.java
#perl plus_code.pl src/com/almalence/opencam/util/exifreader/lang/annotations/Nullable.java >OpenCameraPlus/src/com/almalence/opencam_plus/util/exifreader/lang/annotations/Nullable.java

perl plus_code.pl src/com/almalence/ui/RotateImageView.java >OpenCameraPlus/src/com/almalence/ui/RotateImageView.java
perl plus_code.pl src/com/almalence/opencam/ui/ElementAdapter.java >OpenCameraPlus/src/com/almalence/opencam_plus/ui/ElementAdapter.java
perl plus_code.pl src/com/almalence/ui/VerticalSeekBar.java >OpenCameraPlus/src/com/almalence/ui/VerticalSeekBar.java
perl plus_code.pl src/com/almalence/ui/RotateLayout.java >OpenCameraPlus/src/com/almalence/ui/RotateLayout.java
perl plus_code.pl src/com/almalence/opencam/ui/AlmalenceGUI.java >OpenCameraPlus/src/com/almalence/opencam_plus/ui/AlmalenceGUI.java
perl plus_code.pl src/com/almalence/opencam/ui/GLLayer.java >OpenCameraPlus/src/com/almalence/opencam_plus/ui/GLLayer.java
perl plus_code.pl src/com/almalence/opencam/ui/GUI.java >OpenCameraPlus/src/com/almalence/opencam_plus/ui/GUI.java
perl plus_code.pl src/com/almalence/opencam/ui/SeekBarPreference.java >OpenCameraPlus/src/com/almalence/opencam_plus/ui/SeekBarPreference.java
perl plus_code.pl src/com/almalence/ui/Panel.java >OpenCameraPlus/src/com/almalence/ui/Panel.java
perl plus_code.pl src/com/almalence/ui/Switch/AllCapsTransformationMethod.java >OpenCameraPlus/src/com/almalence/ui/Switch/AllCapsTransformationMethod.java
perl plus_code.pl src/com/almalence/ui/Switch/TransformationMethodCompat2.java >OpenCameraPlus/src/com/almalence/ui/Switch/TransformationMethodCompat2.java
perl plus_code.pl src/com/almalence/ui/Switch/Switch.java >OpenCameraPlus/src/com/almalence/ui/Switch/Switch.java
perl plus_code.pl src/com/almalence/ui/Switch/TransformationMethodCompat.java >OpenCameraPlus/src/com/almalence/ui/Switch/TransformationMethodCompat.java
perl plus_code.pl src/com/almalence/opencam/ui/SamplePagerAdapter.java >OpenCameraPlus/src/com/almalence/opencam_plus/ui/SamplePagerAdapter.java
perl plus_code.pl src/com/almalence/opencam/ui/SelfTimerAndPhotoTimeLapse.java >OpenCameraPlus/src/com/almalence/opencam_plus/ui/SelfTimerAndPhotoTimeLapse.java
perl plus_code.pl src/com/almalence/opencam/ui/SelfTimerAndTimeLapseDialog.java >OpenCameraPlus/src/com/almalence/opencam_plus/ui/SelfTimerAndTimeLapseDialog.java

perl plus_code.pl src/com/almalence/ui/Panel.java >OpenCameraPlus/src/com/almalence/ui/Panel.java

perl plus_code.pl res/values/strings_opencamera.xml >OpenCameraPlus/res/values/strings_opencamera.xml

perl plus_code.pl res/layout/gui_almalence_layout.xml >OpenCameraPlus/res/layout/gui_almalence_layout.xml
perl plus_code.pl res/layout-large/gui_almalence_layout.xml >OpenCameraPlus/res/layout-large/gui_almalence_layout.xml
perl plus_code.pl res/layout-xlarge/gui_almalence_layout.xml >OpenCameraPlus/res/layout-xlarge/gui_almalence_layout.xml
perl plus_code.pl res/layout/gui_almalence_panel.xml >OpenCameraPlus/res/layout/gui_almalence_panel.xml
perl plus_code.pl res/layout/plugin_capture_standard_modeswitcher.xml >OpenCameraPlus/res/layout/plugin_capture_standard_modeswitcher.xml
perl plus_code.pl res/layout/plugin_capture_night_modeswitcher.xml >OpenCameraPlus/res/layout/plugin_capture_night_modeswitcher.xml
perl plus_code.pl res/layout/plugin_capture_preshot_modeswitcher.xml >OpenCameraPlus/res/layout/plugin_capture_preshot_modeswitcher.xml
perl plus_code.pl res/layout/plugin_capture_selftimer_layout.xml >OpenCameraPlus/res/layout/plugin_capture_selftimer_layout.xml
perl plus_code.pl res/layout/plugin_capture_video_layout.xml >OpenCameraPlus/res/layout/plugin_capture_video_layout.xml
perl plus_code.pl res/layout/plugin_capture_video_timelapse_dialog.xml >OpenCameraPlus/res/layout/plugin_capture_video_timelapse_dialog.xml
perl plus_code.pl res/layout/plugin_processing_backintime_layout.xml >OpenCameraPlus/res/layout/plugin_processing_backintime_layout.xml
perl plus_code.pl res/layout/plugin_processing_groupshot_postprocessing.xml >OpenCameraPlus/res/layout/plugin_processing_groupshot_postprocessing.xml
perl plus_code.pl res/layout/plugin_processing_hdr_adjustments_preset_cell.xml >OpenCameraPlus/res/layout/plugin_processing_hdr_adjustments_preset_cell.xml
perl plus_code.pl res/layout/plugin_processing_hdr_adjustments.xml >OpenCameraPlus/res/layout/plugin_processing_hdr_adjustments.xml
perl plus_code.pl res/layout/plugin_processing_objectremoval_postprocessing.xml >OpenCameraPlus/res/layout/plugin_processing_objectremoval_postprocessing.xml
perl plus_code.pl res/layout/plugin_processing_preshot_postprocessing_layout.xml >OpenCameraPlus/res/layout/plugin_processing_preshot_postprocessing_layout.xml
perl plus_code.pl res/layout/plugin_processing_sequence_postprocessing.xml >OpenCameraPlus/res/layout/plugin_processing_sequence_postprocessing.xml
perl plus_code.pl res/layout/plugin_vf_aeawlock_layout.xml >OpenCameraPlus/res/layout/plugin_vf_aeawlock_layout.xml
perl plus_code.pl res/layout/plugin_vf_focus_layout.xml >OpenCameraPlus/res/layout/plugin_vf_focus_layout.xml
perl plus_code.pl res/layout/plugin_vf_infoset_icon.xml >OpenCameraPlus/res/layout/plugin_vf_infoset_icon.xml
perl plus_code.pl res/layout/plugin_vf_infoset_text.xml >OpenCameraPlus/res/layout/plugin_vf_infoset_text.xml
perl plus_code.pl res/layout/plugin_vf_zoom_layout.xml >OpenCameraPlus/res/layout/plugin_vf_zoom_layout.xml
perl plus_code.pl res/layout/selftimer_capture_layout.xml >OpenCameraPlus/res/layout/selftimer_capture_layout.xml
perl plus_code.pl res/layout/selftimer_dialog.xml >OpenCameraPlus/res/layout/selftimer_dialog.xml

perl plus_code.pl res/xml/preferences_advance_inactive.xml >OpenCameraPlus/res/xml/preferences_advance_inactive.xml
perl plus_code.pl res/xml/preferences_advanced_common.xml >OpenCameraPlus/res/xml/preferences_advanced_common.xml
perl plus_code.pl res/xml/preferences_capture_burst.xml >OpenCameraPlus/res/xml/preferences_capture_burst.xml
perl plus_code.pl res/xml/preferences_capture_expobracketing.xml >OpenCameraPlus/res/xml/preferences_capture_expobracketing.xml
perl plus_code.pl res/xml/preferences_capture_night.xml >OpenCameraPlus/res/xml/preferences_capture_night.xml
perl plus_code.pl res/xml/preferences_capture_panoramaaugmented.xml >OpenCameraPlus/res/xml/preferences_capture_panoramaaugmented.xml
perl plus_code.pl res/xml/preferences_capture_preshot.xml >OpenCameraPlus/res/xml/preferences_capture_preshot.xml
perl plus_code.pl res/xml/preferences_capture_selftimer.xml >OpenCameraPlus/res/xml/preferences_capture_selftimer.xml
perl plus_code.pl res/xml/preferences_capture_video.xml >OpenCameraPlus/res/xml/preferences_capture_video.xml
perl plus_code.pl res/xml/preferences_export_common.xml >OpenCameraPlus/res/xml/preferences_export_common.xml
perl plus_code.pl res/xml/preferences_export_export.xml >OpenCameraPlus/res/xml/preferences_export_export.xml
perl plus_code.pl res/xml/preferences_general_saveconfiguration.xml >OpenCameraPlus/res/xml/preferences_general_saveconfiguration.xml
perl plus_code.pl res/xml/preferences_headers.xml >OpenCameraPlus/res/xml/preferences_headers.xml
perl plus_code.pl res/xml/preferences_modes.xml >OpenCameraPlus/res/xml/preferences_modes.xml
perl plus_code.pl res/xml/preferences_processing_hdr.xml >OpenCameraPlus/res/xml/preferences_processing_hdr.xml
perl plus_code.pl res/xml/preferences_processing_night.xml >OpenCameraPlus/res/xml/preferences_processing_night.xml
perl plus_code.pl res/xml/preferences_processing_panorama.xml >OpenCameraPlus/res/xml/preferences_processing_panorama.xml
perl plus_code.pl res/xml/preferences_processing_preshot.xml >OpenCameraPlus/res/xml/preferences_processing_preshot.xml
perl plus_code.pl res/xml/preferences_processing_processing.xml >OpenCameraPlus/res/xml/preferences_processing_processing.xml
perl plus_code.pl res/xml/preferences_saving_inactive.xml >OpenCameraPlus/res/xml/preferences_saving_inactive.xml
perl plus_code.pl res/xml/preferences_shooting_inactive.xml >OpenCameraPlus/res/xml/preferences_shooting_inactive.xml
perl plus_code.pl res/xml/preferences_vf_aeawlock.xml >OpenCameraPlus/res/xml/preferences_vf_aeawlock.xml
perl plus_code.pl res/xml/preferences_vf_common.xml >OpenCameraPlus/res/xml/preferences_vf_common.xml
perl plus_code.pl res/xml/preferences_vf_focus.xml >OpenCameraPlus/res/xml/preferences_vf_focus.xml
perl plus_code.pl res/xml/preferences_vf_grid.xml >OpenCameraPlus/res/xml/preferences_vf_grid.xml
perl plus_code.pl res/xml/preferences_vf_histogram.xml >OpenCameraPlus/res/xml/preferences_vf_histogram.xml
perl plus_code.pl res/xml/preferences_vf_inactive.xml >OpenCameraPlus/res/xml/preferences_vf_inactive.xml
perl plus_code.pl res/xml/preferences_vf_infoset.xml >OpenCameraPlus/res/xml/preferences_vf_infoset.xml
perl plus_code.pl res/xml/preferences_vf_zoom.xml >OpenCameraPlus/res/xml/preferences_vf_zoom.xml
perl plus_code.pl res/xml/preferences.xml >OpenCameraPlus/res/xml/preferences.xml
perl plus_code.pl res/xml/preferences_general_more.xml >OpenCameraPlus/res/xml/preferences_general_more.xml

perl plus_code.pl src/com/almalence/plugins/vf/barcodescanner/Barcode.java >OpenCameraPlus/src/com/almalence/plugins/vf/barcodescanner/Barcode.java
perl plus_code.pl src/com/almalence/plugins/vf/barcodescanner/BarcodeArrayAdapter.java >OpenCameraPlus/src/com/almalence/plugins/vf/barcodescanner/BarcodeArrayAdapter.java 
perl plus_code.pl src/com/almalence/plugins/vf/barcodescanner/BarcodeHistoryListDialog.java >OpenCameraPlus/src/com/almalence/plugins/vf/barcodescanner/BarcodeHistoryListDialog.java 
perl plus_code.pl src/com/almalence/plugins/vf/barcodescanner/BarcodeScannerVFPlugin.java >OpenCameraPlus/src/com/almalence/plugins/vf/barcodescanner/BarcodeScannerVFPlugin.java 
perl plus_code.pl src/com/almalence/plugins/vf/barcodescanner/BarcodeStorageHelper.java >OpenCameraPlus/src/com/almalence/plugins/vf/barcodescanner/BarcodeStorageHelper.java 
perl plus_code.pl src/com/almalence/plugins/vf/barcodescanner/BarcodeViewDialog.java >OpenCameraPlus/src/com/almalence/plugins/vf/barcodescanner/BarcodeViewDialog.java 
perl plus_code.pl src/com/almalence/plugins/vf/barcodescanner/result/CalendarResultHandler.java >OpenCameraPlus/src/com/almalence/plugins/vf/barcodescanner/result/CalendarResultHandler.java 
perl plus_code.pl src/com/almalence/plugins/vf/barcodescanner/result/Contents.java >OpenCameraPlus/src/com/almalence/plugins/vf/barcodescanner/result/Contents.java 
perl plus_code.pl src/com/almalence/plugins/vf/barcodescanner/result/EmailAddressResultHandler.java >OpenCameraPlus/src/com/almalence/plugins/vf/barcodescanner/result/EmailAddressResultHandler.java 
perl plus_code.pl src/com/almalence/plugins/vf/barcodescanner/result/GeoResultHandler.java >OpenCameraPlus/src/com/almalence/plugins/vf/barcodescanner/result/GeoResultHandler.java 
perl plus_code.pl src/com/almalence/plugins/vf/barcodescanner/result/LocaleManager.java >OpenCameraPlus/src/com/almalence/plugins/vf/barcodescanner/result/LocaleManager.java 
perl plus_code.pl src/com/almalence/plugins/vf/barcodescanner/result/ProductResultHandler.java >OpenCameraPlus/src/com/almalence/plugins/vf/barcodescanner/result/ProductResultHandler.java 
perl plus_code.pl src/com/almalence/plugins/vf/barcodescanner/result/ResultButtonListener.java >OpenCameraPlus/src/com/almalence/plugins/vf/barcodescanner/result/ResultButtonListener.java 
perl plus_code.pl src/com/almalence/plugins/vf/barcodescanner/result/ResultHandler.java >OpenCameraPlus/src/com/almalence/plugins/vf/barcodescanner/result/ResultHandler.java 
perl plus_code.pl src/com/almalence/plugins/vf/barcodescanner/result/ResultHandlerFactory.java >OpenCameraPlus/src/com/almalence/plugins/vf/barcodescanner/result/ResultHandlerFactory.java 
perl plus_code.pl src/com/almalence/plugins/vf/barcodescanner/result/SMSResultHandler.java >OpenCameraPlus/src/com/almalence/plugins/vf/barcodescanner/result/SMSResultHandler.java 
perl plus_code.pl src/com/almalence/plugins/vf/barcodescanner/result/TelResultHandler.java >OpenCameraPlus/src/com/almalence/plugins/vf/barcodescanner/result/TelResultHandler.java 
perl plus_code.pl src/com/almalence/plugins/vf/barcodescanner/result/TextResultHandler.java >OpenCameraPlus/src/com/almalence/plugins/vf/barcodescanner/result/TextResultHandler.java 
perl plus_code.pl src/com/almalence/plugins/vf/barcodescanner/result/URIResultHandler.java >OpenCameraPlus/src/com/almalence/plugins/vf/barcodescanner/result/URIResultHandler.java 
