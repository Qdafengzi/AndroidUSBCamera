package com.camera.demo.uvc_data

enum class ExposureModel(val value: Int) {
    //自动曝光（Auto Mode）：通常为 0x02
    //手动曝光（Manual Mode）：通常为 0x01
    //快门优先（Shutter Priority Mode）：通常为 0x04
    //光圈优先（Aperture Priority Mode）：通常为 0x08
    MANUAL_MODEL(0x01),
    AUTO_MODE(0x02),
    SHUTTER_PRIORITY(0x04),
    APERTURE_PRIORITY(0x08),
}